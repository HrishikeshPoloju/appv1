package handlers

import (
	"context"
	"fmt"
	"net/http"
	"regexp"
	"strings"

	"github.com/schoolapp/backend-go/internal/database"
	"github.com/schoolapp/backend-go/internal/middleware"
)

var uuidRegex = regexp.MustCompile(`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`)

func isValidUUID(s string) bool {
	return uuidRegex.MatchString(strings.TrimSpace(s))
}

// resolveOrCreateActorUUID guarantees a real users.id UUID exists in PostgreSQL
// so foreign-key inserts (discussions, replies, posts, notes, homework, referrals)
// never fail with SQLSTATE 23503 even on a freshly wiped database.
func resolveOrCreateActorUUID(
	ctx context.Context,
	db *database.DB,
	r *http.Request,
	schoolID string,
	actorName string,
	roleHint string,
) (string, string, error) {
	user := middleware.GetUserFromContext(r.Context())

	role := strings.ToUpper(strings.TrimSpace(roleHint))
	if role == "" {
		role = strings.ToUpper(strings.TrimSpace(r.Header.Get("x-user-role")))
	}
	if role == "" && user != nil && string(user.Role) != "" {
		role = string(user.Role)
	}
	if role != "ADMIN" && role != "TEACHER" && role != "STUDENT" && role != "PARENT" {
		role = "STUDENT"
	}

	name := strings.TrimSpace(actorName)
	if name == "" {
		name = strings.TrimSpace(r.Header.Get("x-student-name"))
	}
	if name == "" {
		name = strings.TrimSpace(r.Header.Get("x-teacher-name"))
	}
	if name == "" && user != nil && strings.TrimSpace(user.FullName) != "" && user.FullName != "Guest User" {
		name = strings.TrimSpace(user.FullName)
	}
	if name == "" {
		switch role {
		case "ADMIN":
			name = "School Principal"
		case "TEACHER":
			name = "Class Teacher"
		default:
			name = "Student"
		}
	}

	// 1. Check candidate UUIDs from header or JWT
	candidates := []string{
		strings.TrimSpace(r.Header.Get("x-student-id")),
		strings.TrimSpace(r.Header.Get("x-user-id")),
	}
	if user != nil {
		candidates = append(candidates, strings.TrimSpace(user.ID))
	}
	for _, cid := range candidates {
		if isValidUUID(cid) && cid != "00000000-0000-0000-0000-000000000001" {
			var existingID, existingName string
			err := db.Pool.QueryRow(ctx, `SELECT id::text, full_name FROM users WHERE id = $1::uuid AND school_id = $2::uuid`, cid, schoolID).Scan(&existingID, &existingName)
			if err == nil && existingID != "" {
				if name == "" {
					name = existingName
				}
				return existingID, name, nil
			}
		}
	}

	// 2. Lookup by school_id + full_name (case-insensitive)
	var foundID string
	err := db.Pool.QueryRow(ctx, `
		SELECT id::text
		FROM users
		WHERE school_id = $1 AND LOWER(full_name) = LOWER($2)
		ORDER BY created_at DESC
		LIMIT 1
	`, schoolID, name).Scan(&foundID)
	if err == nil && foundID != "" {
		return foundID, name, nil
	}

	// 3. Auto-provision a real user row for this actor (e.g. Teacher/Admin switching role on fresh DB)
	slug := strings.ToLower(regexp.MustCompile(`[^a-zA-Z0-9]+`).ReplaceAllString(name, "."))
	slug = strings.Trim(slug, ".")
	if slug == "" {
		slug = strings.ToLower(role)
	}
	email := fmt.Sprintf("%s.%s@genesis.school", slug, strings.ToLower(role))
	phoneKey := fmt.Sprintf("%s-%s", strings.ToLower(role), slug)
	if len(phoneKey) > 30 {
		phoneKey = phoneKey[:30]
	}

	var createdID string
	err = db.Pool.QueryRow(ctx, `
		INSERT INTO users (school_id, role, full_name, phone, email, password_hash, verification_status)
		VALUES ($1, $2, $3, $4, $5, '', 'VERIFIED')
		ON CONFLICT (school_id, phone) DO UPDATE SET full_name = EXCLUDED.full_name
		RETURNING id::text
	`, schoolID, role, name, phoneKey, email).Scan(&createdID)
	if err != nil {
		return "", name, fmt.Errorf("failed to resolve or create actor user: %w", err)
	}
	return createdID, name, nil
}
