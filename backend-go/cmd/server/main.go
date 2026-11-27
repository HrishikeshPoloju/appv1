package main

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	"github.com/schoolapp/backend-go/internal/config"
	"github.com/schoolapp/backend-go/internal/database"
	"github.com/schoolapp/backend-go/internal/handlers"
	"github.com/schoolapp/backend-go/internal/middleware"
	"github.com/schoolapp/backend-go/internal/notifications"
	"github.com/schoolapp/backend-go/internal/storage"
)

func main() {
	// Initialize structured JSON logging (Cloud Logging & Cloud Run optimized)
	logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelInfo,
	}))
	slog.SetDefault(logger)

	slog.Info("starting Genesis School SaaS High-Throughput Golang Backend...")

	cfg := config.Load()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Connect to PostgreSQL (Cloud SQL)
	db, err := database.Connect(ctx, cfg.DatabaseURL, cfg.CloudSQLConnectionName, cfg.GoogleAppCredentials)
	if err != nil {
		slog.Error("failed to connect to database", "error", err)
	} else {
		defer db.Pool.Close()
		// Try auto-initialization of tables
		if err := db.InitSchema(ctx); err != nil {
			slog.Warn("schema init skipped or deferred", "error", err)
		}
	}

	// Initialize Object Storage (GCS by default; flip STORAGE_PROVIDER=r2 for zero-egress Cloudflare R2)
	objectStore, err := storage.NewObjectStore(ctx, storage.StoreConfig{
		Provider:          cfg.StorageProvider,
		GCPProjectID:      cfg.GCPProjectID,
		GCSBucketName:     cfg.GCSBucketName,
		GoogleCredentials: cfg.GoogleAppCredentials,
		R2AccountID:       cfg.R2AccountID,
		R2AccessKeyID:     cfg.R2AccessKeyID,
		R2SecretAccessKey: cfg.R2SecretAccessKey,
		R2BucketName:      cfg.R2BucketName,
		R2PublicBaseURL:   cfg.R2PublicBaseURL,
	})
	if err != nil {
		slog.Warn("Object storage initialized in offline/fallback mode", "provider", cfg.StorageProvider, "error", err)
	}

	// Initialize Firebase Cloud Messaging (FREE unlimited push notifications for Notices)
	fcmSvc, fcmErr := notifications.NewFCMService(ctx, cfg.GoogleAppCredentials)
	if fcmErr != nil {
		slog.Warn("FCM service initialized in offline mode", "error", fcmErr)
	}

	authMiddleware := middleware.NewAuthMiddleware(cfg.JWTSecret)

	// Handlers
	authHandler := handlers.NewAuthHandler(db, authMiddleware)
	postHandler := handlers.NewPostHandler(db, fcmSvc)
	discussionHandler := handlers.NewDiscussionHandler(db)
	academicHandler := handlers.NewAcademicHandler(db)
	attendanceHandler := handlers.NewAttendanceHandler(db)
	examHandler := handlers.NewExamHandler(db)
	mediaHandler := handlers.NewMediaHandler(objectStore)
	referralHandler := handlers.NewReferralHandler(db)
	teacherHandler := handlers.NewTeacherHandler(db)
	schoolHandler := handlers.NewSchoolHandler(db, authMiddleware)

	// Router setup
	r := chi.NewRouter()

	r.Use(chimw.RequestID)
	r.Use(chimw.RealIP)
	r.Use(middleware.RequestLogger)
	r.Use(chimw.Recoverer)
	r.Use(chimw.Timeout(60 * time.Second))

	// CORS configuration
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   cfg.AllowedOrigins,
		AllowedMethods:   []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-CSRF-Token", "x-student-name", "x-user-role", "x-school-id", "x-class-id", "x-student-id"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	// Health and probe endpoints (Cloud Run liveness & readiness)
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"success":true,"status":"ok","engine":"golang-1.24","cloud":"google-cloud-run"}`))
	})
	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("OK"))
	})
	r.Get("/readyz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("READY"))
	})

	// Public Auth & Multi-School Metadata endpoints (needed before login)
	r.Post("/api/v1/auth/login", authHandler.StaffLogin)
	r.Post("/api/v1/auth/student-login", authHandler.StudentLogin)
	r.Post("/api/v1/students/login", authHandler.StudentLogin)
	r.Post("/api/v1/auth/register", schoolHandler.RegisterUser)
	r.Post("/api/v1/auth/signin", schoolHandler.SignInUser)
	r.Post("/api/v1/auth/verify-role-code", schoolHandler.VerifyRoleAccessCode)
	r.Get("/api/v1/schools", schoolHandler.ListSchools)
	r.Get("/api/v1/schools/resolve", schoolHandler.ResolveSchool)
	r.Post("/api/v1/schools", schoolHandler.CreateSchool)
	r.Put("/api/v1/schools/{schoolId}", schoolHandler.UpdateSchoolBranding)
	r.Get("/api/v1/classes", authHandler.ListClasses)
	r.Get("/api/v1/media/view", mediaHandler.ViewFile)

	// Public Referral endpoints
	r.Post("/api/v1/public/referrals/lead", referralHandler.SubmitPublicLead)
	r.Post("/api/v1/siblings/submit", referralHandler.SubmitSibling)
	r.Post("/api/v1/siblings", referralHandler.SubmitSibling)
	r.Get("/api/v1/admin/siblings", referralHandler.ListSiblings)
	r.Patch("/api/v1/admin/siblings/{siblingId}/status", referralHandler.UpdateSiblingStatus)

	// Authenticated & Mobile App API routes
	r.Group(func(api chi.Router) {
		api.Use(authMiddleware.AuthenticateOrStudent)

		// Posts & Announcements
		api.Get("/api/v1/posts", postHandler.ListPosts)
		api.Post("/api/v1/posts", postHandler.CreateBroadcast)
		api.Post("/api/v1/admin/broadcast", postHandler.CreateBroadcast)

		// Discussions & Doubts
		api.Get("/api/v1/discussions", discussionHandler.ListDiscussions)
		api.Post("/api/v1/discussions", discussionHandler.CreateDiscussion)
		api.Get("/api/v1/classes/{classId}/discussions", discussionHandler.ListDiscussions)
		api.Post("/api/v1/classes/{classId}/discussions", discussionHandler.CreateDiscussion)
		api.Put("/api/v1/discussions/{discussionId}", discussionHandler.UpdateDiscussion)
		api.Delete("/api/v1/discussions/{discussionId}", discussionHandler.DeleteDiscussion)
		api.Get("/api/v1/discussions/{discussionId}/replies", discussionHandler.ListReplies)
		api.Post("/api/v1/discussions/{discussionId}/replies", discussionHandler.AddReply)
		api.Put("/api/v1/discussions/{discussionId}/replies/{replyId}", discussionHandler.UpdateReply)
		api.Delete("/api/v1/discussions/{discussionId}/replies/{replyId}", discussionHandler.DeleteReply)

		// Academic Notes, Materials & Homework
		api.Get("/api/v1/classes/{classId}/notes", academicHandler.ListNotes)
		api.Post("/api/v1/classes/{classId}/notes", academicHandler.CreateNote)
		api.Get("/api/v1/classes/{classId}/homework", academicHandler.ListHomework)
		api.Post("/api/v1/classes/{classId}/homework", academicHandler.CreateHomework)

		// Students, Classes & Roster Management
		api.Put("/api/v1/auth/student-profile", authHandler.UpdateStudentProfile)
		api.Patch("/api/v1/students/{studentId}/class-section", authHandler.UpdateStudentClassSection)
		api.Patch("/api/v1/teacher/students/{studentId}", authHandler.UpdateStudentClassSection)
		api.Post("/api/v1/classes", authHandler.CreateClass)
		api.Put("/api/v1/classes/{classId}", authHandler.UpdateClass)
		api.Delete("/api/v1/classes/{classId}", authHandler.DeleteClass)
		api.Post("/api/v1/classes/{classId}/promote", authHandler.PromoteClass)
		api.Post("/api/v1/classes/{classId}/selective-cleanup", authHandler.SelectiveCleanup)
		api.Get("/api/v1/classes/{classId}/students", authHandler.GetClassStudents)
		api.Patch("/api/v1/students/{studentId}/verification", authHandler.UpdateStudentVerification)
		api.Post("/api/v1/classes/{classId}/verify-all", authHandler.VerifyAllClass)
		api.Get("/api/v1/school-os/intelligence", authHandler.GetSchoolOSIntelligence)

		// Class-Specific Subjects Management
		api.Get("/api/v1/classes/{classId}/subjects", examHandler.GetClassSubjects)
		api.Post("/api/v1/classes/{classId}/subjects", examHandler.AddClassSubject)
		api.Delete("/api/v1/classes/{classId}/subjects/{subjectName}", examHandler.DeleteClassSubject)

		// Exams, Exam-Specific Subject Max Marks & Teacher Single-Entry Marks
		api.Get("/api/v1/classes/{classId}/exams", examHandler.GetClassExams)
		api.Post("/api/v1/classes/{classId}/exams", examHandler.CreateExam)
		api.Post("/api/v1/exams", examHandler.CreateExam)
		api.Get("/api/v1/exams/{examId}/subjects", examHandler.GetExamSubjects)
		api.Post("/api/v1/exams/{examId}/subjects", examHandler.UpsertExamSubject)
		api.Post("/api/v1/exams/{examId}/marks", examHandler.UpsertStudentExamMarks)
		api.Post("/api/v1/classes/{classId}/exam-marks", examHandler.UpsertStudentExamMarks)

		// Attendance (Class Roll Call & Student Yearly Summary)
		api.Post("/api/v1/attendance", attendanceHandler.RecordAttendance)
		api.Get("/api/v1/classes/{classId}/attendance", attendanceHandler.GetClassAttendance)
		api.Get("/api/v1/students/{studentId}/attendance", attendanceHandler.GetStudentAttendanceSummary)

		// Teacher & Staff HR: Profiles, Daily Check-In/Out Attendance, Staff Leaves
		api.Get("/api/v1/staff", teacherHandler.ListStaff)
		api.Post("/api/v1/staff", teacherHandler.UpsertStaff)
		api.Put("/api/v1/staff/{staffId}", teacherHandler.UpdateStaffProfile)
		api.Get("/api/v1/staff/attendance", teacherHandler.ListStaffAttendance)
		api.Post("/api/v1/staff/attendance/check-in", teacherHandler.StaffCheckIn)
		api.Post("/api/v1/staff/attendance/check-out", teacherHandler.StaffCheckOut)
		api.Patch("/api/v1/staff/attendance", teacherHandler.AdminMarkStaffAttendance)
		api.Get("/api/v1/staff/leaves", teacherHandler.ListStaffLeaves)
		api.Post("/api/v1/staff/leaves", teacherHandler.ApplyStaffLeave)
		api.Patch("/api/v1/staff/leaves/{leaveId}/status", teacherHandler.UpdateStaffLeaveStatus)
		api.Delete("/api/v1/staff/leaves/{leaveId}", teacherHandler.CancelStaffLeave)

		// Student Leaves & Parent Absence Explanations (Teacher Review & Approval)
		api.Get("/api/v1/leaves", teacherHandler.ListStudentLeaves)
		api.Post("/api/v1/leaves", teacherHandler.SubmitStudentLeave)
		api.Patch("/api/v1/leaves/{leaveId}/status", teacherHandler.UpdateStudentLeaveStatus)
		api.Get("/api/v1/absence-explanations", teacherHandler.ListAbsenceExplanations)
		api.Post("/api/v1/absence-explanations", teacherHandler.SubmitAbsenceExplanation)
		api.Patch("/api/v1/absence-explanations/{explanationId}/approve", teacherHandler.ApproveAbsenceExplanation)

		// Teacher Substitution Hub
		api.Get("/api/v1/teacher/substitutions", teacherHandler.ListSubstitutions)
		api.Post("/api/v1/teacher/substitutions", teacherHandler.CreateSubstitution)
		api.Patch("/api/v1/teacher/substitutions/{subId}/assign", teacherHandler.AssignSubstitute)

		// Holiday Calendar Publisher
		api.Get("/api/v1/holidays", teacherHandler.ListHolidays)
		api.Post("/api/v1/holidays", teacherHandler.CreateHoliday)
		api.Delete("/api/v1/holidays/{holidayId}", teacherHandler.DeleteHoliday)

		// Exam Results (matches Figma design with Telugu, Hindi, dynamic max marks & A+ Grade)
		api.Get("/api/v1/students/{studentId}/exam-results", examHandler.GetStudentResults)

		// Media & Cloud Storage Upload
		api.Post("/api/v1/media/upload", mediaHandler.UploadFile)

		// Referral Code
		api.Get("/api/v1/referrals/my-code", referralHandler.GetMyCode)
	})

	server := &http.Server{
		Addr:         fmt.Sprintf(":%s", cfg.Port),
		Handler:      r,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 30 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Server runner goroutine
	go func() {
		slog.Info("HTTP server listening", "port", cfg.Port, "env", cfg.Environment)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			slog.Error("server failed to listen", "error", err)
			os.Exit(1)
		}
	}()

	// In local/dev mode, also listen on port 4000 for Android emulator (10.0.2.2:4000) compatibility
	if cfg.Port != "4000" && cfg.Environment != "production" {
		go func() {
			devServer := &http.Server{
				Addr:         ":4000",
				Handler:      r,
				ReadTimeout:  15 * time.Second,
				WriteTimeout: 30 * time.Second,
				IdleTimeout:  60 * time.Second,
			}
			slog.Info("Android emulator compatibility server listening", "port", "4000")
			_ = devServer.ListenAndServe()
		}()
	}

	// Graceful shutdown handling
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	slog.Info("shutting down HTTP server gracefully...")
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		slog.Error("server forced to shutdown", "error", err)
	}

	slog.Info("server exited cleanly")
}
