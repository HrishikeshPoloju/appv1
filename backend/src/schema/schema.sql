CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role AS ENUM ('ADMIN', 'TEACHER', 'PARENT', 'STUDENT');
CREATE TYPE post_category AS ENUM ('CIRCULAR', 'EVENT', 'EMERGENCY', 'ACADEMIC');
CREATE TYPE post_target AS ENUM ('ALL', 'CLASS');
CREATE TYPE referral_status AS ENUM ('LEAD', 'CONTACTED', 'VISIT_SCHEDULED', 'ENROLLED', 'LOST');

CREATE TABLE schools (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL,
  slug text NOT NULL UNIQUE,
  is_active boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE classes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  name text NOT NULL,
  section text,
  UNIQUE (school_id, name, section)
);
CREATE INDEX classes_school_id_idx ON classes(school_id);

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  full_name text NOT NULL,
  phone varchar(30) NOT NULL,
  email text,
  password_hash text NOT NULL,
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
CREATE INDEX users_school_id_idx ON users(school_id);
CREATE INDEX users_class_id_idx ON users(school_id, class_id);

CREATE TABLE posts (
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
  created_at timestamptz NOT NULL DEFAULT now(),
  CHECK ((target_type = 'ALL' AND target_class_id IS NULL) OR (target_type = 'CLASS' AND target_class_id IS NOT NULL))
);
CREATE INDEX posts_school_created_idx ON posts(school_id, created_at DESC);

CREATE TABLE discussions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  class_id text NOT NULL,
  author_id uuid NOT NULL REFERENCES users(id),
  subject_name text NOT NULL,
  topic_title text NOT NULL,
  content text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX discussions_school_created_idx ON discussions(school_id, class_id, created_at DESC);

CREATE TABLE discussion_replies (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  discussion_id uuid NOT NULL REFERENCES discussions(id) ON DELETE CASCADE,
  school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  author_id uuid NOT NULL REFERENCES users(id),
  content text NOT NULL,
  is_verified boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX discussion_replies_discussion_idx ON discussion_replies(discussion_id, created_at ASC);

CREATE TABLE academic_notes (
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
CREATE INDEX academic_notes_class_idx ON academic_notes(class_id, created_at DESC);

CREATE TABLE referral_codes (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  school_id uuid NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code varchar(32) NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (school_id, user_id)
);

CREATE TABLE referral_leads (
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
CREATE INDEX referral_leads_referrer_idx ON referral_leads(referrer_user_id, status);

CREATE TABLE siblings_submissions (
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
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX siblings_parent_phone_idx ON siblings_submissions(parent_phone);

-- Application connections should execute SET app.school_id = '<tenant UUID>' before queries.
-- These policies provide database-level tenant isolation in addition to API-level filtering.
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE discussions ENABLE ROW LEVEL SECURITY;
ALTER TABLE discussion_replies ENABLE ROW LEVEL SECURITY;
ALTER TABLE academic_notes ENABLE ROW LEVEL SECURITY;
ALTER TABLE referral_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE referral_leads ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_users ON users USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_classes ON classes USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_posts ON posts USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_discussions ON discussions USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_discussion_replies ON discussion_replies USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_academic_notes ON academic_notes USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_referral_codes ON referral_codes USING (school_id = current_setting('app.school_id', true)::uuid);
CREATE POLICY tenant_referral_leads ON referral_leads USING (school_id = current_setting('app.school_id', true)::uuid);
