package models

import (
	"context"
	"sync"
	"time"
)

type logMetaContextKey string

const RequestMetaContextKey logMetaContextKey = "requestLogMeta"

type RequestLogMeta struct {
	mu          sync.Mutex
	ActorName   string   `json:"actor_name"`
	ActorRole   string   `json:"actor_role"`
	ActorID     string   `json:"actor_id"`
	StudentName string   `json:"student_name"`
	TeacherName string   `json:"teacher_name,omitempty"`
	ClassID     string   `json:"class_id"`
	Method      string   `json:"method"`
	Path        string   `json:"path"`
	DBOps       []string `json:"db_ops,omitempty"`
	GCSOps      []string `json:"gcs_ops,omitempty"`
}

func (m *RequestLogMeta) AddDBOp(op string) {
	if m == nil || op == "" {
		return
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.DBOps = append(m.DBOps, op)
}

func (m *RequestLogMeta) AddGCSOp(op string) {
	if m == nil || op == "" {
		return
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	m.GCSOps = append(m.GCSOps, op)
}

func (m *RequestLogMeta) SnapshotOps() ([]string, []string) {
	if m == nil {
		return nil, nil
	}
	m.mu.Lock()
	defer m.mu.Unlock()
	dbCopy := append([]string(nil), m.DBOps...)
	gcsCopy := append([]string(nil), m.GCSOps...)
	return dbCopy, gcsCopy
}

func GetLogMeta(ctx context.Context) *RequestLogMeta {
	if ctx == nil {
		return nil
	}
	if val := ctx.Value(RequestMetaContextKey); val != nil {
		if meta, ok := val.(*RequestLogMeta); ok {
			return meta
		}
	}
	return nil
}

type UserRole string

const (
	RoleAdmin   UserRole = "ADMIN"
	RoleTeacher UserRole = "TEACHER"
	RoleParent  UserRole = "PARENT"
	RoleStudent UserRole = "STUDENT"
)

type User struct {
	ID                 string    `json:"id"`
	SchoolID           string    `json:"school_id"`
	SchoolName         string    `json:"school_name,omitempty"`
	FullName           string    `json:"full_name"`
	Phone              string    `json:"phone"`
	Email              string    `json:"email,omitempty"`
	Role               UserRole  `json:"role"`
	ClassID            string    `json:"class_id,omitempty"`
	ClassName          string    `json:"class_name,omitempty"`
	Section            string    `json:"section,omitempty"`
	RollNo             string    `json:"roll_no,omitempty"`
	StudentCode        string    `json:"student_code,omitempty"`
	ParentPhone        string    `json:"parent_phone,omitempty"`
	ParentEmail        string    `json:"parent_email,omitempty"`
	Address            string    `json:"address,omitempty"`
	VerificationStatus string    `json:"verification_status"`
	BlockedReason      string    `json:"blocked_reason,omitempty"`
	VerifiedBy         string    `json:"verified_by,omitempty"`
	IsProfileLocked    bool      `json:"is_profile_locked"`
	IsActive           bool      `json:"is_active"`
	CreatedAt          time.Time `json:"created_at"`
	UpdatedAt          time.Time `json:"updated_at"`
}

type StudentLoginRequest struct {
	StudentID   string `json:"student_id,omitempty"`
	StudentCode string `json:"student_code,omitempty"`
	RollNo      string `json:"roll_no,omitempty"`
	FullName    string `json:"full_name"`
	ClassName   string `json:"class_name"`
	Section     string `json:"section"`
	ParentPhone string `json:"parent_phone,omitempty"`
	ParentEmail string `json:"parent_email,omitempty"`
	Address     string `json:"address,omitempty"`
}

type StaffLoginRequest struct {
	Phone    string `json:"phone"`
	Password string `json:"password"`
}

type LoginResponse struct {
	AccessToken string `json:"access_token"`
	User        User   `json:"user"`
}

type Post struct {
	ID             string     `json:"id"`
	SchoolID       string     `json:"school_id"`
	AuthorID       string     `json:"author_id"`
	AuthorName     string     `json:"author_name,omitempty"`
	Title          string     `json:"title"`
	Content        string     `json:"content"`
	Category       string     `json:"category"`
	TargetType     string     `json:"target_type"`
	TargetClassID  *string    `json:"target_class_id,omitempty"`
	TargetAudience string     `json:"target_audience,omitempty"`
	ImageURL       string     `json:"image_url,omitempty"`
	StartsAt       *time.Time `json:"starts_at,omitempty"`
	ExpiresAt      *time.Time `json:"expires_at,omitempty"`
	Status         string     `json:"status,omitempty"`
	Attachments    []string   `json:"attachments"`
	IsPinned       bool       `json:"is_pinned"`
	CreatedAt      time.Time  `json:"created_at"`
}

type Discussion struct {
	ID          string            `json:"id"`
	SchoolID    string            `json:"school_id"`
	ClassID     string            `json:"class_id"`
	AuthorID    string            `json:"author_id"`
	AuthorName  string            `json:"author_name"`
	SubjectName string            `json:"subject_name"`
	TopicTitle  string            `json:"topic_title"`
	Content     string            `json:"content"`
	ImageURL    string            `json:"image_url,omitempty"`
	CreatedAt   time.Time         `json:"created_at"`
	Replies     []DiscussionReply `json:"replies,omitempty"`
}

type DiscussionReply struct {
	ID           string    `json:"id"`
	DiscussionID string    `json:"discussion_id"`
	SchoolID     string    `json:"school_id"`
	AuthorID     string    `json:"author_id"`
	AuthorName   string    `json:"author_name"`
	Content      string    `json:"content"`
	ImageURL     string    `json:"image_url,omitempty"`
	IsVerified   bool      `json:"is_verified"`
	CreatedAt    time.Time `json:"created_at"`
}

type AcademicNote struct {
	ID          string    `json:"id"`
	SchoolID    string    `json:"school_id"`
	ClassID     string    `json:"class_id"`
	TeacherID   string    `json:"teacher_id"`
	TeacherName string    `json:"teacher_name,omitempty"`
	SubjectName string    `json:"subject_name"`
	Title       string    `json:"title"`
	ChapterName string    `json:"chapter_name"`
	FileURL     string    `json:"file_url"`
	FileType    string    `json:"file_type"`
	CreatedAt   time.Time `json:"created_at"`
}

type SiblingSubmission struct {
	ID          string      `json:"id"`
	SchoolID    string      `json:"school_id"`
	StudentID   string      `json:"student_id"`
	StudentName string      `json:"student_name"`
	ClassName   string      `json:"class_name"`
	ParentName  string      `json:"parent_name"`
	ParentPhone string      `json:"parent_phone"`
	ParentEmail string      `json:"parent_email,omitempty"`
	HasSiblings bool        `json:"has_siblings"`
	Siblings    interface{} `json:"siblings"`
	Status      string      `json:"status"`
	CreatedAt   time.Time   `json:"created_at"`
}

type AttendanceRecord struct {
	ID        string    `json:"id"`
	SchoolID  string    `json:"school_id"`
	ClassID   string    `json:"class_id"`
	StudentID string    `json:"student_id"`
	Date      string    `json:"date"`
	Status    string    `json:"status"` // PRESENT, ABSENT, LATE, EXCUSED
	Remarks   string    `json:"remarks,omitempty"`
	CreatedAt time.Time `json:"created_at"`
}

type ClassSubject struct {
	ID              string    `json:"id"`
	SchoolID        string    `json:"school_id"`
	ClassID         string    `json:"class_id"`
	SubjectName     string    `json:"subject_name"`
	DefaultMaxMarks float64   `json:"default_max_marks"`
	CreatedAt       time.Time `json:"created_at"`
}

type ExamItem struct {
	ID           string    `json:"id"`
	SchoolID     string    `json:"school_id"`
	Title        string    `json:"title"`
	Term         string    `json:"term"`
	AcademicYear string    `json:"academic_year"`
	ClassID      string    `json:"class_id"`
	StartDate    string    `json:"start_date,omitempty"`
	CreatedAt    time.Time `json:"created_at"`
}

type ExamSubjectConfig struct {
	ID          string  `json:"id"`
	ExamID      string  `json:"exam_id"`
	ClassID     string  `json:"class_id"`
	SubjectName string  `json:"subject_name"`
	MaxMarks    float64 `json:"max_marks"`
	PassMarks   float64 `json:"pass_marks"`
}

type SubjectMark struct {
	SubjectName   string  `json:"subject_name"`
	MarksObtained float64 `json:"marks_obtained"`
	MaxMarks      float64 `json:"max_marks"`
	Grade         string  `json:"grade"`
	Remarks       string  `json:"remarks,omitempty"`
}

type ExamResult struct {
	ExamID      string        `json:"exam_id"`
	ExamName    string        `json:"exam_name"`
	StudentID   string        `json:"student_id"`
	StudentName string        `json:"student_name"`
	ClassName   string        `json:"class_name"`
	Section     string        `json:"section"`
	TotalMarks  float64       `json:"total_marks"`
	MaxMarks    float64       `json:"max_marks"`
	Percentage  float64       `json:"percentage"`
	Grade       string        `json:"grade"`
	Rank        int           `json:"rank,omitempty"`
	Subjects    []SubjectMark `json:"subjects"`
	StatusCard  string        `json:"status_card_url,omitempty"`
}

type ApiResponse struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data,omitempty"`
	Message string      `json:"message,omitempty"`
	Error   string      `json:"error,omitempty"`
}
