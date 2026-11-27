package config

import (
	"os"
	"strings"
)

type Config struct {
	Port                   string
	DatabaseURL            string
	CloudSQLConnectionName string
	GCPProjectID           string
	GCSBucketName          string
	GoogleAppCredentials   string
	StorageProvider        string // "gcs" (default) or "r2" (Cloudflare R2 zero-egress)
	R2AccountID            string
	R2AccessKeyID          string
	R2SecretAccessKey      string
	R2BucketName           string
	R2PublicBaseURL        string
	JWTSecret              string
	Environment            string
	AllowedOrigins         []string
}

func Load() *Config {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		// Default fallback for Cloud SQL or local dev
		dbURL = "postgres://postgres:hrishi%402005@localhost:5432/postgres?sslmode=disable"
	}

	connName := os.Getenv("CLOUD_SQL_CONNECTION_NAME")
	if connName == "" {
		connName = "project-00a301af-7c2e-4862-b61:asia-south1:genesis-school-db"
	}

	projectID := os.Getenv("GCP_PROJECT_ID")
	if projectID == "" {
		projectID = "project-00a301af-7c2e-4862-b61"
	}

	bucketName := os.Getenv("GCS_BUCKET_NAME")
	if bucketName == "" {
		bucketName = "genesis-school-storage"
	}

	credsPath := os.Getenv("GOOGLE_APPLICATION_CREDENTIALS")
	if credsPath == "" {
		// Look for project JSON key in workspace root if not explicitly set
		if _, err := os.Stat("project-00a301af-7c2e-4862-b61-477b6bc08ec5.json"); err == nil {
			credsPath = "project-00a301af-7c2e-4862-b61-477b6bc08ec5.json"
		} else if _, err := os.Stat("../project-00a301af-7c2e-4862-b61-477b6bc08ec5.json"); err == nil {
			credsPath = "../project-00a301af-7c2e-4862-b61-477b6bc08ec5.json"
		}
	}

	jwtSecret := os.Getenv("JWT_SECRET")
	if jwtSecret == "" {
		jwtSecret = "genesis-school-super-secret-key-2026"
	}

	env := os.Getenv("ENVIRONMENT")
	if env == "" {
		env = "development"
	}

	origins := []string{"*"}
	if corsEnv := os.Getenv("CORS_ORIGIN"); corsEnv != "" {
		origins = strings.Split(corsEnv, ",")
	}

	storageProvider := strings.ToLower(strings.TrimSpace(os.Getenv("STORAGE_PROVIDER")))
	if storageProvider == "" {
		storageProvider = "gcs"
	}

	return &Config{
		Port:                   port,
		DatabaseURL:            dbURL,
		CloudSQLConnectionName: connName,
		GCPProjectID:           projectID,
		GCSBucketName:          bucketName,
		GoogleAppCredentials:   credsPath,
		StorageProvider:        storageProvider,
		R2AccountID:            strings.TrimSpace(os.Getenv("R2_ACCOUNT_ID")),
		R2AccessKeyID:          strings.TrimSpace(os.Getenv("R2_ACCESS_KEY_ID")),
		R2SecretAccessKey:      strings.TrimSpace(os.Getenv("R2_SECRET_ACCESS_KEY")),
		R2BucketName:           strings.TrimSpace(os.Getenv("R2_BUCKET_NAME")),
		R2PublicBaseURL:        strings.TrimRight(strings.TrimSpace(os.Getenv("R2_PUBLIC_BASE_URL")), "/"),
		JWTSecret:              jwtSecret,
		Environment:            env,
		AllowedOrigins:         origins,
	}
}
