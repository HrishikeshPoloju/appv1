-- ====================================================================
-- GENESIS SCHOOL SAAS PLATFORM — PRODUCTION POSTGRESQL SCHEMA
-- Target Instance: genesis-school-db (Cloud SQL PostgreSQL 18)
-- Region: asia-south1 (Mumbai)
-- ====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Custom Types
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'TEACHER', 'PARENT', 'STUDENT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE post_category AS ENUM ('CIRCULAR', 'EVENT', 'EMERGENCY', 'ACADEMIC');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE post_target AS ENUM ('ALL', 'CLASS');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE referral_status AS ENUM ('LEAD', 'CONTACTED', 'VISIT_SCHEDULED', 'ENROLLED', 'LOST');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- 2. Schools Table
CREATE TABLE IF NOT EXISTS schools (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    slug text NOT NULL UNIQUE,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 3. Classes Table
CREATE TABLE IF NOT EXISTS classes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name text NOT NULL,
    section text,
    UNIQUE (school_id, name, section)
);
CREATE INDEX IF NOT EXISTS classes_school_id_idx ON classes(school_id);

-- 4. Users Table (Students, Teachers, Admins)
CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    full_name text NOT NULL,
    phone varchar(30) NOT NULL,
    email text,
    password_hash text NOT NULL DEFAULT '',
    role user_role NOT NULL,
    class_id text,
    class_name text,
    section text,
    roll_no varchar(16),
    student_code varchar(32),
    parent_phone varchar(30),
    parent_email text,
    address text,
    verification_status varchar(24) NOT NULL DEFAULT 'PENDING',
    blocked_reason text,
    verified_by text,
    is_profile_locked boolean NOT NULL DEFAULT true,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (school_id, phone)
);
CREATE INDEX IF NOT EXISTS users_school_id_idx ON users(school_id);
CREATE INDEX IF NOT EXISTS users_class_id_idx ON users(school_id, class_id);

-- 5. Posts & Circulars Table
CREATE TABLE IF NOT EXISTS posts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    author_id uuid NOT NULL REFERENCES users(id),
    title text NOT NULL,
    content text NOT NULL,
    category post_category NOT NULL DEFAULT 'CIRCULAR',
    target_type post_target NOT NULL DEFAULT 'ALL',
    target_class_id uuid REFERENCES classes(id),
    attachments jsonb NOT NULL DEFAULT '[]'::jsonb,
    is_pinned boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS posts_school_created_idx ON posts(school_id, created_at DESC);

-- 6. Academic Doubts & Discussions Table
CREATE TABLE IF NOT EXISTS discussions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id text NOT NULL,
    author_id uuid NOT NULL REFERENCES users(id),
    author_name text NOT NULL DEFAULT '',
    subject_name text NOT NULL,
    topic_title text NOT NULL,
    content text NOT NULL,
    image_url text,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS discussions_school_created_idx ON discussions(school_id, class_id, created_at DESC);

-- 7. Discussion Replies Table
CREATE TABLE IF NOT EXISTS discussion_replies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    discussion_id uuid NOT NULL REFERENCES discussions(id) ON DELETE CASCADE,
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    author_id uuid NOT NULL REFERENCES users(id),
    author_name text NOT NULL DEFAULT '',
    content text NOT NULL,
    image_url text,
    is_verified boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS discussion_replies_idx ON discussion_replies(discussion_id, created_at ASC);

-- 8. Academic Notes & Study Materials Table
CREATE TABLE IF NOT EXISTS academic_notes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id text NOT NULL,
    teacher_id uuid NOT NULL REFERENCES users(id),
    subject_name text NOT NULL,
    title text NOT NULL,
    chapter_name text NOT NULL,
    file_url text NOT NULL,
    file_type text NOT NULL DEFAULT 'PDF',
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS academic_notes_class_idx ON academic_notes(class_id, created_at DESC);

-- 9. Daily Attendance Table
CREATE TABLE IF NOT EXISTS attendance_records (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id text NOT NULL,
    student_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    date date NOT NULL DEFAULT CURRENT_DATE,
    status varchar(20) NOT NULL DEFAULT 'PRESENT',
    remarks text,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (school_id, student_id, date)
);
CREATE INDEX IF NOT EXISTS attendance_class_date_idx ON attendance_records(school_id, class_id, date);

-- 10. Class-Specific Subjects Table (Handles multiple different subjects per class & section)
CREATE TABLE IF NOT EXISTS class_subjects (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id text NOT NULL,
    subject_name text NOT NULL,
    default_max_marks numeric(5,2) NOT NULL DEFAULT 100.0,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (school_id, class_id, subject_name)
);
CREATE INDEX IF NOT EXISTS class_subjects_class_idx ON class_subjects(school_id, class_id);

-- 11. Examinations Table
CREATE TABLE IF NOT EXISTS exams (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    title text NOT NULL,
    term text NOT NULL DEFAULT 'Term 1',
    academic_year text NOT NULL DEFAULT '2026-2027',
    class_id text NOT NULL DEFAULT 'ALL',
    start_date date,
    created_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE exams ADD COLUMN IF NOT EXISTS class_id text NOT NULL DEFAULT 'ALL';

-- 12. Exam-Specific Subject Max Marks Configuration Table
CREATE TABLE IF NOT EXISTS exam_subjects (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    exam_id uuid NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    class_id text NOT NULL,
    subject_name text NOT NULL,
    max_marks numeric(5,2) NOT NULL DEFAULT 100.0,
    pass_marks numeric(5,2) NOT NULL DEFAULT 35.0,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (exam_id, class_id, subject_name)
);
CREATE INDEX IF NOT EXISTS exam_subjects_exam_class_idx ON exam_subjects(exam_id, class_id);

-- 13. Exam Marks Table (With Telugu, Hindi, English, Maths, Science, Social)
CREATE TABLE IF NOT EXISTS exam_marks (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    exam_id uuid NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_name text NOT NULL,
    marks_obtained numeric(5,2) NOT NULL,
    max_marks numeric(5,2) NOT NULL DEFAULT 100.0,
    grade varchar(10) NOT NULL DEFAULT 'A+',
    remarks text,
    status_card_url text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (exam_id, student_id, subject_name)
);
ALTER TABLE exam_marks ADD COLUMN IF NOT EXISTS status_card_url text;
ALTER TABLE exam_marks ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now();
CREATE INDEX IF NOT EXISTS exam_marks_student_idx ON exam_marks(student_id);

-- 14. Homework Assignments Table (Multi-section homework & template distribution)
CREATE TABLE IF NOT EXISTS homework_assignments (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    class_id text NOT NULL,
    teacher_id uuid REFERENCES users(id) ON DELETE SET NULL,
    teacher_name text NOT NULL DEFAULT 'Faculty',
    subject_name text NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    due_date text NOT NULL,
    attachments jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS homework_class_idx ON homework_assignments(school_id, class_id, created_at DESC);

-- 12. Referral Codes Table
CREATE TABLE IF NOT EXISTS referral_codes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code varchar(32) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (school_id, user_id)
);

-- 13. Referral Leads Table
CREATE TABLE IF NOT EXISTS referral_leads (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    referral_code_id uuid NOT NULL REFERENCES referral_codes(id),
    referrer_user_id uuid NOT NULL REFERENCES users(id),
    parent_name text NOT NULL,
    parent_phone varchar(30) NOT NULL,
    parent_email text,
    child_name text NOT NULL,
    desired_grade text NOT NULL,
    status referral_status NOT NULL DEFAULT 'LEAD',
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 14. Siblings Concession Submissions Table
CREATE TABLE IF NOT EXISTS siblings_submissions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid REFERENCES schools(id) ON DELETE CASCADE,
    student_id text NOT NULL,
    student_name text NOT NULL,
    class_name text NOT NULL,
    parent_name text NOT NULL,
    parent_phone varchar(30) NOT NULL,
    parent_email text,
    has_siblings boolean NOT NULL DEFAULT false,
    siblings jsonb NOT NULL DEFAULT '[]'::jsonb,
    status text NOT NULL DEFAULT 'PENDING',
    created_at timestamptz NOT NULL DEFAULT now()
);

-- ====================================================================
-- SEED INITIAL DATA FOR GENESIS HIGH SCHOOL
-- ====================================================================

-- Default School
INSERT INTO schools (id, name, slug)
VALUES ('00000000-0000-0000-0000-000000000001', 'Genesis High School', 'genesis-high-school')
ON CONFLICT (slug) DO NOTHING;

-- Sample Class Sections
INSERT INTO classes (id, school_id, name, section) VALUES
('11111111-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Class 10', 'A'),
('11111111-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Class 10', 'B')
ON CONFLICT (school_id, name, section) DO NOTHING;

-- Staff Users (Principal & Faculty)
INSERT INTO users (id, school_id, full_name, phone, role, password_hash, verification_status, is_profile_locked) VALUES
('22222222-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Dr. Ramesh Rao (Principal)', '9848012345', 'ADMIN', '$2a$10$w8c4vM81bT350x/9p6hEgu5b1QGjP5o6w5w6x4jY4bQn0t8k.5K4G', 'VERIFIED', true),
('22222222-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'P. Sharma (Mathematics)', '9848012346', 'TEACHER', '$2a$10$w8c4vM81bT350x/9p6hEgu5b1QGjP5o6w5w6x4jY4bQn0t8k.5K4G', 'VERIFIED', true)
ON CONFLICT (school_id, phone) DO NOTHING;

-- Student Roster
INSERT INTO users (id, school_id, full_name, phone, role, class_id, class_name, section, roll_no, parent_phone, verification_status, is_profile_locked) VALUES
('33333333-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Aarav Sharma', '9876543210', 'STUDENT', 'class-10a', 'Class 10', 'A', '1', '9876543210', 'VERIFIED', true),
('33333333-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Ananya Reddy', '9876543211', 'STUDENT', 'class-10a', 'Class 10', 'A', '2', '9876543211', 'VERIFIED', true),
('33333333-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'Ishaan Verma', '9876543212', 'STUDENT', 'class-10a', 'Class 10', 'A', '3', '9876543212', 'VERIFIED', true)
ON CONFLICT (school_id, phone) DO NOTHING;

-- Term 1 Examination
INSERT INTO exams (id, school_id, title, term, academic_year, start_date)
VALUES ('44444444-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Term 1 Final Examination', 'Term 1', '2026-2027', '2026-11-10')
ON CONFLICT DO NOTHING;

-- Marks for Aarav Sharma (Including Telugu & Hindi, 550/600, 91.7%, Grade A+)
INSERT INTO exam_marks (school_id, exam_id, student_id, subject_name, marks_obtained, max_marks, grade, remarks) VALUES
('00000000-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'Telugu', 92.0, 100.0, 'A+', 'Outstanding prose & poetry analysis'),
('00000000-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'Hindi', 88.0, 100.0, 'A', 'Great grammar & comprehension'),
('00000000-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'English', 95.0, 100.0, 'A+', 'Exceptional essay structure'),
('00000000-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'Mathematics', 98.0, 100.0, 'A+', 'Near perfect score in geometry'),
('00000000-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'Science', 91.0, 100.0, 'A+', 'Strong conceptual clarity in physics'),
('00000000-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'Social Studies', 86.0, 100.0, 'A', 'Good map-work and historical analysis')
ON CONFLICT (exam_id, student_id, subject_name) DO NOTHING;
