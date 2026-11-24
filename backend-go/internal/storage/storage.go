package storage

import (
	"context"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"cloud.google.com/go/storage"
	"github.com/schoolapp/backend-go/internal/models"
	"google.golang.org/api/option"
)

type StorageService struct {
	client     *storage.Client
	bucketName string
	projectID  string
}

func NewStorageService(ctx context.Context, projectID string, bucketName string, credsPath string) (*StorageService, error) {
	var opts []option.ClientOption

	if credsPath != "" {
		if _, err := os.Stat(credsPath); err == nil {
			opts = append(opts, option.WithCredentialsFile(credsPath))
			slog.Info("initializing Google Cloud Storage with service account credentials",
				"path", credsPath,
				"project_id", projectID,
				"bucket", bucketName,
			)
		} else {
			slog.Warn("credentials file specified but not found on disk, falling back to default ADC", "path", credsPath)
		}
	}

	client, err := storage.NewClient(ctx, opts...)
	if err != nil {
		return nil, fmt.Errorf("failed to create GCS client: %w", err)
	}

	svc := &StorageService{
		client:     client,
		bucketName: bucketName,
		projectID:  projectID,
	}

	return svc, nil
}

// BuildClassSectionFolder builds a clean, isolated GCS folder path per school, class, section, and category.
// Example: ("genesis-high-school", "class-8a", "doubts") -> "schools/genesis-high-school/class-8/section-a/doubts"
func BuildClassSectionFolder(schoolSlug, classID, category string) string {
	school := strings.Trim(strings.ToLower(strings.TrimSpace(schoolSlug)), "/")
	if school == "" {
		school = "genesis-high-school"
	}
	cat := strings.Trim(strings.ToLower(strings.TrimSpace(category)), "/")
	if cat == "" {
		cat = "uploads"
	}
	rawClass := strings.ToLower(strings.TrimSpace(classID))
	if rawClass == "" || rawClass == "all" || rawClass == "n/a" {
		return fmt.Sprintf("schools/%s/general/%s", school, cat)
	}
	// Parse canonical class ID like "class-8a", "class-10c", "class-10rose", "class-ukga"
	trimmed := strings.TrimPrefix(rawClass, "class-")
	gradePart := ""
	secPart := ""
	for i, ch := range trimmed {
		if ch >= '0' && ch <= '9' {
			gradePart += string(ch)
		} else {
			secPart = trimmed[i:]
			break
		}
	}
	if gradePart != "" && secPart != "" {
		return fmt.Sprintf("schools/%s/class-%s/section-%s/%s", school, gradePart, secPart, cat)
	}
	return fmt.Sprintf("schools/%s/%s/%s", school, rawClass, cat)
}

// UploadStream uploads an io.Reader stream to Google Cloud Storage with full audit logging.
// Returns (objectKey, gcsURL, error).
func (s *StorageService) UploadStream(ctx context.Context, folder string, filename string, r io.Reader, contentType string) (string, string, error) {
	start := time.Now()
	meta := models.GetLogMeta(ctx)

	if contentType == "" {
		contentType = "application/octet-stream"
	}

	cleanFolder := strings.Trim(folder, "/")
	cleanName := filepath.Base(filename)
	cleanName = strings.ReplaceAll(cleanName, " ", "_")
	objectKey := fmt.Sprintf("%s/%d_%s", cleanFolder, time.Now().UnixNano(), cleanName)

	bkt := s.client.Bucket(s.bucketName)
	obj := bkt.Object(objectKey)

	writer := obj.NewWriter(ctx)
	writer.ContentType = contentType
	writer.CacheControl = "public, max-age=86400"

	bytesWritten, err := io.Copy(writer, r)
	if err != nil {
		_ = writer.Close()
		meta.AddGCSOp(fmt.Sprintf("Upload FAILED for '%s' to bucket '%s': %v", cleanName, s.bucketName, err))
		return "", "", fmt.Errorf("failed to write stream to GCS: %w", err)
	}

	if err := writer.Close(); err != nil {
		meta.AddGCSOp(fmt.Sprintf("Upload close FAILED for '%s' in bucket '%s': %v", cleanName, s.bucketName, err))
		return "", "", fmt.Errorf("failed to close GCS object writer: %w", err)
	}

	gcsURL := fmt.Sprintf("https://storage.googleapis.com/%s/%s", s.bucketName, objectKey)
	meta.AddGCSOp(fmt.Sprintf("Uploaded '%s' (%d bytes, %s) to bucket '%s' at '%s' (%dms)", cleanName, bytesWritten, contentType, s.bucketName, objectKey, time.Since(start).Milliseconds()))
	return objectKey, gcsURL, nil
}

// OpenReader streams an object directly from private Google Cloud Storage using the service account.
func (s *StorageService) OpenReader(ctx context.Context, objectKey string) (*storage.Reader, error) {
	cleanKey := strings.TrimLeft(strings.TrimSpace(objectKey), "/")
	if cleanKey == "" || strings.Contains(cleanKey, "..") {
		return nil, fmt.Errorf("invalid object key")
	}
	return s.client.Bucket(s.bucketName).Object(cleanKey).NewReader(ctx)
}

// DetectContentType detects the MIME type from header bytes
func DetectContentType(buffer []byte) string {
	return http.DetectContentType(buffer)
}
