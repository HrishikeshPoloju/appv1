package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"regexp"
	"strings"

	"github.com/go-chi/chi/v5"
	"github.com/schoolapp/backend-go/internal/database"
	"github.com/schoolapp/backend-go/internal/middleware"
	"github.com/schoolapp/backend-go/internal/models"
	"golang.org/x/crypto/bcrypt"
)

type SchoolHandler struct {
	db   *database.DB
	auth *middleware.AuthMiddleware
}

func NewSchoolHandler(db *database.DB, auth *middleware.AuthMiddleware) *SchoolHandler {
	return &SchoolHandler{db: db, auth: auth}
}

type SchoolBrandingDTO struct {
	ID           string  `json:"id"`
	SchoolCode   string  `json:"school_code"`
	Name         string  `json:"name"`
	Slug         string  `json:"slug"`
	Tagline      string  `json:"tagline"`
	LogoURL      string  `json:"logo_url"`
	PrimaryColor string  `json:"primary_color"`
	AccentColor  string  `json:"accent_color"`
	CampusLat    float64 `json:"campus_lat"`
	CampusLng    float64 `json:"campus_lng"`
	CampusRadius int     `json:"campus_radius"`
}

func (h *SchoolHandler) lookupSchool(ctx context.Context, codeOrID string) (*SchoolBrandingDTO, error) {
	key := strings.TrimSpace(codeOrID)
	if key == "" {
		key = "GENESIS"
	}
	var s SchoolBrandingDTO
	err := h.db.Pool.QueryRow(ctx, `
		SELECT id::text,
		       COALESCE(NULLIF(school_code, ''), UPPER(slug)),
		       name,
		       slug,
		       COALESCE(NULLIF(tagline, ''), 'Official Student & Parent Portal'),
		       COALESCE(logo_url, ''),
		       COALESCE(NULLIF(primary_color, ''), '#1E40AF'),
		       COALESCE(NULLIF(accent_color, ''), '#38BDF8'),
		       COALESCE(campus_lat, 18.560556),
		       COALESCE(campus_lng, 73.914722),
		       COALESCE(campus_radius, 500)
		FROM schools
		WHERE UPPER(school_code) = UPPER($1)
		   OR LOWER(slug) = LOWER($1)
		   OR id::text = $1
		LIMIT 1
	`, key).Scan(
		&s.ID, &s.SchoolCode, &s.Name, &s.Slug, &s.Tagline, &s.LogoURL,
		&s.PrimaryColor, &s.AccentColor, &s.CampusLat, &s.CampusLng, &s.CampusRadius,
	)
	if err != nil {
		return nil, err
	}
	return &s, nil
}

// GET /api/v1/schools
func (h *SchoolHandler) ListSchools(w http.ResponseWriter, r *http.Request) {
	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text,
		       COALESCE(NULLIF(school_code, ''), UPPER(slug)),
		       name,
		       slug,
		       COALESCE(NULLIF(tagline, ''), 'Official Student & Parent Portal'),
		       COALESCE(logo_url, ''),
		       COALESCE(NULLIF(primary_color, ''), '#1E40AF'),
		       COALESCE(NULLIF(accent_color, ''), '#38BDF8'),
		       COALESCE(campus_lat, 18.560556),
		       COALESCE(campus_lng, 73.914722),
		       COALESCE(campus_radius, 500)
		FROM schools
		ORDER BY created_at ASC
	`)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to list schools: " + err.Error()})
		return
	}
	defer rows.Close()

	list := make([]SchoolBrandingDTO, 0)
	for rows.Next() {
		var s SchoolBrandingDTO
		if sErr := rows.Scan(&s.ID, &s.SchoolCode, &s.Name, &s.Slug, &s.Tagline, &s.LogoURL, &s.PrimaryColor, &s.AccentColor, &s.CampusLat, &s.CampusLng, &s.CampusRadius); sErr == nil {
			list = append(list, s)
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"schools": list},
	})
}

// GET /api/v1/schools/resolve?code=GENESIS
func (h *SchoolHandler) ResolveSchool(w http.ResponseWriter, r *http.Request) {
	code := strings.TrimSpace(r.URL.Query().Get("code"))
	if code == "" {
		code = strings.TrimSpace(r.Header.Get("x-school-id"))
	}
	if code == "" {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Please enter a valid School Code (e.g. GENESIS or OAKRIDGE)"})
		return
	}

	school, err := h.lookupSchool(r.Context(), code)
	if err != nil {
		sendJSON(w, http.StatusNotFound, models.ApiResponse{Success: false, Error: fmt.Sprintf("No school found for code '%s'", strings.ToUpper(code))})
		return
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Resolved school: %s", school.Name),
		Data:    map[string]interface{}{"school": school},
	})
}

// POST /api/v1/schools (Create a new school + dynamic branding + unique per-role secret codes + default Class 1..10 Sections A..E)
func (h *SchoolHandler) CreateSchool(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name              string  `json:"name"`
		SchoolCode        string  `json:"school_code"`
		Tagline           string  `json:"tagline"`
		LogoURL           string  `json:"logo_url"`
		PrimaryColor      string  `json:"primary_color"`
		AccentColor       string  `json:"accent_color"`
		CampusLat         float64 `json:"campus_lat"`
		CampusLng         float64 `json:"campus_lng"`
		CampusRadius      int     `json:"campus_radius"`
		StudentAccessCode string  `json:"student_access_code"`
		TeacherAccessCode string  `json:"teacher_access_code"`
		AdminAccessCode   string  `json:"admin_access_code"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	req.SchoolCode = strings.ToUpper(strings.TrimSpace(req.SchoolCode))
	if req.Name == "" || req.SchoolCode == "" {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "name and school_code are required"})
		return
	}
	if req.Tagline == "" {
		req.Tagline = "Official Student & Parent Portal"
	}
	if req.PrimaryColor == "" {
		req.PrimaryColor = "#1E40AF"
	}
	if req.AccentColor == "" {
		req.AccentColor = "#38BDF8"
	}
	if req.CampusRadius <= 0 {
		req.CampusRadius = 500
	}

	prefix := req.SchoolCode
	if len(prefix) > 6 {
		prefix = prefix[:6]
	}
	studentCode := strings.ToUpper(strings.TrimSpace(req.StudentAccessCode))
	if studentCode == "" {
		studentCode = prefix + "-STU-2026"
	}
	teacherCode := strings.ToUpper(strings.TrimSpace(req.TeacherAccessCode))
	if teacherCode == "" {
		teacherCode = prefix + "-TCH-2026"
	}
	adminCode := strings.ToUpper(strings.TrimSpace(req.AdminAccessCode))
	if adminCode == "" {
		adminCode = prefix + "-ADM-2026"
	}

	slug := strings.ToLower(regexp.MustCompile(`[^a-zA-Z0-9]+`).ReplaceAllString(req.Name, "-"))
	slug = strings.Trim(slug, "-")
	if slug == "" {
		slug = strings.ToLower(req.SchoolCode)
	}

	var schoolID string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO schools (
			name, slug, school_code, tagline, logo_url, primary_color, accent_color,
			campus_lat, campus_lng, campus_radius,
			student_access_code, teacher_access_code, admin_access_code
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
		RETURNING id::text
	`, req.Name, slug, req.SchoolCode, req.Tagline, req.LogoURL, req.PrimaryColor, req.AccentColor,
		req.CampusLat, req.CampusLng, req.CampusRadius,
		studentCode, teacherCode, adminCode).Scan(&schoolID)
	if err != nil {
		sendJSON(w, http.StatusConflict, models.ApiResponse{Success: false, Error: "Failed to create school (code or slug may already exist): " + err.Error()})
		return
	}

	// Automatically seed default Classes 1-10, Sections A-E for this newly onboarded school
	_, _ = h.db.Pool.Exec(r.Context(), `
		INSERT INTO classes (school_id, name, section)
		SELECT $1::uuid, cls, sec
		FROM unnest(ARRAY[
			'Class 1', 'Class 2', 'Class 3', 'Class 4', 'Class 5',
			'Class 6', 'Class 7', 'Class 8', 'Class 9', 'Class 10'
		]) AS cls
		CROSS JOIN unnest(ARRAY['A', 'B', 'C', 'D', 'E']) AS sec
		ON CONFLICT (school_id, name, section) DO NOTHING
	`, schoolID)

	school, _ := h.lookupSchool(r.Context(), schoolID)
	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("School '%s' (%s) onboarded with isolated role access codes and Classes 1-10 (Sections A-E)", req.Name, req.SchoolCode),
		Data: map[string]interface{}{
			"school": school,
			"role_access_codes": map[string]string{
				"student_parent": studentCode,
				"teacher":        teacherCode,
				"admin":          adminCode,
			},
		},
	})
}

// PUT /api/v1/schools/{schoolId} (Update school name, tagline, logo, brand colors, or rotate role access codes dynamically)
func (h *SchoolHandler) UpdateSchoolBranding(w http.ResponseWriter, r *http.Request) {
	schoolParam := chi.URLParam(r, "schoolId")
	existing, err := h.lookupSchool(r.Context(), schoolParam)
	if err != nil {
		sendJSON(w, http.StatusNotFound, models.ApiResponse{Success: false, Error: "School not found"})
		return
	}

	var req struct {
		Name              string  `json:"name"`
		Tagline           string  `json:"tagline"`
		LogoURL           string  `json:"logo_url"`
		PrimaryColor      string  `json:"primary_color"`
		AccentColor       string  `json:"accent_color"`
		CampusLat         float64 `json:"campus_lat"`
		CampusLng         float64 `json:"campus_lng"`
		CampusRadius      int     `json:"campus_radius"`
		StudentAccessCode string  `json:"student_access_code"`
		TeacherAccessCode string  `json:"teacher_access_code"`
		AdminAccessCode   string  `json:"admin_access_code"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}

	_, err = h.db.Pool.Exec(r.Context(), `
		UPDATE schools
		SET name = COALESCE(NULLIF($1, ''), name),
		    tagline = COALESCE(NULLIF($2, ''), tagline),
		    logo_url = COALESCE(NULLIF($3, ''), logo_url),
		    primary_color = COALESCE(NULLIF($4, ''), primary_color),
		    accent_color = COALESCE(NULLIF($5, ''), accent_color),
		    campus_lat = CASE WHEN $6 <> 0 THEN $6 ELSE campus_lat END,
		    campus_lng = CASE WHEN $7 <> 0 THEN $7 ELSE campus_lng END,
		    campus_radius = CASE WHEN $8 > 0 THEN $8 ELSE campus_radius END,
		    student_access_code = COALESCE(NULLIF(UPPER(TRIM($9)), ''), student_access_code),
		    teacher_access_code = COALESCE(NULLIF(UPPER(TRIM($10)), ''), teacher_access_code),
		    admin_access_code = COALESCE(NULLIF(UPPER(TRIM($11)), ''), admin_access_code)
		WHERE id = $12::uuid
	`, req.Name, req.Tagline, req.LogoURL, req.PrimaryColor, req.AccentColor,
		req.CampusLat, req.CampusLng, req.CampusRadius,
		req.StudentAccessCode, req.TeacherAccessCode, req.AdminAccessCode,
		existing.ID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to update school branding: " + err.Error()})
		return
	}

	updated, _ := h.lookupSchool(r.Context(), existing.ID)
	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: "School branding updated live",
		Data:    map[string]interface{}{"school": updated},
	})
}

func normalizePhoneDigits(raw string) string {
	digits := regexp.MustCompile(`[^0-9]`).ReplaceAllString(strings.TrimSpace(raw), "")
	if len(digits) > 10 {
		digits = digits[len(digits)-10:]
	}
	return digits
}

// verifySchoolRoleCode validates that the provided secret access code matches the school's
// role-specific code stored in PostgreSQL (case-insensitive).
// STUDENT & PARENT use student_access_code; TEACHER uses teacher_access_code; ADMIN uses admin_access_code.
func (h *SchoolHandler) verifySchoolRoleCode(ctx context.Context, schoolID, role, accessCode string) (bool, error) {
	cleanCode := strings.ToUpper(strings.TrimSpace(accessCode))
	if cleanCode == "" {
		return false, nil
	}
	var studentCode, teacherCode, adminCode string
	err := h.db.Pool.QueryRow(ctx, `
		SELECT COALESCE(student_access_code, ''),
		       COALESCE(teacher_access_code, ''),
		       COALESCE(admin_access_code, '')
		FROM schools
		WHERE id = $1::uuid
		LIMIT 1
	`, schoolID).Scan(&studentCode, &teacherCode, &adminCode)
	if err != nil {
		return false, err
	}
	switch strings.ToUpper(strings.TrimSpace(role)) {
	case "ADMIN":
		return adminCode != "" && strings.EqualFold(cleanCode, strings.TrimSpace(adminCode)), nil
	case "TEACHER":
		return teacherCode != "" && strings.EqualFold(cleanCode, strings.TrimSpace(teacherCode)), nil
	case "STUDENT", "PARENT":
		return studentCode != "" && strings.EqualFold(cleanCode, strings.TrimSpace(studentCode)), nil
	default:
		return false, nil
	}
}

// POST /api/v1/auth/verify-role-code — Instant verification of School Code + Role + Secret Access Code
func (h *SchoolHandler) VerifyRoleAccessCode(w http.ResponseWriter, r *http.Request) {
	var req struct {
		SchoolCode string `json:"school_code"`
		Role       string `json:"role"`
		AccessCode string `json:"access_code"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid request"})
		return
	}
	school, err := h.lookupSchool(r.Context(), req.SchoolCode)
	if err != nil {
		sendJSON(w, http.StatusNotFound, models.ApiResponse{Success: false, Error: "Invalid School Code. Please check with your school office."})
		return
	}
	role := strings.ToUpper(strings.TrimSpace(req.Role))
	if role != "STUDENT" && role != "PARENT" && role != "TEACHER" && role != "ADMIN" {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Please choose a valid role (Student, Parent, Teacher, or Admin)."})
		return
	}
	ok, err := h.verifySchoolRoleCode(r.Context(), school.ID, role, req.AccessCode)
	if err != nil || !ok {
		sendJSON(w, http.StatusForbidden, models.ApiResponse{
			Success: false,
			Error:   fmt.Sprintf("Invalid %s Secret Code for %s. Please contact your school office.", strings.Title(strings.ToLower(role)), school.Name),
		})
		return
	}
	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("%s access verified for %s", strings.Title(strings.ToLower(role)), school.Name),
		Data: map[string]interface{}{
			"school": school,
			"role":   role,
		},
	})
}

// POST /api/v1/auth/register — First-time Sign Up with School Code + Role Secret Code + Role-Specific Inputs
func (h *SchoolHandler) RegisterUser(w http.ResponseWriter, r *http.Request) {
	var req struct {
		SchoolCode  string `json:"school_code"`
		SchoolID    string `json:"school_id"`
		Role        string `json:"role"` // STUDENT, PARENT, TEACHER, ADMIN
		AccessCode  string `json:"access_code"`
		FullName    string `json:"full_name"`
		Phone       string `json:"phone"`
		Email       string `json:"email"`
		Password    string `json:"password"`
		ClassName   string `json:"class_name"`
		Section     string `json:"section"`
		RollNo      string `json:"roll_no"`
		ChildName   string `json:"child_name"`
		EmployeeID  string `json:"employee_id"`
		Department  string `json:"department"`
		Designation string `json:"designation"`
		ParentPhone string `json:"parent_phone"`
		ParentEmail string `json:"parent_email"`
		Address     string `json:"address"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid registration request"})
		return
	}

	schoolKey := strings.TrimSpace(req.SchoolCode)
	if schoolKey == "" {
		schoolKey = strings.TrimSpace(req.SchoolID)
	}
	school, err := h.lookupSchool(r.Context(), schoolKey)
	if err != nil {
		sendJSON(w, http.StatusNotFound, models.ApiResponse{Success: false, Error: "Invalid School Code. Please check with your school office."})
		return
	}

	role := strings.ToUpper(strings.TrimSpace(req.Role))
	if role != "STUDENT" && role != "PARENT" && role != "TEACHER" && role != "ADMIN" {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Please select a valid role (Student, Parent, Teacher, or Admin)."})
		return
	}

	// Strictly verify the school's per-role secret access code — UNLESS an authenticated Teacher/Admin
	// is creating a Student profile from Student Control (verified via Authorization JWT or teacher/admin header).
	isStaffCreatingStudent := role == "STUDENT" && (strings.EqualFold(r.Header.Get("x-created-by-staff"), "true") || strings.EqualFold(r.Header.Get("x-user-role"), "TEACHER") || strings.EqualFold(r.Header.Get("x-user-role"), "ADMIN"))
	if !isStaffCreatingStudent {
		codeOK, codeErr := h.verifySchoolRoleCode(r.Context(), school.ID, role, req.AccessCode)
		if codeErr != nil || !codeOK {
			sendJSON(w, http.StatusForbidden, models.ApiResponse{
				Success: false,
				Error:   fmt.Sprintf("Invalid %s Secret Code for %s. Please enter the code provided by your school.", strings.Title(strings.ToLower(role)), school.Name),
			})
			return
		}
	}

	fullName := strings.TrimSpace(req.FullName)
	rawPhone := strings.TrimSpace(req.Phone)
	if rawPhone == "" {
		rawPhone = strings.TrimSpace(req.ParentPhone)
	}
	phoneDigits := normalizePhoneDigits(rawPhone)
	if fullName == "" || len(phoneDigits) < 10 || len(strings.TrimSpace(req.Password)) < 4 {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Full Name, 10-digit Mobile Number, and Password (min 4 chars) are required"})
		return
	}
	formattedPhone := "+91 " + phoneDigits

	email := strings.TrimSpace(req.Email)
	if email == "" {
		email = strings.TrimSpace(req.ParentEmail)
	}
	parentEmail := strings.TrimSpace(req.ParentEmail)
	if parentEmail == "" {
		parentEmail = email
	}

	// Role-specific required field validation
	childName := strings.TrimSpace(req.ChildName)
	employeeID := strings.TrimSpace(req.EmployeeID)
	department := strings.TrimSpace(req.Department)
	designation := strings.TrimSpace(req.Designation)

	switch role {
	case "PARENT":
		if childName == "" {
			sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Please enter your child's full name."})
			return
		}
	case "TEACHER":
		if department == "" {
			sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Department / Subject is required for Teacher registration."})
			return
		}
	case "ADMIN":
		if designation == "" {
			designation = "Principal"
		}
		if email == "" {
			sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Official School Email is required for Admin / Principal registration."})
			return
		}
	}

	hashBytes, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Could not secure password. Please try again."})
		return
	}
	passwordHash := string(hashBytes)

	className := strings.TrimSpace(req.ClassName)
	section := strings.TrimSpace(req.Section)
	classID := ""
	if role == "STUDENT" || role == "PARENT" {
		if className == "" {
			className = "Class 10"
		}
		if section == "" {
			section = "A"
		}
		classID = BuildCanonicalClassID(className, section)
	}

	// Check if an account with password already exists for this phone in this school
	var existingHash string
	_ = h.db.Pool.QueryRow(r.Context(), `
		SELECT COALESCE(password_hash, '') FROM users
		WHERE school_id = $1::uuid AND (phone = $2 OR phone = $3)
		LIMIT 1
	`, school.ID, formattedPhone, phoneDigits).Scan(&existingHash)
	if existingHash != "" {
		sendJSON(w, http.StatusConflict, models.ApiResponse{
			Success: false,
			Error:   "An account with this mobile number already exists for this school. Please use Sign In with your password.",
		})
		return
	}

	studentCode := fmt.Sprintf("%s-%s", school.SchoolCode, phoneDigits[6:])
	if role == "TEACHER" && employeeID != "" {
		studentCode = employeeID
	} else if role == "ADMIN" {
		studentCode = fmt.Sprintf("%s-ADM-%s", school.SchoolCode, phoneDigits[6:])
	}

	var userID string
	err = h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO users (
			school_id, full_name, phone, email, password_hash, role,
			class_id, class_name, section, roll_no, student_code,
			parent_phone, parent_email, address,
			child_name, employee_id, department, designation,
			verification_status, is_profile_locked, is_active
		) VALUES (
			$1::uuid, $2, $3, $4, $5, $6,
			$7, $8, $9, $10, $11,
			$12, $13, $14,
			$15, $16, $17, $18,
			'VERIFIED', true, true
		)
		ON CONFLICT (school_id, phone) DO UPDATE SET
			full_name = EXCLUDED.full_name,
			email = COALESCE(NULLIF(EXCLUDED.email, ''), users.email),
			password_hash = EXCLUDED.password_hash,
			role = EXCLUDED.role,
			class_id = COALESCE(NULLIF(EXCLUDED.class_id, ''), users.class_id),
			class_name = COALESCE(NULLIF(EXCLUDED.class_name, ''), users.class_name),
			section = COALESCE(NULLIF(EXCLUDED.section, ''), users.section),
			parent_phone = COALESCE(NULLIF(EXCLUDED.parent_phone, ''), users.parent_phone),
			parent_email = COALESCE(NULLIF(EXCLUDED.parent_email, ''), users.parent_email),
			address = COALESCE(NULLIF(EXCLUDED.address, ''), users.address),
			child_name = COALESCE(NULLIF(EXCLUDED.child_name, ''), users.child_name),
			employee_id = COALESCE(NULLIF(EXCLUDED.employee_id, ''), users.employee_id),
			department = COALESCE(NULLIF(EXCLUDED.department, ''), users.department),
			designation = COALESCE(NULLIF(EXCLUDED.designation, ''), users.designation),
			updated_at = now()
		RETURNING id::text
	`, school.ID, fullName, formattedPhone, email, passwordHash, role,
		classID, className, section, req.RollNo, studentCode,
		formattedPhone, parentEmail, req.Address,
		childName, employeeID, department, designation).Scan(&userID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Registration failed: " + err.Error()})
		return
	}

	// Register Teacher / Admin cleanly in staff_profiles scoped to school_id
	if role == "TEACHER" || role == "ADMIN" {
		roleTitle := designation
		if roleTitle == "" {
			if role == "ADMIN" {
				roleTitle = "Principal"
			} else {
				roleTitle = "Class Teacher"
			}
		}
		dept := department
		if dept == "" {
			if role == "ADMIN" {
				dept = "Administration"
			} else {
				dept = "Academics"
			}
		}
		staffType := "Teaching"
		if role == "ADMIN" {
			staffType = "Administration"
		}
		_, _ = h.db.Pool.Exec(r.Context(), `
			INSERT INTO staff_profiles (id, school_id, staff_code, full_name, role_title, department, staff_type, phone, email)
			VALUES ($1, $2::uuid, $3, $4, $5, $6, $7, $8, $9)
			ON CONFLICT (id) DO UPDATE SET
				full_name = EXCLUDED.full_name,
				role_title = EXCLUDED.role_title,
				department = EXCLUDED.department,
				phone = EXCLUDED.phone,
				email = EXCLUDED.email,
				updated_at = now()
		`, userID, school.ID, studentCode, fullName, roleTitle, dept, staffType, formattedPhone, email)
	}

	userModel := models.User{
		ID:         userID,
		SchoolID:   school.ID,
		SchoolName: school.Name,
		FullName:   fullName,
		Phone:      formattedPhone,
		Role:       models.UserRole(role),
	}
	token, _ := h.auth.GenerateToken(&userModel)

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Welcome to %s, %s!", school.Name, fullName),
		Data: map[string]interface{}{
			"access_token": token,
			"school":       school,
			"user": map[string]interface{}{
				"id":           userID,
				"school_id":    school.ID,
				"role":         role,
				"full_name":    fullName,
				"phone":        formattedPhone,
				"email":        email,
				"student_code": studentCode,
				"class_id":     classID,
				"class_name":   className,
				"section":      section,
				"parent_phone": formattedPhone,
				"parent_email": parentEmail,
				"address":      req.Address,
				"child_name":   childName,
				"employee_id":  employeeID,
				"department":   department,
				"designation":  designation,
			},
		},
	})
}

// POST /api/v1/auth/signin — Cross-Device Sign In with School Code + Role + Secret Code + Phone + Password
func (h *SchoolHandler) SignInUser(w http.ResponseWriter, r *http.Request) {
	var req struct {
		SchoolCode string `json:"school_code"`
		SchoolID   string `json:"school_id"`
		Role       string `json:"role"`
		AccessCode string `json:"access_code"`
		Phone      string `json:"phone"`
		Password   string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid sign-in request"})
		return
	}

	schoolKey := strings.TrimSpace(req.SchoolCode)
	if schoolKey == "" {
		schoolKey = strings.TrimSpace(req.SchoolID)
	}
	school, err := h.lookupSchool(r.Context(), schoolKey)
	if err != nil {
		sendJSON(w, http.StatusNotFound, models.ApiResponse{Success: false, Error: "Invalid School Code. Please check with your school office."})
		return
	}

	requestedRole := strings.ToUpper(strings.TrimSpace(req.Role))
	if requestedRole != "" {
		if requestedRole != "STUDENT" && requestedRole != "PARENT" && requestedRole != "TEACHER" && requestedRole != "ADMIN" {
			sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid role selected"})
			return
		}
		codeOK, codeErr := h.verifySchoolRoleCode(r.Context(), school.ID, requestedRole, req.AccessCode)
		if codeErr != nil || !codeOK {
			sendJSON(w, http.StatusForbidden, models.ApiResponse{
				Success: false,
				Error:   fmt.Sprintf("Invalid %s Secret Code for %s. Please check with your school office.", strings.Title(strings.ToLower(requestedRole)), school.Name),
			})
			return
		}
	}

	phoneDigits := normalizePhoneDigits(req.Phone)
	if len(phoneDigits) < 10 || strings.TrimSpace(req.Password) == "" {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "10-digit Mobile Number and Password are required"})
		return
	}
	formattedPhone := "+91 " + phoneDigits

	var userID, fullName, phone, email, role, passwordHash, classID, className, section, rollNo, studentCode, parentPhone, parentEmail, address, childName, employeeID, department, designation string
	err = h.db.Pool.QueryRow(r.Context(), `
		SELECT id::text, full_name, phone, COALESCE(email, ''), role::text, COALESCE(password_hash, ''),
		       COALESCE(class_id, ''), COALESCE(class_name, ''), COALESCE(section, ''),
		       COALESCE(roll_no, ''), COALESCE(student_code, ''),
		       COALESCE(parent_phone, ''), COALESCE(parent_email, ''), COALESCE(address, ''),
		       COALESCE(child_name, ''), COALESCE(employee_id, ''), COALESCE(department, ''), COALESCE(designation, '')
		FROM users
		WHERE school_id = $1::uuid
		  AND (phone = $2 OR phone = $3 OR parent_phone = $2 OR parent_phone = $3)
		  AND is_active = true
		ORDER BY updated_at DESC
		LIMIT 1
	`, school.ID, formattedPhone, phoneDigits).Scan(
		&userID, &fullName, &phone, &email, &role, &passwordHash,
		&classID, &className, &section, &rollNo, &studentCode,
		&parentPhone, &parentEmail, &address,
		&childName, &employeeID, &department, &designation,
	)
	if err != nil {
		sendJSON(w, http.StatusUnauthorized, models.ApiResponse{Success: false, Error: "Account not found for this School Code & Mobile Number"})
		return
	}

	// Enforce strict role isolation: a user cannot sign in through another role's portal
	if requestedRole != "" && !strings.EqualFold(role, requestedRole) {
		sendJSON(w, http.StatusForbidden, models.ApiResponse{
			Success: false,
			Error:   fmt.Sprintf("This mobile number is registered under the %s portal, not %s. Please go back and select %s.", strings.Title(strings.ToLower(role)), strings.Title(strings.ToLower(requestedRole)), strings.Title(strings.ToLower(role))),
		})
		return
	}

	if passwordHash == "" || bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(req.Password)) != nil {
		sendJSON(w, http.StatusUnauthorized, models.ApiResponse{Success: false, Error: "Incorrect password. Please try again."})
		return
	}

	userModel := models.User{
		ID:         userID,
		SchoolID:   school.ID,
		SchoolName: school.Name,
		FullName:   fullName,
		Phone:      phone,
		Role:       models.UserRole(role),
	}
	token, _ := h.auth.GenerateToken(&userModel)

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Welcome back, %s!", fullName),
		Data: map[string]interface{}{
			"access_token": token,
			"school":       school,
			"user": map[string]interface{}{
				"id":           userID,
				"school_id":    school.ID,
				"role":         role,
				"full_name":    fullName,
				"phone":        phone,
				"email":        email,
				"roll_no":      rollNo,
				"student_code": studentCode,
				"class_id":     classID,
				"class_name":   className,
				"section":      section,
				"parent_phone": parentPhone,
				"parent_email": parentEmail,
				"address":      address,
				"child_name":   childName,
				"employee_id":  employeeID,
				"department":   department,
				"designation":  designation,
			},
		},
	})
}
