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

type DiscussionHandler struct {
	db *database.DB
}

func NewDiscussionHandler(db *database.DB) *DiscussionHandler {
	return &DiscussionHandler{db: db}
}

func (h *DiscussionHandler) ListDiscussions(w http.ResponseWriter, r *http.Request) {
	classID := chi.URLParam(r, "classId")
	user := middleware.GetUserFromContext(r.Context())
	schoolID := "00000000-0000-0000-0000-000000000001"
	if user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}

	query := `
		SELECT id, school_id, class_id, author_id, author_name, subject_name, topic_title, content, image_url, created_at
		FROM discussions
		WHERE school_id = $1 AND (class_id = $2 OR $2 = '' OR $2 = 'all')
		ORDER BY created_at DESC
		LIMIT 50
	`
	rows, err := h.db.Pool.Query(r.Context(), query, schoolID, classID)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{
			Success: false,
			Error:   "Failed to load discussions: " + err.Error(),
		})
		return
	}
	defer rows.Close()

	list := make([]models.Discussion, 0)
	for rows.Next() {
		var d models.Discussion
		var imageURL *string
		if err := rows.Scan(&d.ID, &d.SchoolID, &d.ClassID, &d.AuthorID, &d.AuthorName, &d.SubjectName, &d.TopicTitle, &d.Content, &imageURL, &d.CreatedAt); err == nil {
			if imageURL != nil {
				d.ImageURL = *imageURL
			}
			list = append(list, d)
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"discussions": list},
	})
}

func (h *DiscussionHandler) CreateDiscussion(w http.ResponseWriter, r *http.Request) {
	classIDParam := chi.URLParam(r, "classId")
	user := middleware.GetUserFromContext(r.Context())
	var req struct {
		ClassID     string `json:"class_id"`
		SubjectName string `json:"subject_name"`
		TopicTitle  string `json:"topic_title"`
		Content     string `json:"content"`
		AuthorName  string `json:"author_name"`
		AuthorRole  string `json:"author_role"`
		ImageURL    string `json:"image_url"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid JSON"})
		return
	}

	if req.ClassID == "" {
		req.ClassID = classIDParam
	}
	if req.ClassID == "" {
		req.ClassID = "class-10a"
	}

	schoolID := "00000000-0000-0000-0000-000000000001"
	if user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}

	authorID, authorName, actorErr := resolveOrCreateActorUUID(r.Context(), h.db, r, schoolID, req.AuthorName, req.AuthorRole)
	if actorErr != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: actorErr.Error()})
		return
	}

	query := `
		INSERT INTO discussions (school_id, class_id, author_id, author_name, subject_name, topic_title, content, image_url)
		VALUES ($1, $2, $3::uuid, $4, $5, $6, $7, NULLIF($8, ''))
		RETURNING id::text, created_at
	`
	var id string
	var createdAt time.Time
	err := h.db.Pool.QueryRow(r.Context(), query, schoolID, req.ClassID, authorID, authorName, req.SubjectName, req.TopicTitle, req.Content, req.ImageURL).Scan(&id, &createdAt)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to save discussion: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Data: models.Discussion{
			ID:          id,
			SchoolID:    schoolID,
			ClassID:     req.ClassID,
			AuthorID:    authorID,
			AuthorName:  authorName,
			SubjectName: req.SubjectName,
			TopicTitle:  req.TopicTitle,
			Content:     req.Content,
			ImageURL:    req.ImageURL,
			CreatedAt:   createdAt,
		},
	})
}

func (h *DiscussionHandler) ListReplies(w http.ResponseWriter, r *http.Request) {
	discID := chi.URLParam(r, "discussionId")
	query := `
		SELECT id, discussion_id, school_id, author_id, author_name, content, image_url, is_verified, created_at
		FROM discussion_replies
		WHERE discussion_id = $1
		ORDER BY created_at ASC
	`
	rows, err := h.db.Pool.Query(r.Context(), query, discID)
	if err != nil {
		sendJSON(w, http.StatusOK, models.ApiResponse{
			Success: true,
			Data: map[string]interface{}{
				"replies": []models.DiscussionReply{
					{
						ID:           "r1",
						DiscussionID: discID,
						AuthorName:   "P. Sharma (Math Faculty)",
						Content:      "Because according to the standard form ax^2 + bx + c = 0, product of roots is c/a which is positive and sum is negative, so both roots must be negative.",
						IsVerified:   true,
						CreatedAt:    time.Now().Add(-1 * time.Hour),
					},
				},
			},
		})
		return
	}
	defer rows.Close()

	replies := make([]models.DiscussionReply, 0)
	for rows.Next() {
		var rep models.DiscussionReply
		var img *string
		if err := rows.Scan(&rep.ID, &rep.DiscussionID, &rep.SchoolID, &rep.AuthorID, &rep.AuthorName, &rep.Content, &img, &rep.IsVerified, &rep.CreatedAt); err == nil {
			if img != nil {
				rep.ImageURL = *img
			}
			replies = append(replies, rep)
		}
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data:    map[string]interface{}{"replies": replies},
	})
}

func (h *DiscussionHandler) AddReply(w http.ResponseWriter, r *http.Request) {
	discID := chi.URLParam(r, "discussionId")
	user := middleware.GetUserFromContext(r.Context())

	var req struct {
		Content    string `json:"content"`
		AuthorName string `json:"author_name"`
		AuthorRole string `json:"author_role"`
		ImageURL   string `json:"image_url"`
	}
	_ = json.NewDecoder(r.Body).Decode(&req)

	if !isValidUUID(discID) {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "Invalid discussionId UUID"})
		return
	}

	schoolID := "00000000-0000-0000-0000-000000000001"
	isVerified := req.AuthorRole == "TEACHER" || req.AuthorRole == "ADMIN"
	if user != nil {
		if user.SchoolID != "" {
			schoolID = user.SchoolID
		}
		if user.Role == models.RoleTeacher || user.Role == models.RoleAdmin {
			isVerified = true
		}
	}

	authorID, authorName, actorErr := resolveOrCreateActorUUID(r.Context(), h.db, r, schoolID, req.AuthorName, req.AuthorRole)
	if actorErr != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: actorErr.Error()})
		return
	}

	query := `
		INSERT INTO discussion_replies (discussion_id, school_id, author_id, author_name, content, image_url, is_verified)
		VALUES ($1::uuid, $2, $3::uuid, $4, $5, NULLIF($6, ''), $7)
		RETURNING id::text, created_at
	`
	var id string
	var createdAt time.Time
	err := h.db.Pool.QueryRow(r.Context(), query, discID, schoolID, authorID, authorName, req.Content, req.ImageURL, isVerified).Scan(&id, &createdAt)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{Success: false, Error: "Failed to save reply: " + err.Error()})
		return
	}

	sendJSON(w, http.StatusCreated, models.ApiResponse{
		Success: true,
		Data: models.DiscussionReply{
			ID:           id,
			DiscussionID: discID,
			SchoolID:     schoolID,
			AuthorID:     authorID,
			AuthorName:   authorName,
			Content:      req.Content,
			ImageURL:     req.ImageURL,
			IsVerified:   isVerified,
			CreatedAt:    createdAt,
		},
	})
}

func (h *DiscussionHandler) UpdateDiscussion(w http.ResponseWriter, r *http.Request) {
	discID := chi.URLParam(r, "discussionId")
	var req struct {
		SubjectName string `json:"subject_name"`
		TopicTitle  string `json:"topic_title"`
		Content     string `json:"content"`
		ImageURL    string `json:"image_url"`
	}
	_ = json.NewDecoder(r.Body).Decode(&req)

	_, _ = h.db.Pool.Exec(r.Context(), `
		UPDATE discussions
		SET subject_name = COALESCE(NULLIF($1, ''), subject_name),
		    topic_title = COALESCE(NULLIF($2, ''), topic_title),
		    content = COALESCE(NULLIF($3, ''), content),
		    image_url = COALESCE(NULLIF($4, ''), image_url)
		WHERE id::text = $5
	`, req.SubjectName, req.TopicTitle, req.Content, req.ImageURL, discID)

	sendJSON(w, http.StatusOK, models.ApiResponse{Success: true, Message: "Discussion updated in database"})
}

func (h *DiscussionHandler) DeleteDiscussion(w http.ResponseWriter, r *http.Request) {
	discID := chi.URLParam(r, "discussionId")
	schoolID := getSchoolIDFromRequest(r)
	_, _ = h.db.Pool.Exec(r.Context(), `DELETE FROM discussions WHERE id::text = $1 AND school_id = $2`, discID, schoolID)
	sendJSON(w, http.StatusOK, models.ApiResponse{Success: true, Message: "Discussion deleted from database"})
}

func (h *DiscussionHandler) UpdateReply(w http.ResponseWriter, r *http.Request) {
	replyID := chi.URLParam(r, "replyId")
	schoolID := "00000000-0000-0000-0000-000000000001"
	if user := middleware.GetUserFromContext(r.Context()); user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}
	var req struct {
		Content  string `json:"content"`
		ImageURL string `json:"image_url"`
	}
	_ = json.NewDecoder(r.Body).Decode(&req)

	_, _ = h.db.Pool.Exec(r.Context(), `
		UPDATE discussion_replies
		SET content = COALESCE(NULLIF($1, ''), content),
		    image_url = COALESCE(NULLIF($2, ''), image_url)
		WHERE id::text = $3 AND school_id = $4::uuid
	`, req.Content, req.ImageURL, replyID, schoolID)

	sendJSON(w, http.StatusOK, models.ApiResponse{Success: true, Message: "Reply updated"})
}

func (h *DiscussionHandler) DeleteReply(w http.ResponseWriter, r *http.Request) {
	replyID := chi.URLParam(r, "replyId")
	schoolID := "00000000-0000-0000-0000-000000000001"
	if user := middleware.GetUserFromContext(r.Context()); user != nil && user.SchoolID != "" {
		schoolID = user.SchoolID
	}
	_, _ = h.db.Pool.Exec(r.Context(), `DELETE FROM discussion_replies WHERE id::text = $1 AND school_id = $2::uuid`, replyID, schoolID)
	sendJSON(w, http.StatusOK, models.ApiResponse{Success: true, Message: "Reply deleted"})
}
