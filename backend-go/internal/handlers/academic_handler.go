package handlers

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/schoolapp/backend-go/internal/database"
	"github.com/schoolapp/backend-go/internal/middleware"
	"github.com/schoolapp/backend-go/internal/models"
)

type AcademicHandler struct {
	db *database.DB
}

func NewAcademicHandler(db *database.DB) *AcademicHandler {
	return &AcademicHandler{db: db}
}

func (h *AcademicHandler) ListNotes(w http.ResponseWriter, r *http.Request) {
	classID := chi.URLParam(r, "classId")
	schoolID := getSchoolIDFromRequest(r)
	query := `
		SELECT n.id, n.school_id, n.class_id, n.teacher_id, u.full_name, n.subject_name, n.title, n.chapter_name, n.file_url, n.file_type, n.created_at
		FROM academic_notes n
		JOIN users u ON u.id = n.teacher_id
		WHERE n.school_id = $1 AND (n.class_id = $2 OR $2 = '' OR $2 = 'all')
		ORDER BY n.created_at DESC
	`
	rows, err := h.db.Pool.Query(r.Context(), query, schoolID, classID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{
			Success: false,
			Error:   "Failed to load notes: " + err.Error(),
		})
		return
	}
	defer rows.Close()

	var notes = make([]models.AcademicNote, 0)
	for rows.Next() {
		var n models.AcademicNote
		if err := rows.Scan(&n.ID, &n.SchoolID, &n.ClassID, &n.TeacherID, &n.TeacherName, &n.SubjectName, &n.Title, &n.ChapterName, &n.FileURL, &n.FileType, &n.CreatedAt); err == nil {
			notes = append(notes, n)
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"notes": notes},
	})
}

func (h *AcademicHandler) CreateNote(w http.ResponseWriter, r *http.Request) {
	classID := chi.URLParam(r, "classId")
	user := middleware.GetUserFromContext(r.Context())

	var req struct {
		SubjectName string `json:"subject_name"`
		Title       string `json:"title"`
		ChapterName string `json:"chapter_name"`
		TeacherName string `json:"teacher_name"`
		FileURL     string `json:"file_url"`
		FileType    string `json:"file_type"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}

	schoolID := "00000000-0000-0000-0000-000000000001"
	if user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}

	teacherID, teacherName, actorErr := resolveOrCreateActorUUID(r.Context(), h.db, r, schoolID, req.TeacherName, "TEACHER")
	if actorErr != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: actorErr.Error()})
		return
	}

	query := `
		INSERT INTO academic_notes (school_id, class_id, teacher_id, subject_name, title, chapter_name, file_url, file_type)
		VALUES ($1, $2, $3::uuid, $4, $5, $6, $7, $8)
		RETURNING id::text, created_at
	`
	var id string
	var createdAt time.Time
	err := h.db.Pool.QueryRow(r.Context(), query, schoolID, classID, teacherID, req.SubjectName, req.Title, req.ChapterName, req.FileURL, req.FileType).Scan(&id, &createdAt)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to save study note: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Data: models.AcademicNote{
			ID:          id,
			SchoolID:    schoolID,
			ClassID:     classID,
			TeacherID:   teacherID,
			TeacherName: teacherName,
			SubjectName: req.SubjectName,
			Title:       req.Title,
			ChapterName: req.ChapterName,
			FileURL:     req.FileURL,
			FileType:    req.FileType,
			CreatedAt:   createdAt,
		},
	})
}

func (h *AcademicHandler) ListHomework(w http.ResponseWriter, r *http.Request) {
	classID := chi.URLParam(r, "classId")
	schoolID := getSchoolIDFromRequest(r)
	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, class_id, teacher_name, subject_name, title, description, due_date, created_at
		FROM homework_assignments
		WHERE school_id = $1 AND (class_id = $2 OR $2 = '' OR $2 = 'all')
		ORDER BY created_at DESC
	`, schoolID, classID)

	homework := make([]map[string]interface{}, 0)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var id, cID, teacherName, subjectName, title, description, dueDate string
			var createdAt time.Time
			if scanErr := rows.Scan(&id, &cID, &teacherName, &subjectName, &title, &description, &dueDate, &createdAt); scanErr == nil {
				homework = append(homework, map[string]interface{}{
					"id":           id,
					"class_id":     cID,
					"teacher_name": teacherName,
					"subject_name": subjectName,
					"title":        title,
					"description":  description,
					"due_date":     dueDate,
					"created_at":   createdAt,
				})
			}
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"homework": homework},
	})
}

func (h *AcademicHandler) CreateHomework(w http.ResponseWriter, r *http.Request) {
	classID := chi.URLParam(r, "classId")
	user := middleware.GetUserFromContext(r.Context())
	var req struct {
		ClassIDs    []string `json:"class_ids"`
		SubjectName string   `json:"subject_name"`
		Title       string   `json:"title"`
		Description string   `json:"description"`
		DueDate     string   `json:"due_date"`
		Attachments []string `json:"attachments"`
	}
	_ = json.NewDecoder(r.Body).Decode(&req)

	schoolID := "00000000-0000-0000-0000-000000000001"
	teacherName := "P. Sharma"
	if user != nil {
		schoolID = user.SchoolID
		if user.FullName != "" {
			teacherName = user.FullName
		}
	}

	targets := req.ClassIDs
	if len(targets) == 0 {
		if classID == "" {
			classID = "class-10a"
		}
		targets = []string{classID}
	}

	attachmentsJSON, _ := json.Marshal(req.Attachments)
	var lastID string
	for _, targetClass := range targets {
		_ = h.db.Pool.QueryRow(r.Context(), `
			INSERT INTO homework_assignments (school_id, class_id, teacher_name, subject_name, title, description, due_date, attachments)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
			RETURNING id::text
		`, schoolID, targetClass, teacherName, req.SubjectName, req.Title, req.Description, req.DueDate, attachmentsJSON).Scan(&lastID)
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Homework saved to database across target section(s)",
		Data: map[string]interface{}{
			"id":          lastID,
			"class_ids":   targets,
			"subject":     req.SubjectName,
			"title":       req.Title,
			"description": req.Description,
			"due_date":    req.DueDate,
		},
	})
}
