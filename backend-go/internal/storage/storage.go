package storage

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
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

// ObjectStore abstracts media storage so the backend can run on Google Cloud Storage
// (default) or switch to Cloudflare R2 (zero-egress) via STORAGE_PROVIDER=r2 with
// zero Android app changes.
type ObjectStore interface {
	ProviderName() string
	HasDirectPublicURL() bool
	UploadStream(ctx context.Context, folder string, filename string, r io.Reader, contentType string) (objectKey string, publicURL string, err error)
	OpenStream(ctx context.Context, objectKey string) (rc io.ReadCloser, contentType string, etag string, err error)
}

// StoreConfig holds configuration for selecting GCS or Cloudflare R2.
type StoreConfig struct {
	Provider          string
	GCPProjectID      string
	GCSBucketName     string
	GoogleCredentials string
	R2AccountID       string
	R2AccessKeyID     string
	R2SecretAccessKey string
	R2BucketName      string
	R2PublicBaseURL   string
}

// NewObjectStore initializes either Cloudflare R2 (when STORAGE_PROVIDER=r2) or Google Cloud Storage (default).
func NewObjectStore(ctx context.Context, cfg StoreConfig) (ObjectStore, error) {
	if strings.EqualFold(cfg.Provider, "r2") {
		if cfg.R2AccountID == "" || cfg.R2AccessKeyID == "" || cfg.R2SecretAccessKey == "" || cfg.R2BucketName == "" {
			slog.Warn("STORAGE_PROVIDER=r2 selected but R2 credentials incomplete; falling back to GCS")
		} else {
			slog.Info("initializing Cloudflare R2 Object Storage (zero-egress mode)",
				"bucket", cfg.R2BucketName,
				"public_base_url", cfg.R2PublicBaseURL,
			)
			return &R2StorageService{
				accountID:     cfg.R2AccountID,
				accessKeyID:   cfg.R2AccessKeyID,
				secretKey:     cfg.R2SecretAccessKey,
				bucketName:    cfg.R2BucketName,
				publicBaseURL: strings.TrimRight(cfg.R2PublicBaseURL, "/"),
				httpClient:    &http.Client{Timeout: 30 * time.Second},
			}, nil
		}
	}
	return NewStorageService(ctx, cfg.GCPProjectID, cfg.GCSBucketName, cfg.GoogleCredentials)
}

// ============================================================================
// Google Cloud Storage Implementation (Default)
// ============================================================================

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

	return &StorageService{
		client:     client,
		bucketName: bucketName,
		projectID:  projectID,
	}, nil
}

func (s *StorageService) ProviderName() string    { return "gcs" }
func (s *StorageService) HasDirectPublicURL() bool { return false }

// BuildClassSectionFolder builds a clean, isolated folder path per school, class, section, and category.
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

func buildUniqueObjectKey(folder, filename string) (string, string) {
	cleanFolder := strings.Trim(folder, "/")
	cleanName := filepath.Base(filename)
	cleanName = strings.ReplaceAll(cleanName, " ", "_")
	return fmt.Sprintf("%s/%d_%s", cleanFolder, time.Now().UnixNano(), cleanName), cleanName
}

// UploadStream uploads an io.Reader stream to Google Cloud Storage with 30-day immutable Cache-Control.
func (s *StorageService) UploadStream(ctx context.Context, folder string, filename string, r io.Reader, contentType string) (string, string, error) {
	start := time.Now()
	meta := models.GetLogMeta(ctx)

	if contentType == "" {
		contentType = "application/octet-stream"
	}

	objectKey, cleanName := buildUniqueObjectKey(folder, filename)

	bkt := s.client.Bucket(s.bucketName)
	obj := bkt.Object(objectKey)

	writer := obj.NewWriter(ctx)
	writer.ContentType = contentType
	writer.CacheControl = "public, max-age=2592000, immutable"

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
	meta.AddGCSOp(fmt.Sprintf("Uploaded '%s' (%d bytes, %s) to GCS bucket '%s' at '%s' (%dms)", cleanName, bytesWritten, contentType, s.bucketName, objectKey, time.Since(start).Milliseconds()))
	return objectKey, gcsURL, nil
}

// OpenStream streams an object from private Google Cloud Storage using the service account.
func (s *StorageService) OpenStream(ctx context.Context, objectKey string) (io.ReadCloser, string, string, error) {
	cleanKey := strings.TrimLeft(strings.TrimSpace(objectKey), "/")
	if cleanKey == "" || strings.Contains(cleanKey, "..") {
		return nil, "", "", fmt.Errorf("invalid object key")
	}
	reader, err := s.client.Bucket(s.bucketName).Object(cleanKey).NewReader(ctx)
	if err != nil {
		return nil, "", "", err
	}
	ct := reader.Attrs.ContentType
	if ct == "" {
		ct = "application/octet-stream"
	}
	etag := fmt.Sprintf(`"%x-%d"`, sha256.Sum256([]byte(cleanKey)), reader.Attrs.Size)
	return reader, ct, etag, nil
}

// ============================================================================
// Cloudflare R2 Implementation (Zero-Egress S3-Compatible via Pure Go SigV4)
// ============================================================================

type R2StorageService struct {
	accountID     string
	accessKeyID   string
	secretKey     string
	bucketName    string
	publicBaseURL string
	httpClient    *http.Client
}

func (r2 *R2StorageService) ProviderName() string { return "r2" }
func (r2 *R2StorageService) HasDirectPublicURL() bool {
	return r2.publicBaseURL != ""
}

func (r2 *R2StorageService) endpointHost() string {
	return fmt.Sprintf("%s.r2.cloudflarestorage.com", r2.accountID)
}

func (r2 *R2StorageService) UploadStream(ctx context.Context, folder string, filename string, reader io.Reader, contentType string) (string, string, error) {
	start := time.Now()
	meta := models.GetLogMeta(ctx)

	if contentType == "" {
		contentType = "application/octet-stream"
	}
	bodyBytes, err := io.ReadAll(reader)
	if err != nil {
		return "", "", fmt.Errorf("failed reading upload payload: %w", err)
	}

	objectKey, cleanName := buildUniqueObjectKey(folder, filename)
	req, err := r2.signedRequest(ctx, http.MethodPut, objectKey, bodyBytes, contentType, "public, max-age=2592000, immutable")
	if err != nil {
		return "", "", err
	}
	resp, err := r2.httpClient.Do(req)
	if err != nil {
		meta.AddGCSOp(fmt.Sprintf("R2 upload FAILED for '%s': %v", cleanName, err))
		return "", "", fmt.Errorf("R2 PUT failed: %w", err)
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		errBody, _ := io.ReadAll(resp.Body)
		return "", "", fmt.Errorf("R2 PUT returned HTTP %d: %s", resp.StatusCode, string(errBody))
	}

	publicURL := ""
	if r2.publicBaseURL != "" {
		publicURL = fmt.Sprintf("%s/%s", r2.publicBaseURL, objectKey)
	} else {
		publicURL = fmt.Sprintf("https://%s/%s/%s", r2.endpointHost(), r2.bucketName, objectKey)
	}
	meta.AddGCSOp(fmt.Sprintf("Uploaded '%s' (%d bytes, %s) to Cloudflare R2 bucket '%s' at '%s' (%dms)", cleanName, len(bodyBytes), contentType, r2.bucketName, objectKey, time.Since(start).Milliseconds()))
	return objectKey, publicURL, nil
}

func (r2 *R2StorageService) OpenStream(ctx context.Context, objectKey string) (io.ReadCloser, string, string, error) {
	cleanKey := strings.TrimLeft(strings.TrimSpace(objectKey), "/")
	if cleanKey == "" || strings.Contains(cleanKey, "..") {
		return nil, "", "", fmt.Errorf("invalid object key")
	}
	req, err := r2.signedRequest(ctx, http.MethodGet, cleanKey, nil, "", "")
	if err != nil {
		return nil, "", "", err
	}
	resp, err := r2.httpClient.Do(req)
	if err != nil {
		return nil, "", "", err
	}
	if resp.StatusCode != http.StatusOK {
		resp.Body.Close()
		return nil, "", "", fmt.Errorf("R2 GET returned HTTP %d", resp.StatusCode)
	}
	ct := resp.Header.Get("Content-Type")
	if ct == "" {
		ct = "application/octet-stream"
	}
	etag := resp.Header.Get("ETag")
	if etag == "" {
		etag = fmt.Sprintf(`"%x"`, sha256.Sum256([]byte(cleanKey)))
	}
	return resp.Body, ct, etag, nil
}

func (r2 *R2StorageService) signedRequest(ctx context.Context, method, objectKey string, payload []byte, contentType, cacheControl string) (*http.Request, error) {
	now := time.Now().UTC()
	amzDate := now.Format("20060102T150405Z")
	dateStamp := now.Format("20060102")
	region := "auto"
	service := "s3"

	payloadHashBytes := sha256.Sum256(payload)
	payloadHash := hex.EncodeToString(payloadHashBytes[:])

	host := r2.endpointHost()
	canonicalURI := "/" + r2.bucketName + "/" + strings.TrimLeft(objectKey, "/")
	endpointURL := "https://" + host + canonicalURI

	var body io.Reader
	if payload != nil {
		body = bytes.NewReader(payload)
	}
	req, err := http.NewRequestWithContext(ctx, method, endpointURL, body)
	if err != nil {
		return nil, err
	}
	req.Host = host
	req.Header.Set("x-amz-date", amzDate)
	req.Header.Set("x-amz-content-sha256", payloadHash)
	if contentType != "" {
		req.Header.Set("Content-Type", contentType)
	}
	if cacheControl != "" {
		req.Header.Set("Cache-Control", cacheControl)
	}

	canonicalHeaders := fmt.Sprintf("host:%s\nx-amz-content-sha256:%s\nx-amz-date:%s\n", host, payloadHash, amzDate)
	signedHeaders := "host;x-amz-content-sha256;x-amz-date"
	canonicalRequest := strings.Join([]string{method, canonicalURI, "", canonicalHeaders, signedHeaders, payloadHash}, "\n")

	crHash := sha256.Sum256([]byte(canonicalRequest))
	credentialScope := fmt.Sprintf("%s/%s/%s/aws4_request", dateStamp, region, service)
	stringToSign := strings.Join([]string{"AWS4-HMAC-SHA256", amzDate, credentialScope, hex.EncodeToString(crHash[:])}, "\n")

	kDate := hmacSHA256([]byte("AWS4"+r2.secretKey), dateStamp)
	kRegion := hmacSHA256(kDate, region)
	kService := hmacSHA256(kRegion, service)
	kSigning := hmacSHA256(kService, "aws4_request")
	signature := hex.EncodeToString(hmacSHA256(kSigning, stringToSign))

	authHeader := fmt.Sprintf("AWS4-HMAC-SHA256 Credential=%s/%s, SignedHeaders=%s, Signature=%s",
		r2.accessKeyID, credentialScope, signedHeaders, signature)
	req.Header.Set("Authorization", authHeader)
	return req, nil
}

func hmacSHA256(key []byte, data string) []byte {
	mac := hmac.New(sha256.New, key)
	mac.Write([]byte(data))
	return mac.Sum(nil)
}

// DetectContentType detects the MIME type from header bytes.
func DetectContentType(buffer []byte) string {
	return http.DetectContentType(buffer)
}
