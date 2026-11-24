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

	return &Config{
		Port:                   port,
		DatabaseURL:            dbURL,
		CloudSQLConnectionName: connName,
		GCPProjectID:           projectID,
		GCSBucketName:          bucketName,
		GoogleAppCredentials:   credsPath,
		JWTSecret:              jwtSecret,
		Environment:            env,
		AllowedOrigins:         origins,
	}
}
