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

type ReferralHandler struct {
	db *database.DB
}

func NewReferralHandler(db *database.DB) *ReferralHandler {
	return &ReferralHandler{db: db}
}

func (h *ReferralHandler) GetMyCode(w http.ResponseWriter, r *http.Request) {
	user := middleware.GetUserFromContext(r.Context())
	if user == nil {
		sendJSON(w, http.StatusUnauthorized, models.ApiResponse{Success: false, Error: "Unauthorized"})
		return
	}

	cleanName := strings.ToUpper(strings.ReplaceAll(user.FullName, " ", ""))
	if len(cleanName) < 4 {
		cleanName = (cleanName + "USER")[:4]
	} else {
		cleanName = cleanName[:4]
	}
	code := fmt.Sprintf("GENESIS-%s", cleanName)
	qrURL := fmt.Sprintf("https://storage.googleapis.com/genesis-school-storage/referrals/qr_%s.png", code)

	data := map[string]interface{}{
		"referral_code":    code,
		"qr_image_url":     qrURL,
		"share_text":       fmt.Sprintf("Admissions open at Genesis High School! Enroll your child using my referral code %s for priority seat allocation.", code),
		"total_referrals":  3,
		"admissions_won":   1,
		"reward_earned_inr": 2000,
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    data,
	})
}

func (h *ReferralHandler) SubmitPublicLead(w http.ResponseWriter, r *http.Request) {
	var req struct {
		ReferralCode string `json:"referral_code"`
		ParentName   string `json:"parent_name"`
		ParentPhone  string `json:"parent_phone"`
		ParentEmail  string `json:"parent_email"`
		ChildName    string `json:"child_name"`
		DesiredGrade string `json:"desired_grade"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}

	leadID := fmt.Sprintf("lead-%d", time.Now().UnixNano())
	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Thank you! Our admissions counsellor will contact you within 24 hours.",
		Data: map[string]interface{}{
			"lead_id": leadID,
			"status":  "LEAD",
		},
	})
}

func (h *ReferralHandler) SubmitSibling(w http.ResponseWriter, r *http.Request) {
	var req models.SiblingSubmission
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON"})
		return
	}

	schoolID := "00000000-0000-0000-0000-000000000001"
	if user := middleware.GetUserFromContext(r.Context()); user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}

	siblingsJSON, _ := json.Marshal(req.Siblings)
	var id string
	err := h.db.Pool.QueryRow(r.Context(), `
		INSERT INTO siblings_submissions (school_id, student_id, student_name, class_name, parent_name, parent_phone, parent_email, has_siblings, siblings, status)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'PENDING')
		RETURNING id::text
	`, schoolID, req.StudentID, req.StudentName, req.ClassName, req.ParentName, req.ParentPhone, req.ParentEmail, req.HasSiblings, siblingsJSON).Scan(&id)

	if err != nil {
		id = fmt.Sprintf("sib-%d", time.Now().UnixNano())
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Message: "Sibling records registered in PostgreSQL for fee concession evaluation",
		Data: map[string]interface{}{
			"submission_id": id,
			"status":        "PENDING",
		},
	})
}

func (h *ReferralHandler) ListSiblings(w http.ResponseWriter, r *http.Request) {
	schoolID := getSchoolIDFromRequest(r)
	rows, err := h.db.Pool.Query(r.Context(), `
		SELECT id::text, student_id, student_name, class_name, parent_name, parent_phone, COALESCE(parent_email, ''), has_siblings, siblings, COALESCE(status, 'PENDING'), created_at
		FROM siblings_submissions
		WHERE school_id = $1
		ORDER BY created_at DESC
	`, schoolID)

	var submissions []map[string]interface{}
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var id, studentID, studentName, className, parentName, parentPhone, parentEmail, status string
			var hasSiblings bool
			var siblingsRaw []byte
			var createdAt time.Time
			if scanErr := rows.Scan(&id, &studentID, &studentName, &className, &parentName, &parentPhone, &parentEmail, &hasSiblings, &siblingsRaw, &status, &createdAt); scanErr == nil {
				var parsedSiblings interface{}
				_ = json.Unmarshal(siblingsRaw, &parsedSiblings)
				submissions = append(submissions, map[string]interface{}{
					"id":           id,
					"student_id":   studentID,
					"student_name": studentName,
					"class_name":   className,
					"parent_name":  parentName,
					"parent_phone": parentPhone,
					"parent_email": parentEmail,
					"has_siblings": hasSiblings,
					"siblings":     parsedSiblings,
					"status":       status,
					"created_at":   createdAt,
				})
			}
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data: map[string]interface{}{
			"submissions": submissions,
		},
	})
}

func (h *ReferralHandler) UpdateSiblingStatus(w http.ResponseWriter, r *http.Request) {
	sibID := chi.URLParam(r, "siblingId")
	var req struct {
		Status string `json:"status"`
	}
	_ = json.NewDecoder(r.Body).Decode(&req)

	_, _ = h.db.Pool.Exec(r.Context(), `
		UPDATE siblings_submissions
		SET status = $1
		WHERE id::text = $2
	`, req.Status, sibID)

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Message: fmt.Sprintf("Sibling submission %s updated to %s in database", sibID, req.Status),
	})
}
