package handlers

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/schoolapp/backend-go/internal/database"
	"github.com/schoolapp/backend-go/internal/middleware"
	"github.com/schoolapp/backend-go/internal/models"
	"github.com/schoolapp/backend-go/internal/notifications"
)

type PostHandler struct {
	db  *database.DB
	fcm *notifications.FCMService
}

func NewPostHandler(db *database.DB, fcm *notifications.FCMService) *PostHandler {
	return &PostHandler{db: db, fcm: fcm}
}

func computeNoticeStatus(startsAt, expiresAt *time.Time, now time.Time) string {
	if startsAt != nil && startsAt.After(now) {
		return "SCHEDULED"
	}
	if expiresAt != nil && expiresAt.Before(now) {
		return "EXPIRED"
	}
	return "ACTIVE"
}

func matchesNoticeAudience(targetType, targetAudience, studentClassID string) bool {
	tt := strings.ToUpper(strings.TrimSpace(targetType))
	ta := strings.ToLower(strings.TrimSpace(targetAudience))
	if tt == "" || tt == "ALL" || ta == "" || ta == "all school" || ta == "entire school" {
		return true
	}
	cls := strings.ToLower(strings.TrimSpace(studentClassID))
	if cls == "" {
		return true
	}
	// Normalize both "class-8-c" and "Class 8 - Section C" into class token ("8") and section token ("c")
	cleanCls := strings.NewReplacer("section ", "", "class-", "", "class ", "", "–", "-", "—", "-").Replace(cls)
	clsParts := strings.FieldsFunc(cleanCls, func(r rune) bool { return r == '-' || r == ' ' })
	studentGrade := ""
	studentSec := ""
	if len(clsParts) >= 1 {
		studentGrade = clsParts[0]
	}
	if len(clsParts) >= 2 {
		studentSec = clsParts[len(clsParts)-1]
	}

	cleanTa := strings.NewReplacer("section ", "", "class-", "", "class ", "", "–", "-", "—", "-").Replace(ta)
	taParts := strings.FieldsFunc(cleanTa, func(r rune) bool { return r == '-' || r == ' ' })
	targetGrade := ""
	targetSec := ""
	if len(taParts) >= 1 {
		targetGrade = taParts[0]
	}
	if len(taParts) >= 2 {
		targetSec = taParts[len(taParts)-1]
	}

	if tt == "SECTION" || targetSec != "" {
		return studentGrade == targetGrade && studentSec == targetSec
	}
	return studentGrade == targetGrade
}

func (h *PostHandler) ListPosts(w http.ResponseWriter, r *http.Request) {
	user := middleware.GetUserFromContext(r.Context())
	schoolID := "00000000-0000-0000-0000-000000000001"
	if user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}
	role := strings.ToUpper(strings.TrimSpace(r.Header.Get("x-user-role")))
	isStaff := role == "ADMIN" || role == "TEACHER"
	studentClassID := strings.TrimSpace(r.Header.Get("x-class-id"))

	query := `
		SELECT p.id, p.school_id, p.author_id, u.full_name, p.title, p.content, p.category,
		       p.target_type, COALESCE(p.target_audience, 'Entire School'), COALESCE(p.image_url, ''),
		       p.starts_at, p.expires_at, p.is_pinned, p.created_at
		FROM posts p
		JOIN users u ON u.id = p.author_id
		WHERE p.school_id = $1
		ORDER BY p.is_pinned DESC, p.created_at DESC
		LIMIT 50
	`
	rows, err := h.db.Pool.Query(r.Context(), query, schoolID)
	if err != nil {
		sendJSON(w, http.StatusOK, models.ApiResponse{
			Success: true,
			Data: map[string]interface{}{
				"posts": []models.Post{},
			},
		})
		return
	}
	defer rows.Close()

	now := time.Now()
	posts := make([]models.Post, 0)
	for rows.Next() {
		var p models.Post
		if err := rows.Scan(
			&p.ID, &p.SchoolID, &p.AuthorID, &p.AuthorName, &p.Title, &p.Content, &p.Category,
			&p.TargetType, &p.TargetAudience, &p.ImageURL,
			&p.StartsAt, &p.ExpiresAt, &p.IsPinned, &p.CreatedAt,
		); err == nil {
			p.Status = computeNoticeStatus(p.StartsAt, p.ExpiresAt, now)
			// Students & Parents only see currently active notices matching their class/section audience
			if !isStaff {
				if p.Status != "ACTIVE" {
					continue
				}
				if !matchesNoticeAudience(p.TargetType, p.TargetAudience, studentClassID) {
					continue
				}
			}
			posts = append(posts, p)
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"posts": posts},
	})
}

func (h *PostHandler) CreateBroadcast(w http.ResponseWriter, r *http.Request) {
	user := middleware.GetUserFromContext(r.Context())
	var req struct {
		Title          string   `json:"title"`
		Content        string   `json:"content"`
		Category       string   `json:"category"`
		TargetType     string   `json:"target_type"`
		TargetClassID  *string  `json:"target_class_id"`
		TargetAudience string   `json:"target_audience"`
		ImageURL       string   `json:"image_url"`
		StartsAt       string   `json:"starts_at"`
		ExpiresAt      string   `json:"expires_at"`
		AuthorName     string   `json:"author_name"`
		Attachments    []string `json:"attachments"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON payload"})
		return
	}

	schoolID := "00000000-0000-0000-0000-000000000001"
	if user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}

	authorID, _, actorErr := resolveOrCreateActorUUID(r.Context(), h.db, r, schoolID, req.AuthorName, "ADMIN")
	if actorErr != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: actorErr.Error()})
		return
	}

	if req.Category == "" {
		req.Category = "CIRCULAR"
	}
	if req.TargetType == "" {
		req.TargetType = "ALL"
	}
	if strings.TrimSpace(req.TargetAudience) == "" {
		req.TargetAudience = "Entire School"
	}

	var startsAt *time.Time
	if strings.TrimSpace(req.StartsAt) != "" {
		if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(req.StartsAt)); err == nil {
			startsAt = &parsed
		}
	}
	var expiresAt *time.Time
	if strings.TrimSpace(req.ExpiresAt) != "" {
		if parsed, err := time.Parse(time.RFC3339, strings.TrimSpace(req.ExpiresAt)); err == nil {
			expiresAt = &parsed
		}
	}

	query := `
		INSERT INTO posts (school_id, author_id, title, content, category, target_type, target_class_id, target_audience, image_url, starts_at, expires_at, attachments)
		VALUES ($1, $2::uuid, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
		RETURNING id::text, created_at
	`
	attachmentsJSON, _ := json.Marshal(req.Attachments)
	var id string
	var createdAt time.Time
	err := h.db.Pool.QueryRow(
		r.Context(), query,
		schoolID, authorID, req.Title, req.Content, req.Category, req.TargetType,
		req.TargetClassID, req.TargetAudience, strings.TrimSpace(req.ImageURL), startsAt, expiresAt, attachmentsJSON,
	).Scan(&id, &createdAt)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to save broadcast post: " + err.Error()})
		return
	}

	status := computeNoticeStatus(startsAt, expiresAt, time.Now())

	// Send FREE real-time FCM push notification immediately if ACTIVE (not future-scheduled)
	fcmMsgID := ""
	if h.fcm != nil && status == "ACTIVE" {
		if msgID, pushErr := h.fcm.SendSchoolNotice(r.Context(), schoolID, req.Category, req.Title, req.Content, id); pushErr == nil {
			fcmMsgID = msgID
		}
	}

	sendJSON(w, http.StatusAccepted, models.ApiResponse{
		Success: true,
		Message: "Notice published successfully",
		Data: map[string]interface{}{
			"post_id":         id,
			"status":          status,
			"image_url":       strings.TrimSpace(req.ImageURL),
			"starts_at":       startsAt,
			"expires_at":      expiresAt,
			"target_audience": req.TargetAudience,
			"fcm_topic":       notifications.SchoolNoticeTopic(schoolID),
			"fcm_message_id":  fcmMsgID,
		},
	})
}
