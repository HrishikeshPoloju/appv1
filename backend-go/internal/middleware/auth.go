package middleware

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/schoolapp/backend-go/internal/models"
)

type contextKey string

const (
	UserContextKey contextKey = "currentUser"
)

type JWTClaims struct {
	ID         string          `json:"id"`
	SchoolID   string          `json:"school_id"`
	Role       models.UserRole `json:"role"`
	FullName   string          `json:"full_name"`
	SchoolName string          `json:"school_name,omitempty"`
	jwt.RegisteredClaims
}

type AuthMiddleware struct {
	jwtSecret []byte
}

func NewAuthMiddleware(secret string) *AuthMiddleware {
	return &AuthMiddleware{jwtSecret: []byte(secret)}
}

func (m *AuthMiddleware) GenerateToken(user *models.User) (string, error) {
	claims := JWTClaims{
		ID:         user.ID,
		SchoolID:   user.SchoolID,
		Role:       user.Role,
		FullName:   user.FullName,
		SchoolName: user.SchoolName,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(30 * 24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(m.jwtSecret)
}

func (m *AuthMiddleware) AuthenticateJWT(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			http.Error(w, `{"success":false,"error":"Authorization header required"}`, http.StatusUnauthorized)
			return
		}

		tokenStr := strings.TrimPrefix(authHeader, "Bearer ")
		tokenStr = strings.TrimSpace(tokenStr)

		claims := &JWTClaims{}
		token, err := jwt.ParseWithClaims(tokenStr, claims, func(token *jwt.Token) (interface{}, error) {
			if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
				return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
			}
			return m.jwtSecret, nil
		})

		if err != nil || !token.Valid {
			http.Error(w, `{"success":false,"error":"Invalid or expired token"}`, http.StatusUnauthorized)
			return
		}

		if meta := models.GetLogMeta(r.Context()); meta != nil {
			meta.ActorName = claims.FullName
			meta.ActorRole = string(claims.Role)
			if claims.Role == models.RoleTeacher || claims.Role == models.RoleAdmin {
				meta.TeacherName = claims.FullName
			} else {
				meta.StudentName = claims.FullName
			}
		}

		ctx := context.WithValue(r.Context(), UserContextKey, claims)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AuthenticateOrStudent matches the Android app client contract
func (m *AuthMiddleware) AuthenticateOrStudent(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader != "" {
			tokenStr := strings.TrimPrefix(authHeader, "Bearer ")
			tokenStr = strings.TrimSpace(tokenStr)

			claims := &JWTClaims{}
			token, err := jwt.ParseWithClaims(tokenStr, claims, func(token *jwt.Token) (interface{}, error) {
				return m.jwtSecret, nil
			})
			if err == nil && token.Valid {
				ctx := context.WithValue(r.Context(), UserContextKey, claims)
				next.ServeHTTP(w, r.WithContext(ctx))
				return
			}
		}

		meta := models.GetLogMeta(r.Context())
		studentName := r.Header.Get("x-student-name")
		if studentName == "" {
			studentName = r.URL.Query().Get("student_name")
		}
		if studentName == "" && meta != nil && meta.StudentName != "" {
			studentName = meta.StudentName
		}
		if studentName == "" {
			studentName = r.Header.Get("x-actor-name")
		}
		if studentName == "" {
			studentName = "Guest User"
		}

		roleHeader := strings.ToUpper(r.Header.Get("x-user-role"))
		if roleHeader == "" {
			roleHeader = strings.ToUpper(r.Header.Get("x-role"))
		}
		userRole := models.RoleStudent
		if roleHeader == "TEACHER" {
			userRole = models.RoleTeacher
		} else if roleHeader == "ADMIN" {
			userRole = models.RoleAdmin
		} else if roleHeader == "PARENT" {
			userRole = models.RoleParent
		}

		schoolID := strings.TrimSpace(r.Header.Get("x-school-id"))
		if schoolID == "" {
			schoolID = strings.TrimSpace(r.URL.Query().Get("school_id"))
		}
		if schoolID == "" {
			schoolID = "00000000-0000-0000-0000-000000000001"
		}

		fallbackClaims := &JWTClaims{
			ID:       "00000000-0000-0000-0000-000000000001",
			SchoolID: schoolID,
			Role:     userRole,
			FullName: studentName,
		}

		ctx := context.WithValue(r.Context(), UserContextKey, fallbackClaims)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func GetUserFromContext(ctx context.Context) *JWTClaims {
	if val := ctx.Value(UserContextKey); val != nil {
		if claims, ok := val.(*JWTClaims); ok {
			return claims
		}
	}
	return nil
}

// RequestLogger captures request body, response body, student_name, teacher_name, role, and class_id for grep-friendly audit logging
func RequestLogger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		studentName := strings.TrimSpace(r.Header.Get("x-student-name"))
		if studentName == "" {
			studentName = strings.TrimSpace(r.Header.Get("x-actor-name"))
		}
		if studentName == "" {
			studentName = strings.TrimSpace(r.URL.Query().Get("student_name"))
		}
		teacherName := strings.TrimSpace(r.Header.Get("x-teacher-name"))
		roleHeader := strings.ToUpper(strings.TrimSpace(r.Header.Get("x-user-role")))
		if roleHeader == "" {
			roleHeader = strings.ToUpper(strings.TrimSpace(r.Header.Get("x-role")))
		}
		if roleHeader == "" {
			roleHeader = "STUDENT"
		}
		if (roleHeader == "TEACHER" || roleHeader == "ADMIN") && teacherName == "" {
			teacherName = strings.TrimSpace(r.Header.Get("x-actor-name"))
		}
		classID := strings.TrimSpace(r.Header.Get("x-class-id"))
		if classID == "" {
			classID = strings.TrimSpace(r.URL.Query().Get("class_id"))
		}

		// Extract classId or studentId from URL path segments if present
		pathParts := strings.Split(strings.Trim(r.URL.Path, "/"), "/")
		for i := 0; i < len(pathParts)-1; i++ {
			if pathParts[i] == "classes" && classID == "" {
				classID = pathParts[i+1]
			}
			if pathParts[i] == "students" && studentName == "" {
				studentName = pathParts[i+1]
			}
		}

		// Capture Request Body safely (skip raw binary stream if multipart upload)
		var reqBodyParsed interface{}
		contentType := r.Header.Get("Content-Type")
		if strings.HasPrefix(contentType, "multipart/form-data") {
			reqBodyParsed = map[string]string{"multipart": contentType}
		} else if r.Body != nil {
			reqBytes, err := io.ReadAll(r.Body)
			if err == nil {
				r.Body = io.NopCloser(bytes.NewBuffer(reqBytes))
				trimmed := bytes.TrimSpace(reqBytes)
				if len(trimmed) > 0 {
					var jsonMap map[string]interface{}
					if jsonErr := json.Unmarshal(trimmed, &jsonMap); jsonErr == nil {
						reqBodyParsed = jsonMap
						if v, ok := jsonMap["full_name"].(string); ok && v != "" && studentName == "" {
							studentName = v
						}
						if v, ok := jsonMap["student_name"].(string); ok && v != "" && studentName == "" {
							studentName = v
						}
						if v, ok := jsonMap["author_name"].(string); ok && v != "" {
							if roleHeader == "TEACHER" || roleHeader == "ADMIN" {
								teacherName = v
							} else if studentName == "" {
								studentName = v
							}
						}
						if v, ok := jsonMap["teacher_name"].(string); ok && v != "" && teacherName == "" {
							teacherName = v
						}
						if v, ok := jsonMap["class_id"].(string); ok && v != "" && classID == "" {
							classID = v
						}
						if cls, ok1 := jsonMap["class_name"].(string); ok1 && cls != "" && classID == "" {
							sec, _ := jsonMap["section"].(string)
							classID = fmt.Sprintf("%s-%s", cls, sec)
						}
					} else {
						reqBodyParsed = string(trimmed)
					}
				}
			}
		}

		if studentName == "" {
			studentName = "Unknown"
		}
		if classID == "" {
			classID = "N/A"
		}
		actorName := studentName
		if (roleHeader == "TEACHER" || roleHeader == "ADMIN") && teacherName != "" {
			actorName = teacherName
		} else if roleHeader == "TEACHER" && teacherName == "" {
			teacherName = "Class Teacher (" + classID + ")"
			actorName = teacherName
		}

		actorID := strings.TrimSpace(r.Header.Get("x-student-id"))
		for i := 0; i < len(pathParts)-1; i++ {
			if pathParts[i] == "students" && actorID == "" {
				actorID = pathParts[i+1]
			}
		}

		meta := &models.RequestLogMeta{
			ActorName:   actorName,
			ActorRole:   roleHeader,
			ActorID:     actorID,
			StudentName: studentName,
			TeacherName: teacherName,
			ClassID:     classID,
			Method:      r.Method,
			Path:        r.URL.Path,
		}

		ctx := context.WithValue(r.Context(), models.RequestMetaContextKey, meta)
		rw := &responseWriterInterceptor{
			ResponseWriter: w,
			statusCode:     http.StatusOK,
			bodyBuf:        &bytes.Buffer{},
		}

		next.ServeHTTP(rw, r.WithContext(ctx))

		reqStr := "-"
		if reqBodyParsed != nil {
			if b, err := json.Marshal(reqBodyParsed); err == nil {
				reqStr = string(b)
			}
		}

		respCT := rw.Header().Get("Content-Type")
		var respStr string
		if strings.HasPrefix(respCT, "image/") || strings.HasPrefix(respCT, "application/pdf") || strings.HasPrefix(respCT, "application/octet-stream") || strings.HasPrefix(r.URL.Path, "/api/v1/media/view") {
			respStr = fmt.Sprintf("[Binary Media Stream: %s, %d bytes]", respCT, rw.bodyBuf.Len())
		} else {
			respStr = strings.TrimSpace(rw.bodyBuf.String())
			if respStr == "" {
				respStr = "{}"
			}
		}

		fullPath := r.URL.Path
		if r.URL.RawQuery != "" {
			fullPath = fullPath + "?" + r.URL.RawQuery
		}

		dbOps, gcsOps := meta.SnapshotOps()
		var sb strings.Builder
		sb.WriteString("================================================================================\n")
		idTag := meta.ActorID
		if idTag == "" {
			idTag = "N/A"
		}
		sb.WriteString(fmt.Sprintf("[%s] [%s] %s (ID: %s) | Class: %s\n",
			time.Now().Format("2006-01-02 15:04:05"),
			meta.ActorRole,
			meta.ActorName,
			idTag,
			meta.ClassID,
		))
		sb.WriteString(fmt.Sprintf("API      : %s %s  -->  %d (%dms)\n", r.Method, fullPath, rw.statusCode, time.Since(start).Milliseconds()))
		if reqStr != "-" {
			sb.WriteString(fmt.Sprintf("REQUEST  : %s\n", reqStr))
		}
		if len(dbOps) > 0 {
			sb.WriteString("DB OPS   :\n")
			for _, op := range dbOps {
				sb.WriteString("  • " + op + "\n")
			}
		}
		if len(gcsOps) > 0 {
			sb.WriteString("GCS OPS  :\n")
			for _, op := range gcsOps {
				sb.WriteString("  • " + op + "\n")
			}
		}
		sb.WriteString(fmt.Sprintf("RESPONSE : %s\n", respStr))
		sb.WriteString("================================================================================\n")
		fmt.Print(sb.String())
	})
}

type responseWriterInterceptor struct {
	http.ResponseWriter
	statusCode int
	bodyBuf    *bytes.Buffer
}

func (rw *responseWriterInterceptor) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

func (rw *responseWriterInterceptor) Write(b []byte) (int, error) {
	if rw.bodyBuf != nil && rw.bodyBuf.Len() < 65536 {
		rw.bodyBuf.Write(b)
	}
	return rw.ResponseWriter.Write(b)
}
