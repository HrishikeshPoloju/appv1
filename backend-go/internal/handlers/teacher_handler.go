package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/schoolapp/backend-go/internal/database"
	"github.com/schoolapp/backend-go/internal/middleware"
	"github.com/schoolapp/backend-go/internal/models"
)

type TeacherHandler struct {
	db *database.DB
}

func NewTeacherHandler(db *database.DB) *TeacherHandler {
	return &TeacherHandler{db: db}
}

func getSchoolIDFromRequest(r *http.Request) string {
	if user := middleware.GetUserFromContext(r.Context()); user != nil && user.SchoolID != "" {
		return user.SchoolID
	}
	return "00000000-0000-0000-0000-000000000001"
}

// ============================================================================
// 1. STAFF DIRECTORY & PROFILES (GET / POST / PUT)
// ============================================================================

func (h *TeacherHandler) ListStaff(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id, staff_code, full_name, role_title, department, staff_type, phone, email, qualification, subjects, classes_assigned, attendance_status
		FROM staff_profiles
		WHERE school_id = $1
		ORDER BY full_name ASC
	`, schoolID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch staff directory: " + err.Error()})
		return
	}
	defer rows.Close()

	staff := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, code, name, roleTitle, dept, staffType, phone, email, qual, subjects, classes, attStatus string
		if sErr := rows.Scan(&id, &code, &name, &roleTitle, &dept, &staffType, &phone, &email, &qual, &subjects, &classes, &attStatus); sErr == nil {
			staff = append(staff, map[string]interface{}{
				"id":                id,
				"staff_code":        code,
				"full_name":         name,
				"role_title":        roleTitle,
				"department":        dept,
				"staff_type":        staffType,
				"phone":             phone,
				"email":             email,
				"qualification":     qual,
				"subjects":          subjects,
				"classes_assigned":  classes,
				"attendance_status": attStatus,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"staff": staff},
	})
}

func (h *TeacherHandler) UpsertStaff(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		ID              string `json:"id"`
		StaffCode       string `json:"staff_code"`
		FullName        string `json:"full_name"`
		RoleTitle       string `json:"role_title"`
		Department      string `json:"department"`
		StaffType       string `json:"staff_type"`
		Phone           string `json:"phone"`
		Email           string `json:"email"`
		Qualification   string `json:"qualification"`
		Subjects        string `json:"subjects"`
		ClassesAssigned string `json:"classes_assigned"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if strings.TrimSpace(req.FullName) == "" {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "full_name is required"})
		return
	}
	if req.ID == "" {
		req.ID = fmt.Sprintf("stf-%d", time.Now().UnixMilli())
	}
	if req.RoleTitle == "" {
		req.RoleTitle = "Teacher"
	}
	if req.Department == "" {
		req.Department = "Academics"
	}
	if req.StaffType == "" {
		req.StaffType = "Teaching"
	}

	_, err := h.db.Pool.Exec(r.Context(), `
		INSERT INTO staff_profiles (id, school_id, staff_code, full_name, role_title, department, staff_type, phone, email, qualification, subjects, classes_assigned)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
		ON CONFLICT (id) DO UPDATE SET
			full_name = EXCLUDED.full_name,
			role_title = EXCLUDED.role_title,
			department = EXCLUDED.department,
			staff_type = EXCLUDED.staff_type,
			phone = EXCLUDED.phone,
			email = EXCLUDED.email,
			qualification = EXCLUDED.qualification,
			subjects = EXCLUDED.subjects,
			classes_assigned = EXCLUDED.classes_assigned,
			updated_at = now()
	`, req.ID, schoolID, req.StaffCode, req.FullName, req.RoleTitle, req.Department, req.StaffType, req.Phone, req.Email, req.Qualification, req.Subjects, req.ClassesAssigned)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to save staff profile: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Staff profile saved",
		Data:    map[string]interface{}{"id": req.ID, "full_name": req.FullName},
	})
}

func (h *TeacherHandler) UpdateStaffProfile(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	staffID := chi.URLParam(r, "staffId")
	var req struct {
		FullName      string `json:"full_name"`
		Phone         string `json:"phone"`
		Email         string `json:"email"`
		Qualification string `json:"qualification"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}

	tag, err := h.db.Pool.Exec(r.Context(), `
		UPDATE staff_profiles
		SET phone = COALESCE(NULLIF($1, ''), phone),
		    email = $2,
		    qualification = COALESCE(NULLIF($3, ''), qualification),
		    updated_at = now()
		WHERE school_id = $4 AND id = $5
	`, req.Phone, req.Email, req.Qualification, schoolID, staffID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to update staff profile: " + err.Error()})
		return
	}
	if tag.RowsAffected() == 0 {
		// Upsert if not present yet
		name := req.FullName
		if name == "" {
			name = "Staff Member"
		}
		_, _ = h.db.Pool.Exec(r.Context(), `
			INSERT INTO staff_profiles (id, school_id, full_name, phone, email, qualification)
			VALUES ($1, $2, $3, $4, $5, $6)
			ON CONFLICT (id) DO UPDATE SET phone = EXCLUDED.phone, email = EXCLUDED.email, qualification = EXCLUDED.qualification, updated_at = now()
		`, staffID, schoolID, name, req.Phone, req.Email, req.Qualification)
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: "Staff contact profile updated",
		Data:    map[string]interface{}{"staff_id": staffID, "phone": req.Phone, "email": req.Email, "qualification": req.Qualification},
	})
}

// ============================================================================
// 2. STAFF ATTENDANCE (CHECK-IN, CHECK-OUT, ADMIN OVERRIDE, LOGS)
// ============================================================================

func (h *TeacherHandler) ListStaffAttendance(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	staffIDFilter := strings.TrimSpace(r.URL.Query().Get("staff_id"))
	dateFilter := strings.TrimSpace(r.URL.Query().Get("date"))

	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, staff_id, COALESCE(staff_name, ''), date::text, COALESCE(check_in_time, ''), COALESCE(check_out_time, ''), status, COALESCE(distance_meters, 0), verification_method
		FROM staff_attendance
		WHERE school_id = $1
		  AND ($2 = '' OR staff_id = $2)
		  AND ($3 = '' OR date::text = $3)
		ORDER BY date DESC, updated_at DESC
		LIMIT 100
	`, schoolID, staffIDFilter, dateFilter)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch staff attendance: " + err.Error()})
		return
	}
	defer rows.Close()

	todayISO := time.Now().Format("2006-01-02")
	records := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, sID, sName, dateStr, checkIn, checkOut, status, method string
		var dist int
		if sErr := rows.Scan(&id, &sID, &sName, &dateStr, &checkIn, &checkOut, &status, &dist, &method); sErr == nil {
			dateLabel := dateStr
			if dateStr == todayISO {
				dateLabel = "Today"
			}
			records = append(records, map[string]interface{}{
				"id":                  id,
				"staff_id":            sID,
				"staff_name":          sName,
				"date":                dateStr,
				"date_label":          dateLabel,
				"check_in":            checkIn,
				"check_out":           checkOut,
				"status":              status,
				"distance_meters":     dist,
				"verification_method": method,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"records": records},
	})
}

func (h *TeacherHandler) StaffCheckIn(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		StaffID            string `json:"staff_id"`
		StaffName          string `json:"staff_name"`
		Date               string `json:"date"`
		CheckInTime        string `json:"check_in"`
		DistanceMeters     *int   `json:"distance_meters"`
		VerificationMethod string `json:"verification_method"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.StaffID == "" {
		req.StaffID = "stf-1"
	}
	if req.Date == "" {
		req.Date = time.Now().Format("2006-01-02")
	}
	if req.CheckInTime == "" {
		req.CheckInTime = time.Now().Format("03:04 PM")
	}
	if req.VerificationMethod == "" {
		req.VerificationMethod = "Fingerprint • Within 500m"
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO staff_attendance (school_id, staff_id, staff_name, date, check_in_time, status, distance_meters, verification_method)
		VALUES ($1, $2, $3, $4::date, $5, 'Present', $6, $7)
		ON CONFLICT (school_id, staff_id, date) DO UPDATE SET
			check_in_time = EXCLUDED.check_in_time,
			status = 'Present',
			distance_meters = EXCLUDED.distance_meters,
			verification_method = EXCLUDED.verification_method,
			updated_at = now()
		RETURNING id::text
	`, schoolID, req.StaffID, req.StaffName, req.Date, req.CheckInTime, req.DistanceMeters, req.VerificationMethod).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to record staff check-in: " + err.Error()})
		return
	}

	_, _ = h.db.Pool.Exec(r.Context(), `UPDATE staff_profiles SET attendance_status = 'Present', updated_at = now() WHERE school_id = $1 AND id = $2`, schoolID, req.StaffID)

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: "Staff checked in successfully",
		Data: map[string]interface{}{
			"id":                  id,
			"staff_id":            req.StaffID,
			"date":                req.Date,
			"check_in":            req.CheckInTime,
			"status":              "Present",
			"verification_method": req.VerificationMethod,
		},
	})
}

func (h *TeacherHandler) StaffCheckOut(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		StaffID      string `json:"staff_id"`
		StaffName    string `json:"staff_name"`
		Date         string `json:"date"`
		CheckOutTime string `json:"check_out"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.StaffID == "" {
		req.StaffID = "stf-1"
	}
	if req.Date == "" {
		req.Date = time.Now().Format("2006-01-02")
	}
	if req.CheckOutTime == "" {
		req.CheckOutTime = time.Now().Format("03:04 PM")
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO staff_attendance (school_id, staff_id, staff_name, date, check_out_time, status)
		VALUES ($1, $2, $3, $4::date, $5, 'Present')
		ON CONFLICT (school_id, staff_id, date) DO UPDATE SET
			check_out_time = EXCLUDED.check_out_time,
			updated_at = now()
		RETURNING id::text
	`, schoolID, req.StaffID, req.StaffName, req.Date, req.CheckOutTime).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to record staff check-out: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: "Staff checked out successfully",
		Data: map[string]interface{}{
			"id":        id,
			"staff_id":  req.StaffID,
			"date":      req.Date,
			"check_out": req.CheckOutTime,
		},
	})
}

func (h *TeacherHandler) AdminMarkStaffAttendance(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		StaffID   string `json:"staff_id"`
		StaffName string `json:"staff_name"`
		Date      string `json:"date"`
		Status    string `json:"status"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.Date == "" {
		req.Date = time.Now().Format("2006-01-02")
	}
	if req.Status == "" {
		req.Status = "Present"
	}

	var checkIn *string
	if req.Status == "Present" {
		now := time.Now().Format("03:04 PM")
		checkIn = &now
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO staff_attendance (school_id, staff_id, staff_name, date, check_in_time, status, verification_method)
		VALUES ($1, $2, $3, $4::date, $5, $6, 'Admin Register')
		ON CONFLICT (school_id, staff_id, date) DO UPDATE SET
			status = EXCLUDED.status,
			check_in_time = COALESCE(staff_attendance.check_in_time, EXCLUDED.check_in_time),
			updated_at = now()
		RETURNING id::text
	`, schoolID, req.StaffID, req.StaffName, req.Date, checkIn, req.Status).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to mark staff attendance: " + err.Error()})
		return
	}

	_, _ = h.db.Pool.Exec(r.Context(), `UPDATE staff_profiles SET attendance_status = $1, updated_at = now() WHERE school_id = $2 AND id = $3`, req.Status, schoolID, req.StaffID)

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Staff attendance marked %s", req.Status),
		Data:    map[string]interface{}{"id": id, "staff_id": req.StaffID, "status": req.Status, "date": req.Date},
	})
}

// ============================================================================
// 3. STAFF LEAVE APPLICATIONS (LIST, APPLY, APPROVE/REJECT, CANCEL)
// ============================================================================

func (h *TeacherHandler) ListStaffLeaves(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	staffID := strings.TrimSpace(r.URL.Query().Get("staff_id"))

	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, staff_id, staff_name, leave_type, from_date, to_date, days, reason, status, reviewed_by, substitute_requested, applied_on, created_at
		FROM staff_leaves
		WHERE school_id = $1 AND ($2 = '' OR staff_id = $2)
		ORDER BY created_at DESC
	`, schoolID, staffID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch staff leaves: " + err.Error()})
		return
	}
	defer rows.Close()

	leaves := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, sID, sName, lType, fromD, toD, reason, status, revBy, appliedOn string
		var days int
		var subReq bool
		var createdAt time.Time
		if sErr := rows.Scan(&id, &sID, &sName, &lType, &fromD, &toD, &days, &reason, &status, &revBy, &subReq, &appliedOn, &createdAt); sErr == nil {
			leaves = append(leaves, map[string]interface{}{
				"id":                   id,
				"staff_id":             sID,
				"staff_name":           sName,
				"leave_type":           lType,
				"from_date":            fromD,
				"to_date":              toD,
				"days":                 days,
				"reason":               reason,
				"status":               status,
				"reviewed_by":          revBy,
				"substitute_requested": subReq,
				"applied_on":           appliedOn,
				"created_at":           createdAt,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"leaves": leaves},
	})
}

func (h *TeacherHandler) ApplyStaffLeave(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		StaffID   string `json:"staff_id"`
		StaffName string `json:"staff_name"`
		LeaveType string `json:"leave_type"`
		FromDate  string `json:"from_date"`
		ToDate    string `json:"to_date"`
		Days      int    `json:"days"`
		Reason    string `json:"reason"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.StaffName == "" {
		req.StaffName = "Teacher"
	}
	if req.LeaveType == "" {
		req.LeaveType = "Casual Leave"
	}
	if req.Days <= 0 {
		req.Days = 1
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO staff_leaves (school_id, staff_id, staff_name, leave_type, from_date, to_date, days, reason, status, applied_on)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'Pending', 'Today')
		RETURNING id::text
	`, schoolID, req.StaffID, req.StaffName, req.LeaveType, req.FromDate, req.ToDate, req.Days, req.Reason).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to submit staff leave: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Staff leave application submitted",
		Data: map[string]interface{}{
			"id":         id,
			"staff_id":   req.StaffID,
			"staff_name": req.StaffName,
			"leave_type": req.LeaveType,
			"from_date":  req.FromDate,
			"to_date":    req.ToDate,
			"days":       req.Days,
			"status":     "Pending",
		},
	})
}

func (h *TeacherHandler) UpdateStaffLeaveStatus(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	leaveID := chi.URLParam(r, "leaveId")
	var req struct {
		Status              string `json:"status"` // Approved / Rejected
		ReviewedBy          string `json:"reviewed_by"`
		SubstituteRequested bool   `json:"substitute_requested"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.ReviewedBy == "" {
		req.ReviewedBy = "Principal"
	}

	if isValidUUID(leaveID) {
		_, err := h.db.Pool.Exec(r.Context(), `
			UPDATE staff_leaves
			SET status = $1, reviewed_by = $2, substitute_requested = $3, updated_at = now()
			WHERE school_id = $4 AND id = $5::uuid
		`, req.Status, req.ReviewedBy, req.SubstituteRequested, schoolID, leaveID)
		if err != nil {
			sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to update staff leave: " + err.Error()})
			return
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Staff leave %s", req.Status),
		Data:    map[string]interface{}{"leave_id": leaveID, "status": req.Status, "reviewed_by": req.ReviewedBy},
	})
}

func (h *TeacherHandler) CancelStaffLeave(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	leaveID := chi.URLParam(r, "leaveId")
	if isValidUUID(leaveID) {
		_, err := h.db.Pool.Exec(r.Context(), `DELETE FROM staff_leaves WHERE school_id = $1 AND id = $2::uuid AND status = 'Pending'`, schoolID, leaveID)
		if err != nil {
			sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to cancel leave: " + err.Error()})
			return
		}
	}
	sendJSON(w, http.StatusOK, models.ApiResponse{Success: true, Message: "Pending staff leave cancelled"})
}

// ============================================================================
// 4. STUDENT LEAVES (SUBMIT, LIST, TEACHER/ADMIN APPROVE/REJECT)
// ============================================================================

func (h *TeacherHandler) ListStudentLeaves(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	classID := strings.TrimSpace(r.URL.Query().Get("class_id"))

	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, student_id, student_name, class_id, class_name, leave_type, start_date, end_date, reason, status, reviewed_by, created_at
		FROM student_leaves
		WHERE school_id = $1 AND ($2 = '' OR class_id = $2)
		ORDER BY created_at DESC
	`, schoolID, classID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch student leaves: " + err.Error()})
		return
	}
	defer rows.Close()

	list := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, stuID, stuName, cID, cName, lType, sDate, eDate, reason, status, revBy string
		var createdAt time.Time
		if sErr := rows.Scan(&id, &stuID, &stuName, &cID, &cName, &lType, &sDate, &eDate, &reason, &status, &revBy, &createdAt); sErr == nil {
			list = append(list, map[string]interface{}{
				"id":           id,
				"student_id":   stuID,
				"student_name": stuName,
				"class_id":     cID,
				"class_name":   cName,
				"leave_type":   lType,
				"start_date":   sDate,
				"end_date":     eDate,
				"reason":       reason,
				"status":       status,
				"reviewed_by":  revBy,
				"created_at":   createdAt,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"leaves": list},
	})
}

func (h *TeacherHandler) SubmitStudentLeave(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		StudentID   string `json:"student_id"`
		StudentName string `json:"student_name"`
		ClassID     string `json:"class_id"`
		ClassName   string `json:"class_name"`
		LeaveType   string `json:"leave_type"`
		StartDate   string `json:"start_date"`
		EndDate     string `json:"end_date"`
		Reason      string `json:"reason"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO student_leaves (school_id, student_id, student_name, class_id, class_name, leave_type, start_date, end_date, reason, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'Pending Review')
		RETURNING id::text
	`, schoolID, req.StudentID, req.StudentName, req.ClassID, req.ClassName, req.LeaveType, req.StartDate, req.EndDate, req.Reason).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to submit student leave: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Student leave request saved",
		Data:    map[string]interface{}{"id": id, "status": "Pending Review"},
	})
}

func (h *TeacherHandler) UpdateStudentLeaveStatus(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	leaveID := chi.URLParam(r, "leaveId")
	var req struct {
		Status     string `json:"status"`
		ReviewedBy string `json:"reviewed_by"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.ReviewedBy == "" {
		req.ReviewedBy = "Class Teacher"
	}

	if isValidUUID(leaveID) {
		_, err := h.db.Pool.Exec(r.Context(), `
			UPDATE student_leaves SET status = $1, reviewed_by = $2, updated_at = now()
			WHERE school_id = $3 AND id = $4::uuid
		`, req.Status, req.ReviewedBy, schoolID, leaveID)
		if err != nil {
			sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to update student leave: " + err.Error()})
			return
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Student leave updated to %s", req.Status),
		Data:    map[string]interface{}{"leave_id": leaveID, "status": req.Status},
	})
}

// ============================================================================
// 5. PARENT ABSENCE EXPLANATIONS (LIST, SUBMIT, TEACHER APPROVE)
// ============================================================================

func (h *TeacherHandler) ListAbsenceExplanations(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	classID := strings.TrimSpace(r.URL.Query().Get("class_id"))

	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, student_id, student_name, class_id, class_section, date_text, parent_explanation, status, approved_by, created_at
		FROM parent_absence_explanations
		WHERE school_id = $1 AND ($2 = '' OR class_id = $2)
		ORDER BY created_at DESC
	`, schoolID, classID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch absence explanations: " + err.Error()})
		return
	}
	defer rows.Close()

	list := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, stuID, stuName, cID, cSec, dText, expl, status, appBy string
		var createdAt time.Time
		if sErr := rows.Scan(&id, &stuID, &stuName, &cID, &cSec, &dText, &expl, &status, &appBy, &createdAt); sErr == nil {
			list = append(list, map[string]interface{}{
				"id":                 id,
				"student_id":         stuID,
				"student_name":       stuName,
				"class_id":           cID,
				"class_section":      cSec,
				"date_text":          dText,
				"parent_explanation": expl,
				"status":             status,
				"approved_by":        appBy,
				"created_at":         createdAt,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"explanations": list},
	})
}

func (h *TeacherHandler) SubmitAbsenceExplanation(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		StudentID         string `json:"student_id"`
		StudentName       string `json:"student_name"`
		ClassID           string `json:"class_id"`
		ClassSection      string `json:"class_section"`
		DateText          string `json:"date_text"`
		ParentExplanation string `json:"parent_explanation"`
		Status            string `json:"status"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.DateText == "" {
		req.DateText = "Today"
	}
	if req.Status == "" {
		req.Status = "Submitted by Parent"
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO parent_absence_explanations (school_id, student_id, student_name, class_id, class_section, date_text, parent_explanation, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		RETURNING id::text
	`, schoolID, req.StudentID, req.StudentName, req.ClassID, req.ClassSection, req.DateText, req.ParentExplanation, req.Status).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to save absence explanation: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Absence explanation recorded",
		Data:    map[string]interface{}{"id": id, "status": req.Status},
	})
}

func (h *TeacherHandler) ApproveAbsenceExplanation(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	explID := chi.URLParam(r, "explanationId")
	if isValidUUID(explID) {
		_, err := h.db.Pool.Exec(r.Context(), `
			UPDATE parent_absence_explanations
			SET status = 'Approved by Teacher', approved_by = 'Class Teacher', updated_at = now()
			WHERE school_id = $1 AND id = $2::uuid
		`, schoolID, explID)
		if err != nil {
			sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to approve explanation: " + err.Error()})
			return
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: "Absence explanation approved by teacher",
		Data:    map[string]interface{}{"id": explID, "status": "Approved by Teacher"},
	})
}

// ============================================================================
// 6. TEACHER SUBSTITUTION HUB (LIST, CREATE, ASSIGN)
// ============================================================================

func (h *TeacherHandler) ListSubstitutions(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, absent_teacher_name, leave_reason, period_slot, class_section_name, subject, suggested_substitutes, COALESCE(assigned_substitute, ''), status, created_at
		FROM teacher_substitutions
		WHERE school_id = $1
		ORDER BY created_at DESC
	`, schoolID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch substitutions: " + err.Error()})
		return
	}
	defer rows.Close()

	list := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, absentName, reason, slot, classSec, subject, assigned, status string
		var suggestedRaw []byte
		var createdAt time.Time
		if sErr := rows.Scan(&id, &absentName, &reason, &slot, &classSec, &subject, &suggestedRaw, &assigned, &status, &createdAt); sErr == nil {
			var suggested []string
			_ = json.Unmarshal(suggestedRaw, &suggested)
			if suggested == nil {
				suggested = []string{}
			}
			list = append(list, map[string]interface{}{
				"id":                    id,
				"absent_teacher_name":   absentName,
				"leave_reason":          reason,
				"period_slot":           slot,
				"class_section_name":    classSec,
				"subject":               subject,
				"suggested_substitutes": suggested,
				"assigned_substitute":   assigned,
				"status":                status,
				"created_at":            createdAt,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"substitutions": list},
	})
}

func (h *TeacherHandler) CreateSubstitution(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		AbsentTeacherName    string   `json:"absent_teacher_name"`
		LeaveReason          string   `json:"leave_reason"`
		PeriodSlot           string   `json:"period_slot"`
		ClassSectionName     string   `json:"class_section_name"`
		Subject              string   `json:"subject"`
		SuggestedSubstitutes []string `json:"suggested_substitutes"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if len(req.SuggestedSubstitutes) == 0 {
		req.SuggestedSubstitutes = []string{
			fmt.Sprintf("Mrs. S. Gupta (Free in %s)", req.PeriodSlot),
			fmt.Sprintf("Mr. K. Kumar (Free in %s • 0 Subs Today)", req.PeriodSlot),
			fmt.Sprintf("Pooja Ma'am (Free in %s)", req.PeriodSlot),
		}
	}
	suggestedJSON, _ := json.Marshal(req.SuggestedSubstitutes)

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO teacher_substitutions (school_id, absent_teacher_name, leave_reason, period_slot, class_section_name, subject, suggested_substitutes, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7, 'Action Needed')
		RETURNING id::text
	`, schoolID, req.AbsentTeacherName, req.LeaveReason, req.PeriodSlot, req.ClassSectionName, req.Subject, suggestedJSON).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to create substitution request: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Substitute matching request created",
		Data:    map[string]interface{}{"id": id, "status": "Action Needed", "suggested_substitutes": req.SuggestedSubstitutes},
	})
}

func (h *TeacherHandler) AssignSubstitute(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	subID := chi.URLParam(r, "subId")
	var req struct {
		AssignedSubstitute string `json:"assigned_substitute"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if isValidUUID(subID) {
		_, err := h.db.Pool.Exec(r.Context(), `
			UPDATE teacher_substitutions
			SET assigned_substitute = $1, status = 'Assigned & Notified', updated_at = now()
			WHERE school_id = $2 AND id = $3::uuid
		`, req.AssignedSubstitute, schoolID, subID)
		if err != nil {
			sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to assign substitute: " + err.Error()})
			return
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: "Substitute teacher assigned & notified",
		Data:    map[string]interface{}{"id": subID, "assigned_substitute": req.AssignedSubstitute, "status": "Assigned & Notified"},
	})
}

// ============================================================================
// 7. HOLIDAY CALENDAR PUBLISHER (LIST, CREATE, DELETE)
// ============================================================================

func (h *TeacherHandler) ListHolidays(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, title, date_iso, scope_type, target_class, target_section, description, created_by, created_at
		FROM school_holidays
		WHERE school_id = $1
		ORDER BY date_iso ASC
	`, schoolID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to fetch holidays: " + err.Error()})
		return
	}
	defer rows.Close()

	holidays := make([]map[string]interface{}, 0)
	for rows.Next() {
		var id, title, dateIso, scopeType, tClass, tSec, desc, createdBy string
		var createdAt time.Time
		if sErr := rows.Scan(&id, &title, &dateIso, &scopeType, &tClass, &tSec, &desc, &createdBy, &createdAt); sErr == nil {
			holidays = append(holidays, map[string]interface{}{
				"id":             id,
				"title":          title,
				"date_iso":       dateIso,
				"scope_type":     scopeType,
				"target_class":   tClass,
				"target_section": tSec,
				"description":    desc,
				"created_by":     createdBy,
				"created_at":     createdAt,
			})
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"holidays": holidays},
	})
}

func (h *TeacherHandler) CreateHoliday(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	var req struct {
		Title         string `json:"title"`
		DateIso       string `json:"date_iso"`
		ScopeType     string `json:"scope_type"`
		TargetClass   string `json:"target_class"`
		TargetSection string `json:"target_section"`
		Description   string `json:"description"`
		CreatedBy     string `json:"created_by"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	if req.ScopeType == "" {
		req.ScopeType = "SCHOOL"
	}
	if req.TargetClass == "" {
		req.TargetClass = "ALL"
	}
	if req.TargetSection == "" {
		req.TargetSection = "ALL"
	}
	if req.CreatedBy == "" {
		req.CreatedBy = "Class Teacher"
	}

	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO school_holidays (school_id, title, date_iso, scope_type, target_class, target_section, description, created_by)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		RETURNING id::text
	`, schoolID, req.Title, req.DateIso, req.ScopeType, req.TargetClass, req.TargetSection, req.Description, req.CreatedBy).Scan(&id)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to publish holiday: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Holiday published to school calendar",
		Data:    map[string]interface{}{"id": id, "title": req.Title, "date_iso": req.DateIso},
	})
}

func (h *TeacherHandler) DeleteHoliday(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	holidayID := chi.URLParam(r, "holidayId")
	if isValidUUID(holidayID) {
		_, err := h.db.Pool.Exec(r.Context(), `DELETE FROM school_holidays WHERE school_id = $1 AND id = $2::uuid`, schoolID, holidayID)
		if err != nil {
			sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to delete holiday: " + err.Error()})
			return
		}
	}
	sendJSON(w, http.StatusOK, models.ApiResponse{Success: true, Message: "Holiday removed"})
}
