package handlers

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"path/filepath"
	"strings"

	"github.com/schoolapp/backend-go/internal/models"
	"github.com/schoolapp/backend-go/internal/storage"
)

type MediaHandler struct {
	storage *storage.StorageService
}

func NewMediaHandler(storage *storage.StorageService) *MediaHandler {
	return &MediaHandler{storage: storage}
}

func (h *MediaHandler) UploadFile(w http.ResponseWriter, r *http.Request) {
	// 32MB max multipart memory
	if err := r.ParseMultipartForm(32 << 20); err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "File too large or invalid multipart form"})
		return
	}

	file, header, err := r.FormFile("file")
	if err != nil {
		sendJSON(w, http.StatusBadRequest, models.ApiResponse{Success: false, Error: "No file provided in form field 'file'"})
		return
	}
	defer file.Close()

	folder := r.FormValue("folder")
	if folder == "" {
		folder = "uploads"
	}
	classID := strings.TrimSpace(r.FormValue("class_id"))
	if classID == "" {
		classID = strings.TrimSpace(r.Header.Get("x-class-id"))
	}
	schoolSlug := strings.TrimSpace(r.FormValue("school_slug"))
	if schoolSlug == "" {
		schoolSlug = strings.TrimSpace(r.Header.Get("x-school-slug"))
	}
	if schoolSlug == "" {
		schoolSlug = "genesis-high-school"
	}

	// Build clean school/class/section/category folder hierarchy in GCS (strictly isolated per school)
	structuredFolder := storage.BuildClassSectionFolder(schoolSlug, classID, folder)

	// Detect content type
	buff := make([]byte, 512)
	_, _ = file.Read(buff)
	contentType := storage.DetectContentType(buff)
	_, _ = file.Seek(0, 0) // Reset read pointer

	if header.Header.Get("Content-Type") != "" {
		contentType = header.Header.Get("Content-Type")
	}

	ext := strings.ToLower(filepath.Ext(header.Filename))
	if ext == ".pdf" {
		contentType = "application/pdf"
	}

	// Stream upload directly to Google Cloud Storage
	objectKey, gcsURL, err := h.storage.UploadStream(r.Context(), structuredFolder, header.Filename, file, contentType)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{
			Success: false,
			Error:   "Cloud Storage upload failed: " + err.Error(),
		})
		return
	}

	scheme := "http"
	if r.TLS != nil || strings.EqualFold(r.Header.Get("X-Forwarded-Proto"), "https") {
		scheme = "https"
	}
	viewURL := fmt.Sprintf("%s://%s/api/v1/media/view?key=%s", scheme, r.Host, url.QueryEscape(objectKey))

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data: map[string]interface{}{
			"url":          viewURL,
			"gcs_url":      gcsURL,
			"object_key":   objectKey,
			"folder":       structuredFolder,
			"filename":     header.Filename,
			"content_type": contentType,
			"size_bytes":   header.Size,
			"status":       "stored_in_gcs",
		},
	})
}

// ViewFile streams a private Google Cloud Storage object directly to the mobile/web client.
func (h *MediaHandler) ViewFile(w http.ResponseWriter, r *http.Request) {
	objectKey := strings.TrimSpace(r.URL.Query().Get("key"))
	if objectKey == "" {
		http.Error(w, "Missing 'key' query parameter", http.StatusBadRequest)
		return
	}

	reader, err := h.storage.OpenReader(r.Context(), objectKey)
	if err != nil {
		http.Error(w, "File not found in Cloud Storage", http.StatusNotFound)
		return
	}
	defer reader.Close()

	if ct := reader.Attrs.ContentType; ct != "" {
		w.Header().Set("Content-Type", ct)
	} else {
		w.Header().Set("Content-Type", "application/octet-stream")
	}
	w.Header().Set("Cache-Control", "public, max-age=86400")
	w.WriteHeader(http.StatusOK)
	_, _ = io.Copy(w, reader)
}
