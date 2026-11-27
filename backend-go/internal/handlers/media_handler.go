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
	storage storage.ObjectStore
}

func NewMediaHandler(store storage.ObjectStore) *MediaHandler {
	return &MediaHandler{storage: store}
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

	// Stream upload directly to configured Object Storage (GCS or Cloudflare R2)
	objectKey, directURL, err := h.storage.UploadStream(r.Context(), structuredFolder, header.Filename, file, contentType)
	if err != nil {
		sendJSON(w, http.StatusInternalServerError, models.ApiResponse{
			Success: false,
			Error:   "Storage upload failed: " + err.Error(),
		})
		return
	}

	viewURL := directURL
	if !h.storage.HasDirectPublicURL() {
		scheme := "http"
		if r.TLS != nil || strings.EqualFold(r.Header.Get("X-Forwarded-Proto"), "https") {
			scheme = "https"
		}
		viewURL = fmt.Sprintf("%s://%s/api/v1/media/view?key=%s", scheme, r.Host, url.QueryEscape(objectKey))
	}

	sendJSON(w, http.StatusOK, models.ApiResponse{
		Success: true,
		Data: map[string]interface{}{
			"url":          viewURL,
			"gcs_url":      directURL,
			"object_key":   objectKey,
			"folder":       structuredFolder,
			"filename":     header.Filename,
			"content_type": contentType,
			"size_bytes":   header.Size,
			"provider":     h.storage.ProviderName(),
			"status":       "stored_in_" + h.storage.ProviderName(),
		},
	})
}

// ViewFile streams an object from Object Storage with 30-day immutable HTTP caching + ETag 304 support.
func (h *MediaHandler) ViewFile(w http.ResponseWriter, r *http.Request) {
	objectKey := strings.TrimSpace(r.URL.Query().Get("key"))
	if objectKey == "" {
		http.Error(w, "Missing 'key' query parameter", http.StatusBadRequest)
		return
	}

	rc, contentType, etag, err := h.storage.OpenStream(r.Context(), objectKey)
	if err != nil {
		http.Error(w, "File not found", http.StatusNotFound)
		return
	}
	defer rc.Close()

	w.Header().Set("Content-Type", contentType)
	w.Header().Set("Cache-Control", "public, max-age=2592000, immutable")
	if etag != "" {
		w.Header().Set("ETag", etag)
		if match := strings.TrimSpace(r.Header.Get("If-None-Match")); match != "" && match == etag {
			w.WriteHeader(http.StatusNotModified)
			return
		}
	}
	w.WriteHeader(http.StatusOK)
	_, _ = io.Copy(w, rc)
}
