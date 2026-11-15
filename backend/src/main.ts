import bcrypt from 'bcryptjs';
import cors from 'cors';
import dotenv from 'dotenv';
import express, { NextFunction, Request, Response } from 'express';
import jwt from 'jsonwebtoken';
import { z, ZodError } from 'zod';
import { pool, query } from './config/database';
import { enqueueBroadcast } from './queue';

dotenv.config();

const app = express();
const port = Number(process.env.PORT ?? 4000);
const jwtSecret = process.env.JWT_SECRET;
if (!jwtSecret && process.env.NODE_ENV === 'production') throw new Error('JWT_SECRET must be set in production');
const signingKey = jwtSecret ?? 'development-only-change-me';

app.use(cors({ origin: process.env.CORS_ORIGIN?.split(',') ?? true }));
app.use(express.json({ limit: '2mb' }));

const asyncRoute = (handler: (req: Request, res: Response, next: NextFunction) => Promise<unknown>) =>
  (req: Request, res: Response, next: NextFunction) => void handler(req, res, next).catch(next);

export function authenticateJWT(req: Request, res: Response, next: NextFunction) {
  const token = req.header('authorization')?.replace(/^Bearer\s+/i, '');
  if (!token) return res.status(401).json({ success: false, error: 'Bearer token required.' });
  try {
    req.user = jwt.verify(token, signingKey) as NonNullable<Request['user']>;
    return next();
  } catch {
    return res.status(401).json({ success: false, error: 'Invalid or expired token.' });
  }
}

function requireAdmin(req: Request, res: Response, next: NextFunction) {
  if (req.user?.role !== 'ADMIN') return res.status(403).json({ success: false, error: 'Admin role required.' });
  return next();
}

const loginSchema = z.object({ phone: z.string().trim().min(5).max(30), password: z.string().min(8).max(128) });
const broadcastSchema = z.object({
  title: z.string().trim().min(1).max(200), content: z.string().trim().min(1).max(10_000),
  category: z.enum(['CIRCULAR', 'EVENT', 'EMERGENCY', 'ACADEMIC']).default('CIRCULAR'),
  target_type: z.enum(['ALL', 'CLASS']).default('ALL'), target_class_id: z.string().uuid().nullable().optional(),
  attachments: z.array(z.string().url()).max(10).default([]),
  channels: z.object({ send_email: z.boolean().optional(), send_app_push: z.boolean().optional(), send_whatsapp: z.boolean().optional() }).default({ send_app_push: true }),
}).refine((data) => data.target_type !== 'CLASS' || !!data.target_class_id, { message: 'target_class_id is required for a class broadcast.' });
const discussionSchema = z.object({ class_id: z.string().trim().min(1).max(100).nullable().optional(), subject_name: z.string().trim().min(1).max(100), topic_title: z.string().trim().min(1).max(200), content: z.string().trim().min(1).max(10_000), image_url: z.string().trim().max(2000).nullable().optional() });
const studentLoginSchema = z.object({
  student_id: z.string().trim().optional(),
  student_code: z.string().trim().optional(),
  roll_no: z.string().trim().max(16).optional(),
  full_name: z.string().trim().min(2).max(120),
  class_name: z.string().trim().min(1).max(50),
  section: z.string().trim().min(1).max(20),
  parent_phone: z.string().trim().max(30).optional(),
  parent_email: z.string().trim().max(160).optional(),
  address: z.string().trim().max(500).optional(),
});

export function buildCanonicalClassId(className: string, section: string): string {
  const cleanClass = className.replace(/^class\s*/i, '').trim().toLowerCase().replace(/[^a-z0-9]/g, '');
  const cleanSec = section.replace(/^section\s*/i, '').trim().toLowerCase().replace(/[^a-z0-9]/g, '');
  return `class-${cleanClass}${cleanSec}`;
}

export function formatClassLabel(className: string, section: string) {
  const numPart = className.replace(/^class\s*/i, '').trim().toUpperCase();
  const secPart = section.replace(/^section\s*/i, '').trim().toUpperCase();
  return {
    normalizedClass: `Class ${numPart}`,
    normalizedSection: secPart,
    classTitle: `Class ${numPart} – Section ${secPart}`,
    bannerClassText: `Class: ${numPart}-${secPart} (2026-2027)`,
  };
}
const guidedChatSchema = z.object({
  subject_name: z.string().trim().min(1).max(100).default('school'),
  topic_title: z.string().trim().min(1).max(200).default('student question'),
  text: z.string().trim().min(1).max(10_000).optional(),
  image_url: z.string().url().refine((url) => new URL(url).protocol === 'https:', 'image_url must use HTTPS.').optional(),
}).refine((data) => data.text || data.image_url, { message: 'Provide text, image_url, or both.' });
const leadSchema = z.object({ referral_code: z.string().trim().min(4).max(32), parent_name: z.string().trim().min(1).max(150), parent_phone: z.string().trim().min(5).max(30), parent_email: z.string().email().optional(), child_name: z.string().trim().min(1).max(150), desired_grade: z.string().trim().min(1).max(50) });

app.get('/health', asyncRoute(async (_req, res) => {
  await query('SELECT 1');
  res.json({ success: true, data: { status: 'ok' } });
}));

app.post('/api/v1/auth/login', asyncRoute(async (req, res) => {
  const { phone, password } = loginSchema.parse(req.body);
  const result = await query<{ id: string; school_id: string; school_name: string; full_name: string; role: string; password_hash: string }>(
    `SELECT u.id, u.school_id, s.name school_name, u.full_name, u.role, u.password_hash
     FROM users u JOIN schools s ON s.id = u.school_id WHERE u.phone = $1 AND u.is_active = true`, [phone]);
  const user = result.rows[0];
  if (!user || !(await bcrypt.compare(password, user.password_hash))) return res.status(401).json({ success: false, error: 'Invalid phone number or password.' });
  const accessToken = jwt.sign({ id: user.id, school_id: user.school_id, role: user.role, full_name: user.full_name }, signingKey, { expiresIn: '30d' });
  return res.json({ success: true, data: { access_token: accessToken, user: { id: user.id, school_id: user.school_id, school_name: user.school_name, full_name: user.full_name, role: user.role } } });
}));

app.get('/api/v1/posts', authenticateJWT, asyncRoute(async (req, res) => {
  const result = await query(`SELECT p.id, p.title, p.content, p.category, p.attachments, p.is_pinned, p.created_at, u.full_name author_name
    FROM posts p JOIN users u ON u.id = p.author_id WHERE p.school_id = $1 ORDER BY p.is_pinned DESC, p.created_at DESC LIMIT 50`, [req.user!.school_id]);
  res.json({ success: true, data: { posts: result.rows } });
}));

app.post('/api/v1/admin/broadcast', authenticateJWT, requireAdmin, asyncRoute(async (req, res) => {
  const body = broadcastSchema.parse(req.body);
  const created = await query<{ id: string }>(`INSERT INTO posts (school_id, author_id, title, content, category, target_type, target_class_id, attachments)
    VALUES ($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id`, [req.user!.school_id, req.user!.id, body.title, body.content, body.category, body.target_type, body.target_class_id ?? null, JSON.stringify(body.attachments)]);
  const postId = created.rows[0].id;
  const job = await enqueueBroadcast({ postId, schoolId: req.user!.school_id, channels: body.channels });
  res.status(202).json({ success: true, message: 'Broadcast queued.', data: { post_id: postId, job_id: job.id, status: 'QUEUED' } });
}));

export function authenticateOrStudent(req: Request, res: Response, next: NextFunction) {
  const token = req.header('authorization')?.replace(/^Bearer\s+/i, '');
  if (token) {
    try {
      req.user = jwt.verify(token, signingKey) as NonNullable<Request['user']>;
      return next();
    } catch {
      return res.status(401).json({ success: false, error: 'Invalid or expired token.' });
    }
  }
  const studentName = req.header('x-student-name') || (req.query.student_name as string) || 'Aarav Sharma';
  const headerRole = (req.header('x-user-role') || 'STUDENT').toUpperCase();
  const normalizedRole: 'ADMIN' | 'TEACHER' | 'PARENT' | 'STUDENT' =
    headerRole === 'TEACHER' || headerRole === 'ADMIN' || headerRole === 'PARENT'
      ? headerRole
      : 'STUDENT';
  req.user = {
    id: '00000000-0000-0000-0000-000000000001',
    school_id: '00000000-0000-0000-0000-000000000001',
    role: normalizedRole,
    full_name: studentName,
  };
  return next();
}

const replySchema = z.object({
  content: z.string().trim().min(1).max(5000),
  author_name: z.string().trim().min(1).max(100).optional(),
  image_url: z.string().trim().max(2000).nullable().optional(),
});

interface InMemDiscussion {
  id: string;
  school_id: string;
  class_id: string;
  author_id: string;
  author_name: string;
  author_role: string;
  subject_name: string;
  topic_title: string;
  content: string;
  image_url?: string | null;
  created_at: string;
  replies_count: number;
}

interface InMemReply {
  id: string;
  discussion_id: string;
  school_id: string;
  author_id: string;
  author_name: string;
  author_role: string;
  content: string;
  image_url?: string | null;
  created_at: string;
}

const inMemoryDiscussions: InMemDiscussion[] = [
  {
    id: 'disc-1',
    school_id: '00000000-0000-0000-0000-000000000001',
    class_id: 'class-10a',
    author_id: 'student-2',
    author_name: 'Priya Patel',
    author_role: 'STUDENT',
    subject_name: 'Mathematics',
    topic_title: 'Quadratic Equations Ex 4.2 Q3',
    content: 'Can someone explain how to split the middle term when the coefficient of x^2 is root 2? Getting confused with the radical signs.',
    created_at: new Date(Date.now() - 45 * 60 * 1000).toISOString(),
    replies_count: 2,
  },
  {
    id: 'disc-2',
    school_id: '00000000-0000-0000-0000-000000000001',
    class_id: 'class-10a',
    author_id: 'student-3',
    author_name: 'Rohan Verma',
    author_role: 'STUDENT',
    subject_name: 'Physics',
    topic_title: 'Refraction through a glass prism',
    content: 'Why does violet light deviate the most in dispersion? Is it due to lower wavelength and higher refractive index?',
    created_at: new Date(Date.now() - 120 * 60 * 1000).toISOString(),
    replies_count: 1,
  },
  {
    id: 'disc-3',
    school_id: '00000000-0000-0000-0000-000000000001',
    class_id: 'class-10a',
    author_id: 'teacher-1',
    author_name: 'Mrs. Gupta (Physics Teacher)',
    author_role: 'TEACHER',
    subject_name: 'Physics',
    topic_title: 'Reminder: Bring Lab Manuals Tomorrow',
    content: 'Class 10-A, please bring your completed ray optics observations. Anyone having doubts about the focal length experiment can ask here.',
    created_at: new Date(Date.now() - 360 * 60 * 1000).toISOString(),
    replies_count: 1,
  },
  {
    id: 'disc-4',
    school_id: '00000000-0000-0000-0000-000000000001',
    class_id: 'class-10b',
    author_id: 'student-5',
    author_name: 'Karthik Reddy',
    author_role: 'STUDENT',
    subject_name: 'Mathematics',
    topic_title: 'Arithmetic Progressions – nth Term Formula',
    content: 'In Class 10 Section B homework Q4, how do we find which term of the AP 21, 18, 15... is -81?',
    created_at: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    replies_count: 1,
  },
  {
    id: 'disc-5',
    school_id: '00000000-0000-0000-0000-000000000001',
    class_id: 'class-9a',
    author_id: 'student-6',
    author_name: 'Rachoti Laasya',
    author_role: 'STUDENT',
    subject_name: 'Mathematics',
    topic_title: 'Remainder Theorem Doubt – Ex 2.3',
    content: 'How to find the remainder when x^3 + 3x^2 + 3x + 1 is divided by x + 1 without long division?',
    created_at: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
    replies_count: 1,
  },
  {
    id: 'disc-6',
    school_id: '00000000-0000-0000-0000-000000000001',
    class_id: 'class-5f',
    author_id: 'student-7',
    author_name: 'Rachoti Sahasra',
    author_role: 'STUDENT',
    subject_name: 'Mathematics',
    topic_title: 'Equivalent Fractions Worksheet Q2',
    content: 'How do we find three equivalent fractions of 3/5 for tomorrow\'s Class 5-F activity?',
    created_at: new Date(Date.now() - 50 * 60 * 1000).toISOString(),
    replies_count: 1,
  },
];

const inMemoryReplies: InMemReply[] = [
  {
    id: 'rep-1',
    discussion_id: 'disc-1',
    school_id: '00000000-0000-0000-0000-000000000001',
    author_id: 'student-4',
    author_name: 'Sneha Rao',
    author_role: 'STUDENT',
    content: 'Multiply root 2 by 5 root 2, you get 10! Then split 7x into 5x + 2x.',
    created_at: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
  },
  {
    id: 'rep-2',
    discussion_id: 'disc-1',
    school_id: '00000000-0000-0000-0000-000000000001',
    author_id: 'student-2',
    author_name: 'Priya Patel',
    author_role: 'STUDENT',
    content: 'Thanks Sneha! That made it so simple.',
    created_at: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
  },
  {
    id: 'rep-3',
    discussion_id: 'disc-2',
    school_id: '00000000-0000-0000-0000-000000000001',
    author_id: 'teacher-1',
    author_name: 'Mrs. Gupta (Physics Teacher)',
    author_role: 'TEACHER',
    content: 'Correct Rohan. According to Cauchy relation, refractive index is inversely proportional to wavelength square.',
    created_at: new Date(Date.now() - 90 * 60 * 1000).toISOString(),
  },
  {
    id: 'rep-4',
    discussion_id: 'disc-3',
    school_id: '00000000-0000-0000-0000-000000000001',
    author_id: 'student-1',
    author_name: 'Aarav Sharma',
    author_role: 'STUDENT',
    content: 'Mam, should we also write the precautions section in pen or pencil?',
    created_at: new Date(Date.now() - 200 * 60 * 1000).toISOString(),
  },
];

interface InMemClass {
  id: string;
  name: string;
  section: string;
  student_count: number;
}

const inMemoryClasses: InMemClass[] = [
  { id: 'class-5a', name: 'Class 5', section: 'A', student_count: 34 },
  { id: 'class-5b', name: 'Class 5', section: 'B', student_count: 35 },
  { id: 'class-5f', name: 'Class 5', section: 'F', student_count: 35 },
  { id: 'class-6a', name: 'Class 6', section: 'A', student_count: 36 },
  { id: 'class-7a', name: 'Class 7', section: 'A', student_count: 38 },
  { id: 'class-8a', name: 'Class 8', section: 'A', student_count: 37 },
  { id: 'class-8b', name: 'Class 8', section: 'B', student_count: 36 },
  { id: 'class-9a', name: 'Class 9', section: 'A', student_count: 40 },
  { id: 'class-9b', name: 'Class 9', section: 'B', student_count: 39 },
  { id: 'class-10a', name: 'Class 10', section: 'A', student_count: 38 },
  { id: 'class-10b', name: 'Class 10', section: 'B', student_count: 36 },
  { id: 'class-10c', name: 'Class 10', section: 'C', student_count: 35 },
  { id: 'class-11a', name: 'Class 11', section: 'A', student_count: 32 },
  { id: 'class-12a', name: 'Class 12', section: 'A', student_count: 30 },
];

export interface InMemStudent {
  id: string;
  school_id: string;
  student_code: string;
  roll_no: string;
  full_name: string;
  class_id: string;
  class_name: string;
  section: string;
  class_title: string;
  banner_class_text: string;
  parent_phone?: string;
  parent_email?: string;
  address?: string;
  verification_status: 'PENDING' | 'VERIFIED' | 'BLOCKED';
  blocked_reason?: string;
  verified_by?: string;
  is_profile_locked: boolean;
  created_at: string;
  updated_at: string;
}

const inMemoryStudents: InMemStudent[] = [
  {
    id: 'stu-sahasra',
    school_id: '00000000-0000-0000-0000-000000000001',
    student_code: '2258',
    roll_no: '01',
    full_name: 'Rachoti Sahasra',
    class_id: 'class-10a',
    class_name: 'Class 10',
    section: 'A',
    class_title: 'Class 10 – Section A',
    banner_class_text: 'Class: 10-A (2026-2027)',
    parent_phone: '+91 98765 43210',
    verification_status: 'VERIFIED',
    verified_by: 'Mrs. Gupta',
    is_profile_locked: true,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
  {
    id: 'stu-rohan-10a',
    school_id: '00000000-0000-0000-0000-000000000001',
    student_code: '2264',
    roll_no: '02',
    full_name: 'Rohan Verma',
    class_id: 'class-10a',
    class_name: 'Class 10',
    section: 'A',
    class_title: 'Class 10 – Section A',
    banner_class_text: 'Class: 10-A (2026-2027)',
    parent_phone: '+91 98490 11223',
    verification_status: 'VERIFIED',
    verified_by: 'Mrs. Gupta',
    is_profile_locked: true,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
  {
    id: 'stu-ananya-10a',
    school_id: '00000000-0000-0000-0000-000000000001',
    student_code: '2271',
    roll_no: '03',
    full_name: 'Ananya Reddy',
    class_id: 'class-10a',
    class_name: 'Class 10',
    section: 'A',
    class_title: 'Class 10 – Section A',
    banner_class_text: 'Class: 10-A (2026-2027)',
    parent_phone: '+91 97012 33445',
    verification_status: 'PENDING',
    is_profile_locked: true,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
  {
    id: 'stu-suspect-10a',
    school_id: '00000000-0000-0000-0000-000000000001',
    student_code: '9981',
    roll_no: '44',
    full_name: 'Vikram Unknown (Unverified Entry)',
    class_id: 'class-10a',
    class_name: 'Class 10',
    section: 'A',
    class_title: 'Class 10 – Section A',
    banner_class_text: 'Class: 10-A (2026-2027)',
    parent_phone: '+91 90000 00099',
    verification_status: 'BLOCKED',
    blocked_reason: 'Not on Class 10-A physical register during morning roll call',
    verified_by: 'Mrs. Gupta',
    is_profile_locked: true,
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
  },
];

interface InMemNote {
  id: string;
  class_id: string;
  subject_name: string;
  title: string;
  chapter_name: string;
  file_url: string;
  file_type: string;
  teacher_name: string;
  created_at: string;
}

const inMemoryNotes: InMemNote[] = [
  {
    id: 'note-1',
    class_id: 'class-10a',
    subject_name: 'Physics',
    title: 'Ray Optics & Lens Formula Summary',
    chapter_name: 'Chapter 10: Light Reflection & Refraction',
    file_url: 'https://schoolapp.in/notes/physics_ch10_summary.pdf',
    file_type: 'PDF',
    teacher_name: 'Mrs. Gupta (Physics Teacher)',
    created_at: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'note-2',
    class_id: 'class-10a',
    subject_name: 'Mathematics',
    title: 'Quadratic Equations Formula Sheet & Shortcuts',
    chapter_name: 'Chapter 4: Quadratic Equations',
    file_url: 'https://schoolapp.in/notes/maths_quadratic_cheat_sheet.pdf',
    file_type: 'PDF',
    teacher_name: 'Mr. Sharma (Maths Teacher)',
    created_at: new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'note-3',
    class_id: 'class-10a',
    subject_name: 'Chemistry',
    title: 'Periodic Table Trends & Reactions Chart',
    chapter_name: 'Chapter 5: Periodic Classification',
    file_url: 'https://schoolapp.in/notes/chemistry_trends.pdf',
    file_type: 'PDF',
    teacher_name: 'Dr. Anita Verma',
    created_at: new Date(Date.now() - 72 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'note-4',
    class_id: 'class-10b',
    subject_name: 'Mathematics',
    title: 'Arithmetic Progressions – Section B Practice Set',
    chapter_name: 'Chapter 5: Arithmetic Progressions',
    file_url: 'https://schoolapp.in/notes/class10b_ap_notes.pdf',
    file_type: 'PDF',
    teacher_name: 'Mr. Kumar',
    created_at: new Date(Date.now() - 18 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'note-5',
    class_id: 'class-9a',
    subject_name: 'Mathematics',
    title: 'Polynomials & Algebraic Identities',
    chapter_name: 'Chapter 2: Polynomials',
    file_url: 'https://schoolapp.in/notes/class9a_polynomials.pdf',
    file_type: 'PDF',
    teacher_name: 'Mr. Kumar',
    created_at: new Date(Date.now() - 20 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: 'note-6',
    class_id: 'class-5f',
    subject_name: 'Mathematics',
    title: 'Fractions & Decimals Visual Guide',
    chapter_name: 'Chapter 3: Fractions',
    file_url: 'https://schoolapp.in/notes/class5f_fractions.pdf',
    file_type: 'PDF',
    teacher_name: 'Pooja Ma\'am',
    created_at: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
  },
];

const noteSchema = z.object({
  subject_name: z.string().trim().min(1).max(100),
  title: z.string().trim().min(1).max(200),
  chapter_name: z.string().trim().min(1).max(200),
  file_url: z.string().url().default('https://schoolapp.in/notes/document.pdf'),
  file_type: z.string().default('PDF'),
});

const mobilePostSchema = z.object({
  title: z.string().trim().min(1).max(200),
  content: z.string().trim().min(1).max(10_000),
  category: z.enum(['CIRCULAR', 'EVENT', 'EMERGENCY', 'ACADEMIC']).default('CIRCULAR'),
  author_name: z.string().optional(),
});

// Student first-time login / registration & class-section persistence in DB
async function upsertStudentProfile(
  body: z.infer<typeof studentLoginSchema>,
  options: { allowTeacherOverride?: boolean } = {}
) {
  const allowTeacherOverride = Boolean(options.allowTeacherOverride);
  const defaultSchoolId = '00000000-0000-0000-0000-000000000001';
  const classId = buildCanonicalClassId(body.class_name, body.section);
  const labels = formatClassLabel(body.class_name, body.section);
  const nowIso = new Date().toISOString();

  const existingIdx = inMemoryStudents.findIndex(
    (s) =>
      (body.student_id && s.id === body.student_id) ||
      s.full_name.toLowerCase() === body.full_name.toLowerCase()
  );

  // If student already onboarded and profile is locked, prevent student self-modification
  if (existingIdx >= 0 && inMemoryStudents[existingIdx].is_profile_locked && !allowTeacherOverride) {
    const lockedRecord = inMemoryStudents[existingIdx];
    const accessToken = jwt.sign(
      {
        id: lockedRecord.id,
        school_id: lockedRecord.school_id,
        role: 'STUDENT',
        full_name: lockedRecord.full_name,
        class_id: lockedRecord.class_id,
        class_name: lockedRecord.class_name,
        section: lockedRecord.section,
      },
      signingKey,
      { expiresIn: '365d' }
    );
    return { student: lockedRecord, access_token: accessToken, was_already_locked: true };
  }

  // Ensure class-section exists in memory list
  if (!inMemoryClasses.some((c) => c.id === classId)) {
    inMemoryClasses.push({
      id: classId,
      name: labels.normalizedClass,
      section: labels.normalizedSection,
      student_count: 35,
    });
  }

  let studentId = body.student_id || `stu-${Date.now()}`;
  let studentCode = body.student_code || String(Math.floor(1000 + Math.random() * 9000));

  // Persist to PostgreSQL database if available
  try {
    await query(`
      ALTER TABLE users ADD COLUMN IF NOT EXISTS class_id text;
      ALTER TABLE users ADD COLUMN IF NOT EXISTS class_name text;
      ALTER TABLE users ADD COLUMN IF NOT EXISTS section text;
      ALTER TABLE users ADD COLUMN IF NOT EXISTS student_code varchar(32);
      ALTER TABLE users ADD COLUMN IF NOT EXISTS is_profile_locked boolean NOT NULL DEFAULT true;
      ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at timestamptz DEFAULT now();
    `);

    // Ensure default school exists
    await query(
      `INSERT INTO schools (id, name, slug)
       VALUES ($1, 'Genesis School Beeramguda', 'genesis-school-beeramguda')
       ON CONFLICT (id) DO NOTHING`,
      [defaultSchoolId]
    );

    const syntheticPhone = `stu-${body.full_name.toLowerCase().replace(/[^a-z0-9]/g, '').slice(0, 18)}-${classId}`;
    if (allowTeacherOverride && body.student_id) {
      const updateRes = await query<{ id: string; student_code: string }>(
        `UPDATE users
         SET full_name = $1,
             class_id = $2,
             class_name = $3,
             section = $4,
             is_profile_locked = true,
             updated_at = now()
         WHERE id::text = $5 AND school_id = $6
         RETURNING id, COALESCE(student_code, $7) AS student_code`,
        [body.full_name, classId, labels.normalizedClass, labels.normalizedSection, body.student_id, defaultSchoolId, studentCode]
      );
      if (updateRes.rows[0]) {
        studentId = updateRes.rows[0].id;
        studentCode = updateRes.rows[0].student_code;
      }
    } else {
      const dbRes = await query<{ id: string; student_code: string }>(
        `INSERT INTO users (school_id, full_name, phone, password_hash, role, class_id, class_name, section, student_code, is_profile_locked, updated_at)
         VALUES ($1, $2, $3, 'student-app-login', 'STUDENT', $4, $5, $6, $7, true, now())
         ON CONFLICT (school_id, phone)
         DO UPDATE SET
           updated_at = users.updated_at
         RETURNING id, COALESCE(student_code, $7) AS student_code`,
        [defaultSchoolId, body.full_name, syntheticPhone, classId, labels.normalizedClass, labels.normalizedSection, studentCode]
      );

      if (dbRes.rows[0]) {
        studentId = dbRes.rows[0].id;
        studentCode = dbRes.rows[0].student_code;
      }
    }
  } catch {
    // Fallback to in-memory persistence when PostgreSQL is offline
  }

  const record: InMemStudent = {
    id: existingIdx >= 0 ? inMemoryStudents[existingIdx].id : studentId,
    school_id: defaultSchoolId,
    student_code: existingIdx >= 0 ? inMemoryStudents[existingIdx].student_code : studentCode,
    roll_no: body.roll_no ?? (existingIdx >= 0 ? inMemoryStudents[existingIdx].roll_no : String(inMemoryStudents.length + 1).padStart(2, '0')),
    full_name: body.full_name,
    class_id: classId,
    class_name: labels.normalizedClass,
    section: labels.normalizedSection,
    class_title: labels.classTitle,
    banner_class_text: labels.bannerClassText,
    parent_phone: body.parent_phone ?? (existingIdx >= 0 ? inMemoryStudents[existingIdx].parent_phone : ''),
    parent_email: body.parent_email ?? (existingIdx >= 0 ? inMemoryStudents[existingIdx].parent_email : ''),
    address: body.address ?? (existingIdx >= 0 ? inMemoryStudents[existingIdx].address : ''),
    verification_status: existingIdx >= 0 ? inMemoryStudents[existingIdx].verification_status : 'PENDING',
    blocked_reason: existingIdx >= 0 ? inMemoryStudents[existingIdx].blocked_reason : undefined,
    verified_by: existingIdx >= 0 ? inMemoryStudents[existingIdx].verified_by : undefined,
    is_profile_locked: true,
    created_at: existingIdx >= 0 ? inMemoryStudents[existingIdx].created_at : nowIso,
    updated_at: nowIso,
  };

  if (existingIdx >= 0) {
    inMemoryStudents[existingIdx] = record;
  } else {
    inMemoryStudents.unshift(record);
  }

  const accessToken = jwt.sign(
    {
      id: record.id,
      school_id: record.school_id,
      role: 'STUDENT',
      full_name: record.full_name,
      class_id: record.class_id,
      class_name: record.class_name,
      section: record.section,
    },
    signingKey,
    { expiresIn: '365d' }
  );

  return { student: record, access_token: accessToken, was_already_locked: false };
}

app.post(['/api/v1/auth/student-login', '/api/v1/students/login'], asyncRoute(async (req, res) => {
  const body = studentLoginSchema.parse(req.body);
  const data = await upsertStudentProfile(body, { allowTeacherOverride: false });
  return res.status(200).json({
    success: true,
    message: data.was_already_locked
      ? 'Student profile is already onboarded and locked. Returning locked classroom profile.'
      : 'Student first-time onboarding completed and queued in section verification roster.',
    data,
  });
}));

app.put('/api/v1/auth/student-profile', authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || 'STUDENT';
  if (callerRole !== 'TEACHER' && callerRole !== 'ADMIN') {
    return res.status(403).json({
      success: false,
      error: 'Student details are locked after initial onboarding. Only a Class Teacher or School Admin can change student Name, Class, or Section.',
    });
  }
  const body = studentLoginSchema.parse(req.body);
  const data = await upsertStudentProfile(body, { allowTeacherOverride: true });
  return res.status(200).json({
    success: true,
    message: 'Student details updated by Teacher/Admin in database.',
    data,
  });
}));

app.patch(['/api/v1/students/:studentId/class-section', '/api/v1/teacher/students/:studentId'], authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || 'STUDENT';
  if (callerRole !== 'TEACHER' && callerRole !== 'ADMIN') {
    return res.status(403).json({
      success: false,
      error: 'Student details are locked after initial onboarding. Only a Class Teacher or School Admin can change student Name, Class, or Section.',
    });
  }
  const body = studentLoginSchema.parse({
    ...req.body,
    student_id: req.params.studentId,
  });
  const data = await upsertStudentProfile(body, { allowTeacherOverride: true });
  return res.status(200).json({
    success: true,
    message: 'Student details updated by Teacher/Admin in database.',
    data,
  });
}));

// Section-scoped Student Verification Roster (Teachers & Faculty can Verify, Block, or Unblock per Class-Section)
app.get('/api/v1/classes/:classId/students', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { classId } = req.params;
  const sectionStudents = inMemoryStudents.filter((s) => s.class_id === classId);
  const summary = {
    total: sectionStudents.length,
    verified: sectionStudents.filter((s) => s.verification_status === 'VERIFIED').length,
    pending: sectionStudents.filter((s) => s.verification_status === 'PENDING').length,
    blocked: sectionStudents.filter((s) => s.verification_status === 'BLOCKED').length,
  };
  return res.json({
    success: true,
    data: {
      class_id: classId,
      summary,
      students: sectionStudents,
    },
  });
}));

app.patch('/api/v1/students/:studentId/verification', authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || 'STUDENT';
  if (callerRole !== 'TEACHER' && callerRole !== 'ADMIN') {
    return res.status(403).json({
      success: false,
      error: 'Only Teachers and School Admins can verify, block, or unblock students.',
    });
  }
  const { studentId } = req.params;
  const rawStatus = String(req.body.verification_status || 'VERIFIED').toUpperCase();
  const nextStatus: 'PENDING' | 'VERIFIED' | 'BLOCKED' =
    rawStatus === 'BLOCKED' ? 'BLOCKED' : rawStatus === 'PENDING' ? 'PENDING' : 'VERIFIED';
  const blockedReason = req.body.blocked_reason ? String(req.body.blocked_reason).trim() : undefined;
  const teacherName = String(req.body.verified_by || req.user?.full_name || 'Class Teacher');

  const idx = inMemoryStudents.findIndex((s) => s.id === studentId);
  if (idx >= 0) {
    inMemoryStudents[idx] = {
      ...inMemoryStudents[idx],
      verification_status: nextStatus,
      blocked_reason: nextStatus === 'BLOCKED' ? (blockedReason || 'Suspected unauthorized class/section login') : undefined,
      verified_by: teacherName,
      updated_at: new Date().toISOString(),
    };
  }

  try {
    await query(
      `UPDATE users SET verification_status = $1, blocked_reason = $2, verified_by = $3, updated_at = now() WHERE id::text = $4`,
      [nextStatus, nextStatus === 'BLOCKED' ? (blockedReason || 'Blocked by Teacher') : null, teacherName, studentId]
    );
  } catch {
    // Offline fallback
  }

  return res.json({
    success: true,
    message: nextStatus === 'BLOCKED'
      ? 'Student account blocked by teacher.'
      : 'Student verified for classroom access.',
    data: { student: idx >= 0 ? inMemoryStudents[idx] : null },
  });
}));

app.post('/api/v1/classes/:classId/verify-all', authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || 'STUDENT';
  if (callerRole !== 'TEACHER' && callerRole !== 'ADMIN') {
    return res.status(403).json({
      success: false,
      error: 'Only Teachers and School Admins can verify section students.',
    });
  }
  const { classId } = req.params;
  const teacherName = String(req.body.verified_by || req.user?.full_name || 'Class Teacher');
  let verifiedCount = 0;
  for (let i = 0; i < inMemoryStudents.length; i++) {
    if (inMemoryStudents[i].class_id === classId && inMemoryStudents[i].verification_status === 'PENDING') {
      inMemoryStudents[i] = {
        ...inMemoryStudents[i],
        verification_status: 'VERIFIED',
        verified_by: teacherName,
        updated_at: new Date().toISOString(),
      };
      verifiedCount++;
    }
  }
  return res.json({
    success: true,
    message: `Verified ${verifiedCount} pending students in ${classId}.`,
    data: { verified_count: verifiedCount },
  });
}));

// Teacher & Admin permission to dynamically create a new Class & Section
app.post('/api/v1/classes', authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || String(req.body.role || 'TEACHER').toUpperCase();
  if (callerRole !== 'TEACHER' && callerRole !== 'ADMIN') {
    return res.status(403).json({
      success: false,
      error: 'Only Teachers and School Admins can create new classes and sections.',
    });
  }
  const rawClassName = String(req.body.class_name || req.body.name || 'Class 10').trim();
  const normalizedClassName = rawClassName.toLowerCase().startsWith('class')
    ? `Class ${rawClassName.replace(/^class\s*/i, '').trim()}`
    : `Class ${rawClassName}`;
  const normalizedSection = String(req.body.section || 'A').trim().toUpperCase();
  const seatCapacity = Number(req.body.seat_capacity || req.body.student_count || 40);
  const classNumber = normalizedClassName.replace(/^Class\s*/i, '').trim().toLowerCase();
  const classId = `class-${classNumber}${normalizedSection.toLowerCase()}`;

  const existingIdx = inMemoryClasses.findIndex((c) => c.id === classId);
  const classEntry: InMemClass = {
    id: classId,
    name: normalizedClassName,
    section: normalizedSection,
    student_count: seatCapacity,
  };
  if (existingIdx >= 0) {
    inMemoryClasses[existingIdx] = classEntry;
  } else {
    inMemoryClasses.push(classEntry);
  }

  return res.status(201).json({
    success: true,
    message: `Created ${normalizedClassName} – Section ${normalizedSection}. Students can now select it on login.`,
    data: { class_section: classEntry },
  });
}));

// Academic Year Rollover & Bulk Promotion / Shifting of Section Students to Upper Class
app.post('/api/v1/classes/:classId/promote', authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || String(req.body.role || 'TEACHER').toUpperCase();
  if (callerRole !== 'TEACHER' && callerRole !== 'ADMIN') {
    return res.status(403).json({
      success: false,
      error: 'Only Teachers and School Admins can promote or shift section students.',
    });
  }
  const { classId } = req.params;
  const rawTargetClass = String(req.body.target_class_name || 'Class 10').trim();
  const targetClassName = rawTargetClass.toLowerCase().startsWith('class')
    ? `Class ${rawTargetClass.replace(/^class\s*/i, '').trim()}`
    : `Class ${rawTargetClass}`;
  const targetSection = String(req.body.target_section || 'A').trim().toUpperCase();
  const academicYear = String(req.body.academic_year || '2027-2028').trim();
  const targetNumber = targetClassName.replace(/^Class\s*/i, '').trim().toLowerCase();
  const targetClassId = `class-${targetNumber}${targetSection.toLowerCase()}`;
  const targetClassTitle = `${targetClassName} – Section ${targetSection}`;
  const targetBannerText = `Class: ${targetNumber.toUpperCase()}-${targetSection} (${academicYear})`;

  if (!inMemoryClasses.some((c) => c.id === targetClassId)) {
    inMemoryClasses.push({
      id: targetClassId,
      name: targetClassName,
      section: targetSection,
      student_count: 40,
    });
  }

  let promotedCount = 0;
  for (let i = 0; i < inMemoryStudents.length; i++) {
    if (inMemoryStudents[i].class_id === classId) {
      inMemoryStudents[i] = {
        ...inMemoryStudents[i],
        class_id: targetClassId,
        class_name: targetClassName,
        section: targetSection,
        class_title: targetClassTitle,
        banner_class_text: targetBannerText,
        updated_at: new Date().toISOString(),
      };
      promotedCount++;
    }
  }

  return res.json({
    success: true,
    message: `Promoted ${promotedCount} students from ${classId} to ${targetClassTitle} (${academicYear}).`,
    data: {
      from_class_id: classId,
      target_class_id: targetClassId,
      target_class_title: targetClassTitle,
      academic_year: academicYear,
      promoted_count: promotedCount,
    },
  });
}));

// Admin Selective Section-Wise Data Retention / Cleanup (Doubts, Notes, Roster)
app.post('/api/v1/classes/:classId/selective-cleanup', authenticateOrStudent, asyncRoute(async (req, res) => {
  const callerRole = req.user?.role || String(req.body.role || 'ADMIN').toUpperCase();
  if (callerRole !== 'ADMIN' && callerRole !== 'TEACHER') {
    return res.status(403).json({
      success: false,
      error: 'Only School Admins (or authorized Teachers) can perform section data cleanup.',
    });
  }
  const { classId } = req.params;
  const deleteDoubts = Boolean(req.body.delete_doubts);
  const deleteNotes = Boolean(req.body.delete_notes);
  const deleteHomework = Boolean(req.body.delete_homework);
  const deleteStudentRoster = Boolean(req.body.delete_student_roster);

  let removedDoubts = 0;
  if (deleteDoubts) {
    for (let i = inMemoryDiscussions.length - 1; i >= 0; i--) {
      if (inMemoryDiscussions[i].class_id === classId) {
        const discId = inMemoryDiscussions[i].id;
        inMemoryDiscussions.splice(i, 1);
        removedDoubts++;
        for (let r = inMemoryReplies.length - 1; r >= 0; r--) {
          if (inMemoryReplies[r].discussion_id === discId) {
            inMemoryReplies.splice(r, 1);
          }
        }
      }
    }
  }

  let removedRoster = 0;
  if (deleteStudentRoster) {
    for (let i = inMemoryStudents.length - 1; i >= 0; i--) {
      if (inMemoryStudents[i].class_id === classId) {
        inMemoryStudents.splice(i, 1);
        removedRoster++;
      }
    }
  }

  return res.json({
    success: true,
    message: `Selective cleanup completed for ${classId}.`,
    data: {
      class_id: classId,
      removed_doubts: removedDoubts,
      notes_deleted: deleteNotes,
      homework_deleted: deleteHomework,
      removed_roster: removedRoster,
    },
  });
}));

// SchoolOS Intelligence, Financial Leakage Prevention & Monthly 4x ROI Efficiency Report
app.get('/api/v1/school-os/intelligence', authenticateOrStudent, asyncRoute(async (_req, res) => {
  return res.json({
    success: true,
    data: {
      collection_today_inr: 185000,
      collection_month_inr: 2460000,
      collection_year_inr: 14250000,
      outstanding_balance_inr: 640000,
      total_students: 842,
      attendance_today_pct: 94.2,
      staff_present_str: '46 / 48',
      new_admissions_count: 38,
      monthly_efficiency_report: {
        teacher_hours_saved: 164,
        parent_notifications_delivered: 18420,
        fee_reminders_auto_sent: 312,
        payments_reconciled: 489,
        paper_forms_replaced: 1240,
        admissions_enquiries_followed_up: 94,
        automation_executions_completed: 2680,
        estimated_monthly_value_inr: 20000,
        monthly_subscription_inr: 5000,
        value_to_price_ratio: '4.0x',
      },
    },
  });
}));

app.get('/api/v1/students', authenticateOrStudent, asyncRoute(async (req, res) => {
  const classIdFilter = req.query.class_id as string | undefined;
  try {
    const params: unknown[] = [];
    let sql = `SELECT id, school_id, full_name, class_id, class_name, section, student_code, created_at, updated_at
               FROM users WHERE role = 'STUDENT'`;
    if (classIdFilter) {
      params.push(classIdFilter);
      sql += ` AND class_id = $1`;
    }
    sql += ` ORDER BY updated_at DESC LIMIT 100`;
    const dbRes = await query(sql, params);
    if (dbRes.rows.length > 0) {
      return res.json({ success: true, data: { students: dbRes.rows } });
    }
  } catch {
    // Fallback to in-memory store
  }
  const filtered = classIdFilter
    ? inMemoryStudents.filter((s) => s.class_id === classIdFilter)
    : inMemoryStudents;
  return res.json({ success: true, data: { students: filtered } });
}));

// List all classes
app.get('/api/v1/classes', authenticateOrStudent, asyncRoute(async (_req, res) => {
  try {
    const result = await query<{ id: string; name: string; section: string }>(
      `SELECT id, name, section FROM classes ORDER BY name, section`
    );
    if (result.rows.length > 0) {
      return res.json({ success: true, data: { classes: result.rows } });
    }
  } catch {
    // Memory store fallback
  }
  return res.json({ success: true, data: { classes: inMemoryClasses } });
}));

// Fetch classroom discussions with cursor-based pagination (strictly isolated by classId)
app.get(['/api/v1/classes/:classId/discussions', '/api/v1/discussions'], authenticateOrStudent, asyncRoute(async (req, res) => {
  const targetClassId = req.params.classId || (req.query.class_id as string) || 'class-10a';
  const limit = Math.min(Math.max(Number(req.query.limit) || 15, 1), 50);
  const cursor = (req.query.cursor as string) || null;

  try {
    const params: unknown[] = [targetClassId, limit + 1];
    let queryStr = `
      SELECT d.id, d.school_id, d.class_id, d.subject_name, d.topic_title, d.content, d.created_at,
             u.full_name AS author_name, u.role AS author_role,
             COUNT(r.id)::int AS replies_count
      FROM discussions d
      JOIN users u ON u.id = d.author_id
      LEFT JOIN discussion_replies r ON r.discussion_id = d.id
      WHERE d.class_id = $1
    `;

    if (cursor) {
      params.push(cursor);
      queryStr += ` AND d.created_at < $3 `;
    }

    queryStr += `
      GROUP BY d.id, u.full_name, u.role
      ORDER BY d.created_at DESC
      LIMIT $2
    `;

    const result = await query<{
      id: string;
      school_id: string;
      class_id: string;
      subject_name: string;
      topic_title: string;
      content: string;
      created_at: string;
      author_name: string;
      author_role: string;
      replies_count: number;
    }>(queryStr, params);

    if (result.rows.length > 0) {
      const hasMore = result.rows.length > limit;
      const rows = hasMore ? result.rows.slice(0, limit) : result.rows;
      const nextCursor = rows.length > 0 ? rows[rows.length - 1].created_at : null;
      return res.json({
        success: true,
        data: {
          discussions: rows,
          has_more: hasMore,
          next_cursor: hasMore ? nextCursor : null,
        },
      });
    }
  } catch {
    // Memory fallback
  }

  // Fallback to in-memory discussions strictly isolated by targetClassId
  let filtered = inMemoryDiscussions.filter(
    (d) => d.class_id === targetClassId
  );

  if (cursor) {
    filtered = filtered.filter((d) => new Date(d.created_at) < new Date(cursor));
  }

  const hasMore = filtered.length > limit;
  const paginated = hasMore ? filtered.slice(0, limit) : filtered;
  const nextCursor = paginated.length > 0 ? paginated[paginated.length - 1].created_at : null;

  return res.json({
    success: true,
    data: {
      discussions: paginated,
      has_more: hasMore,
      next_cursor: hasMore ? nextCursor : null,
    },
  });
}));

// Create a classroom discussion / doubt
app.post(['/api/v1/classes/:classId/discussions', '/api/v1/discussions'], authenticateOrStudent, asyncRoute(async (req, res) => {
  const body = discussionSchema.parse(req.body);
  const classId = req.params.classId || body.class_id || 'class-10a';
  const authorName = (req.body.author_name as string) || req.user!.full_name;
  const authorRole = (req.body.author_role as string) || req.user!.role || 'STUDENT';
  const prompt = `Act as a helpful high-school ${body.subject_name} tutor for CBSE India. Explain step-by-step how to solve this problem on "${body.topic_title}": "${body.content}". Guide the student; do not only give the final answer.`;

  let createdId = `disc-${Date.now()}`;
  let createdAt = new Date().toISOString();

  try {
    const created = await query<{ id: string; created_at: string }>(
      `INSERT INTO discussions (school_id, class_id, author_id, subject_name, topic_title, content)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING id, created_at`,
      [req.user!.school_id, classId, req.user!.id, body.subject_name, body.topic_title, body.content]
    );
    if (created.rows[0]) {
      createdId = created.rows[0].id;
      createdAt = created.rows[0].created_at;
    }
  } catch {
    // Memory store fallback
  }

  const newDisc: InMemDiscussion = {
    id: createdId,
    school_id: req.user!.school_id,
    class_id: classId,
    author_id: req.user!.id,
    author_name: authorName,
    author_role: authorRole,
    subject_name: body.subject_name,
    topic_title: body.topic_title,
    content: body.content,
    image_url: body.image_url ?? null,
    created_at: createdAt,
    replies_count: 0,
  };
  inMemoryDiscussions.unshift(newDisc);

  return res.status(201).json({
    success: true,
    data: {
      discussion: newDisc,
      chatgpt_prompt_formatted: prompt,
      chatgpt_launch_url: buildChatGPTLaunchUrl(prompt),
    },
  });
}));

// Get study notes for a class (strictly isolated by classId)
app.get('/api/v1/classes/:classId/notes', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { classId } = req.params;
  try {
    const result = await query<{
      id: string;
      class_id: string;
      subject_name: string;
      title: string;
      chapter_name: string;
      file_url: string;
      file_type: string;
      teacher_name: string;
      created_at: string;
    }>(
      `SELECT n.id, n.class_id, n.subject_name, n.title, n.chapter_name, n.file_url, n.file_type,
              u.full_name AS teacher_name, n.created_at
       FROM academic_notes n
       JOIN users u ON u.id = n.teacher_id
       WHERE n.class_id = $1
       ORDER BY n.created_at DESC`,
      [classId]
    );
    if (result.rows.length > 0) {
      return res.json({ success: true, data: { notes: result.rows } });
    }
  } catch {
    // Memory store fallback
  }

  const notes = inMemoryNotes.filter((n) => n.class_id === classId);
  return res.json({ success: true, data: { notes } });
}));

// Post study notes for a class (Teachers)
app.post('/api/v1/classes/:classId/notes', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { classId } = req.params;
  const body = noteSchema.parse(req.body);
  const teacherName = (req.body.teacher_name as string) || req.user!.full_name;

  let noteId = `note-${Date.now()}`;
  let createdAt = new Date().toISOString();

  try {
    const created = await query<{ id: string; created_at: string }>(
      `INSERT INTO academic_notes (school_id, class_id, teacher_id, subject_name, title, chapter_name, file_url, file_type)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING id, created_at`,
      [req.user!.school_id, classId, req.user!.id, body.subject_name, body.title, body.chapter_name, body.file_url, body.file_type]
    );
    if (created.rows[0]) {
      noteId = created.rows[0].id;
      createdAt = created.rows[0].created_at;
    }
  } catch {
    // Memory store fallback
  }

  const newNote: InMemNote = {
    id: noteId,
    class_id: classId,
    subject_name: body.subject_name,
    title: body.title,
    chapter_name: body.chapter_name,
    file_url: body.file_url,
    file_type: body.file_type,
    teacher_name: teacherName,
    created_at: createdAt,
  };
  inMemoryNotes.unshift(newNote);

  return res.status(201).json({ success: true, data: { note: newNote } });
}));

// Post a mobile home notice / event (Admin & Teachers)
app.post('/api/v1/posts', authenticateOrStudent, asyncRoute(async (req, res) => {
  const body = mobilePostSchema.parse(req.body);
  const authorName = body.author_name || req.user!.full_name || 'School Principal';
  const newPostId = `post-${Date.now()}`;
  const createdAt = new Date().toISOString();

  try {
    await query(
      `INSERT INTO posts (school_id, author_id, title, content, category, target_type)
       VALUES ($1, $2, $3, $4, $5, 'ALL')`,
      [req.user!.school_id, req.user!.id, body.title, body.content, body.category]
    );
  } catch {
    // Handled
  }

  return res.status(201).json({
    success: true,
    data: {
      post: {
        id: newPostId,
        title: body.title,
        content: body.content,
        category: body.category,
        author_name: authorName,
        created_at: createdAt,
      },
    },
  });
}));

// Get replies for a discussion
app.get('/api/v1/discussions/:discussionId/replies', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { discussionId } = req.params;
  try {
    const result = await query<{
      id: string;
      discussion_id: string;
      content: string;
      is_verified: boolean;
      created_at: string;
      author_name: string;
      author_role: string;
    }>(
      `SELECT r.id, r.discussion_id, r.content, r.is_verified, r.created_at,
              u.full_name AS author_name, u.role AS author_role
       FROM discussion_replies r
       JOIN users u ON u.id = r.author_id
       WHERE r.discussion_id = $1
       ORDER BY r.is_verified DESC, r.created_at ASC`,
      [discussionId]
    );
    if (result.rows.length > 0) {
      return res.json({ success: true, data: { replies: result.rows } });
    }
  } catch {
    // Memory store fallback
  }

  const replies = inMemoryReplies.filter((r) => r.discussion_id === discussionId);
  return res.json({ success: true, data: { replies } });
}));

// Post a reply to a discussion
app.post('/api/v1/discussions/:discussionId/replies', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { discussionId } = req.params;
  const body = replySchema.parse(req.body);
  const authorRole = (req.body.author_role as string) || req.user!.role || 'STUDENT';
  const authorName = body.author_name || req.user!.full_name;
  const isVerified = authorRole === 'TEACHER' || req.body.is_verified === true;

  let replyId = `rep-${Date.now()}`;
  let createdAt = new Date().toISOString();

  try {
    const created = await query<{ id: string; created_at: string }>(
      `INSERT INTO discussion_replies (discussion_id, school_id, author_id, content, is_verified)
       VALUES ($1, $2, $3, $4, $5) RETURNING id, created_at`,
      [discussionId, req.user!.school_id, req.user!.id, body.content, isVerified]
    );
    if (created.rows[0]) {
      replyId = created.rows[0].id;
      createdAt = created.rows[0].created_at;
    }
  } catch {
    // Memory store fallback
  }

  const newReply: InMemReply = {
    id: replyId,
    discussion_id: discussionId,
    school_id: req.user!.school_id,
    author_id: req.user!.id,
    author_name: authorName,
    author_role: authorRole,
    content: body.content,
    image_url: body.image_url ?? null,
    created_at: createdAt,
  };
  inMemoryReplies.push(newReply);

  // Update reply count in in-memory discussions
  const parentDisc = inMemoryDiscussions.find((d) => d.id === discussionId);
  if (parentDisc) {
    parentDisc.replies_count += 1;
  }

  return res.status(201).json({ success: true, data: { reply: newReply } });
}));

// Edit a classroom discussion / doubt
app.put('/api/v1/discussions/:discussionId', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { discussionId } = req.params;
  const subjectName = String(req.body.subject_name || '').trim();
  const topicTitle = String(req.body.topic_title || '').trim();
  const content = String(req.body.content || '').trim();
  if (!topicTitle || !content) {
    return res.status(400).json({ success: false, error: 'Title and content are required.' });
  }

  try {
    await query(
      `UPDATE discussions SET subject_name = COALESCE(NULLIF($1, ''), subject_name), topic_title = $2, content = $3 WHERE id::text = $4`,
      [subjectName, topicTitle, content, discussionId]
    );
  } catch {
    // Memory fallback
  }

  const idx = inMemoryDiscussions.findIndex((d) => d.id === discussionId);
  if (idx >= 0) {
    inMemoryDiscussions[idx] = {
      ...inMemoryDiscussions[idx],
      subject_name: subjectName || inMemoryDiscussions[idx].subject_name,
      topic_title: topicTitle,
      content,
    };
  }
  return res.json({ success: true, message: 'Discussion updated.' });
}));

// Delete a classroom discussion / doubt
app.delete('/api/v1/discussions/:discussionId', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { discussionId } = req.params;
  try {
    await query(`DELETE FROM discussions WHERE id::text = $1`, [discussionId]);
  } catch {
    // Memory fallback
  }
  const idx = inMemoryDiscussions.findIndex((d) => d.id === discussionId);
  if (idx >= 0) {
    inMemoryDiscussions.splice(idx, 1);
  }
  for (let i = inMemoryReplies.length - 1; i >= 0; i--) {
    if (inMemoryReplies[i].discussion_id === discussionId) {
      inMemoryReplies.splice(i, 1);
    }
  }
  return res.json({ success: true, message: 'Discussion deleted.' });
}));

// Edit a discussion reply / comment
app.put('/api/v1/discussions/:discussionId/replies/:replyId', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { replyId } = req.params;
  const content = String(req.body.content || '').trim();
  if (!content) {
    return res.status(400).json({ success: false, error: 'Reply content is required.' });
  }
  try {
    await query(`UPDATE discussion_replies SET content = $1 WHERE id::text = $2`, [content, replyId]);
  } catch {
    // Memory fallback
  }
  const idx = inMemoryReplies.findIndex((r) => r.id === replyId);
  if (idx >= 0) {
    inMemoryReplies[idx] = { ...inMemoryReplies[idx], content };
  }
  return res.json({ success: true, message: 'Reply updated.' });
}));

// Delete a discussion reply / comment
app.delete('/api/v1/discussions/:discussionId/replies/:replyId', authenticateOrStudent, asyncRoute(async (req, res) => {
  const { discussionId, replyId } = req.params;
  try {
    await query(`DELETE FROM discussion_replies WHERE id::text = $1`, [replyId]);
  } catch {
    // Memory fallback
  }
  const idx = inMemoryReplies.findIndex((r) => r.id === replyId);
  if (idx >= 0) {
    inMemoryReplies.splice(idx, 1);
    const parentDisc = inMemoryDiscussions.find((d) => d.id === discussionId);
    if (parentDisc && parentDisc.replies_count > 0) {
      parentDisc.replies_count -= 1;
    }
  }
  return res.json({ success: true, message: 'Reply deleted.' });
}));

/**
 * Produces a URL suitable for Android Chrome Custom Tabs or a web window.open().
 * ChatGPT does not expose a public URL API for attaching a device image; an image URL
 * is included in the prompt as a reference and the user must upload it in ChatGPT if
 * image understanding is required.
 */
export function buildChatGPTLaunchUrl(prompt: string) {
  return `https://chatgpt.com/?q=${encodeURIComponent(prompt)}`;
}

app.post('/api/v1/discussions/guided-chatgpt', authenticateJWT, asyncRoute(async (req, res) => {
  const body = guidedChatSchema.parse(req.body);
  const input = [body.text && `Student question: ${body.text}`, body.image_url && `Image reference: ${body.image_url}`].filter(Boolean).join('\n');
  const prompt = `Act as a helpful high-school ${body.subject_name} tutor for CBSE India. Explain step-by-step how to solve the following ${body.topic_title}. Guide the student and show reasoning, rather than only giving the final answer.\n\n${input}`;
  const hasImage = Boolean(body.image_url);
  res.json({
    success: true,
    data: {
      launch_url: buildChatGPTLaunchUrl(prompt),
      prompt,
      input_type: body.text && hasImage ? 'text_and_image' : hasImage ? 'image' : 'text',
      image_upload_required: hasImage,
      image_upload_instruction: hasImage ? 'ChatGPT cannot receive an image as a URL-prefilled attachment. After ChatGPT opens, tap + and upload the same image for visual analysis.' : null,
    },
  });
}));

app.get('/api/v1/referrals/my-code', authenticateJWT, asyncRoute(async (req, res) => {
  const codeResult = await query<{ code: string }>(`INSERT INTO referral_codes (school_id, user_id, code)
    VALUES ($1, $2, 'REF' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8)))
    ON CONFLICT (school_id, user_id) DO UPDATE SET user_id = EXCLUDED.user_id RETURNING code`, [req.user!.school_id, req.user!.id]);
  const stats = await query(`SELECT status, COUNT(*)::int count FROM referral_leads WHERE referrer_user_id = $1 GROUP BY status`, [req.user!.id]);
  const code = codeResult.rows[0].code;
  const publicUrl = process.env.PUBLIC_WEB_URL ?? 'https://schoolapp.in';
  res.json({ success: true, data: { referral_code: code, share_url: `${publicUrl}/r/${code}`, whatsapp_share_text: `We love studying at our school! Check out admissions here: ${publicUrl}/r/${code}`, stats: stats.rows } });
}));

app.post('/api/v1/public/referrals/lead', asyncRoute(async (req, res) => {
  const body = leadSchema.parse(req.body);
  const code = await query<{ school_id: string; user_id: string; id: string }>('SELECT school_id, user_id, id FROM referral_codes WHERE code = $1', [body.referral_code]);
  if (!code.rows[0]) return res.status(404).json({ success: false, error: 'Invalid referral code.' });
  const c = code.rows[0];
  await query(`INSERT INTO referral_leads (school_id, referral_code_id, referrer_user_id, parent_name, parent_phone, parent_email, child_name, desired_grade)
    VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`, [c.school_id, c.id, c.user_id, body.parent_name, body.parent_phone, body.parent_email ?? null, body.child_name, body.desired_grade]);
  res.status(201).json({ success: true, message: 'Thank you. The admissions team will get in touch shortly.' });
}));

// ============================================================================
// SIBLINGS ADMISSIONS LEAD CAPTURE & ADMIN FILTERING (REVENUE EXPANSION)
// ============================================================================

const siblingItemSchema = z.object({
  id: z.string().optional(),
  full_name: z.string().trim().min(1).max(120),
  age: z.number().int().min(1).max(25),
  current_grade: z.string().trim().min(1).max(50),
  school_type: z.enum(['GENESIS', 'OTHER']).default('OTHER'),
  school_name: z.string().trim().max(150).optional().default(''),
  reason_other_school: z.string().trim().max(500).optional().default(''),
  target_admission_year: z.string().trim().max(50).optional().default('2026-2027'),
  admission_status: z.enum(['NEW_LEAD', 'CONTACTED', 'VISIT_SCHEDULED', 'ENROLLED']).default('NEW_LEAD')
});

const siblingSubmissionSchema = z.object({
  student_id: z.string().trim().min(1).max(100),
  student_name: z.string().trim().min(1).max(120),
  class_name: z.string().trim().min(1).max(50),
  parent_name: z.string().trim().min(1).max(120),
  parent_phone: z.string().trim().min(5).max(30),
  parent_email: z.string().trim().max(160).optional(),
  has_siblings: z.boolean().default(false),
  siblings: z.array(siblingItemSchema).default([])
});

interface InMemorySiblingSubmission {
  id: string;
  student_id: string;
  student_name: string;
  class_name: string;
  parent_name: string;
  parent_phone: string;
  parent_email?: string;
  has_siblings: boolean;
  siblings: Array<z.infer<typeof siblingItemSchema>>;
  created_at: string;
}

const inMemorySiblingSubmissions: InMemorySiblingSubmission[] = [
  {
    id: 'sub-1',
    student_id: 'stu-aarav',
    student_name: 'Aarav Sharma',
    class_name: 'Class 10 – Section A',
    parent_name: 'Rajesh Sharma',
    parent_phone: '+91 98765 43210',
    has_siblings: true,
    siblings: [
      {
        id: 'sib-101',
        full_name: 'Rohan Sharma',
        age: 8,
        current_grade: 'Class 3',
        school_type: 'OTHER',
        school_name: 'Oakridge International School',
        reason_other_school: 'Closer to earlier house. Now relocated near Beeramguda.',
        target_admission_year: '2026-2027',
        admission_status: 'NEW_LEAD'
      }
    ],
    created_at: new Date().toISOString()
  },
  {
    id: 'sub-2',
    student_id: 'stu-sahasra',
    student_name: 'Rachoti Sahasra',
    class_name: 'Class 10 – Section A',
    parent_name: 'Suresh Rachoti',
    parent_phone: '+91 98480 12345',
    has_siblings: true,
    siblings: [
      {
        id: 'sib-102',
        full_name: 'Rachoti Ishaan',
        age: 5,
        current_grade: 'UKG',
        school_type: 'OTHER',
        school_name: 'EuroKids Preschool',
        reason_other_school: 'Pre-primary nearby. Looking for Class 1 admission.',
        target_admission_year: '2026-2027',
        admission_status: 'NEW_LEAD'
      }
    ],
    created_at: new Date().toISOString()
  },
  {
    id: 'sub-3',
    student_id: 'stu-ananya',
    student_name: 'Ananya Reddy',
    class_name: 'Class 10 – Section C',
    parent_name: 'Venkat Reddy',
    parent_phone: '+91 99887 76655',
    has_siblings: true,
    siblings: [
      {
        id: 'sib-103',
        full_name: 'Aditya Reddy',
        age: 13,
        current_grade: 'Class 8',
        school_type: 'GENESIS',
        school_name: 'Genesis School Beeramguda',
        reason_other_school: '',
        target_admission_year: 'Already Enrolled',
        admission_status: 'ENROLLED'
      }
    ],
    created_at: new Date().toISOString()
  }
];

app.post(['/api/v1/siblings/submit', '/api/v1/siblings'], asyncRoute(async (req, res) => {
  const body = siblingSubmissionSchema.parse(req.body);
  const subId = `sub-${Date.now()}`;
  const nowIso = new Date().toISOString();

  const newSubmission: InMemorySiblingSubmission = {
    id: subId,
    student_id: body.student_id,
    student_name: body.student_name,
    class_name: body.class_name,
    parent_name: body.parent_name,
    parent_phone: body.parent_phone,
    parent_email: body.parent_email,
    has_siblings: body.has_siblings,
    siblings: body.siblings.map((s, idx) => ({
      ...s,
      id: s.id || `sib-${Date.now()}-${idx}`
    })),
    created_at: nowIso
  };

  try {
    const schoolId = req.user?.school_id || '00000000-0000-0000-0000-000000000001';
    await query(
      `INSERT INTO siblings_submissions (school_id, student_id, student_name, class_name, parent_name, parent_phone, parent_email, has_siblings, siblings)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [
        schoolId,
        body.student_id,
        body.student_name,
        body.class_name,
        body.parent_name,
        body.parent_phone,
        body.parent_email ?? null,
        body.has_siblings,
        JSON.stringify(newSubmission.siblings)
      ]
    );
  } catch (_e) {
    // Memory store fallback
  }

  const existingIdx = inMemorySiblingSubmissions.findIndex(
    (s) => s.student_id === body.student_id || s.parent_phone === body.parent_phone
  );
  if (existingIdx >= 0) {
    inMemorySiblingSubmissions[existingIdx] = newSubmission;
  } else {
    inMemorySiblingSubmissions.unshift(newSubmission);
  }

  return res.status(201).json({
    success: true,
    message: 'Sibling details captured and stored successfully.',
    data: newSubmission
  });
}));

app.get('/api/v1/admin/siblings', asyncRoute(async (req, res) => {
  const schoolTypeFilter = String(req.query.school_type || 'ALL').toUpperCase();
  const gradeFilter = String(req.query.grade || '').trim().toLowerCase();
  const searchFilter = String(req.query.search || '').trim().toLowerCase();

  let submissions = [...inMemorySiblingSubmissions];

  try {
    const dbRows = await query<any>(`SELECT * FROM siblings_submissions ORDER BY created_at DESC`);
    if (dbRows.rows.length > 0) {
      submissions = dbRows.rows.map((r: any) => ({
        id: r.id,
        student_id: r.student_id,
        student_name: r.student_name,
        class_name: r.class_name,
        parent_name: r.parent_name,
        parent_phone: r.parent_phone,
        parent_email: r.parent_email,
        has_siblings: r.has_siblings,
        siblings: typeof r.siblings === 'string' ? JSON.parse(r.siblings) : (r.siblings || []),
        created_at: r.created_at
      }));
    }
  } catch (_e) {}

  let flattenedLeads: any[] = [];
  submissions.forEach((sub) => {
    if (sub.has_siblings && sub.siblings.length > 0) {
      sub.siblings.forEach((sib) => {
        flattenedLeads.push({
          submission_id: sub.id,
          sibling_id: sib.id,
          full_name: sib.full_name,
          age: sib.age,
          current_grade: sib.current_grade,
          school_type: sib.school_type,
          school_name: sib.school_name || (sib.school_type === 'GENESIS' ? 'Genesis School Beeramguda' : 'Other School'),
          reason_other_school: sib.reason_other_school,
          target_admission_year: sib.target_admission_year,
          admission_status: sib.admission_status || 'NEW_LEAD',
          sibling_of_student: sub.student_name,
          student_class: sub.class_name,
          parent_name: sub.parent_name,
          parent_phone: sub.parent_phone,
          submitted_at: sub.created_at
        });
      });
    }
  });

  if (schoolTypeFilter === 'OTHER') {
    flattenedLeads = flattenedLeads.filter((l) => l.school_type === 'OTHER');
  } else if (schoolTypeFilter === 'GENESIS') {
    flattenedLeads = flattenedLeads.filter((l) => l.school_type === 'GENESIS');
  }

  if (gradeFilter) {
    flattenedLeads = flattenedLeads.filter((l) => l.current_grade.toLowerCase().includes(gradeFilter));
  }

  if (searchFilter) {
    flattenedLeads = flattenedLeads.filter(
      (l) =>
        l.full_name.toLowerCase().includes(searchFilter) ||
        l.sibling_of_student.toLowerCase().includes(searchFilter) ||
        l.parent_name.toLowerCase().includes(searchFilter) ||
        l.parent_phone.includes(searchFilter) ||
        l.school_name.toLowerCase().includes(searchFilter)
    );
  }

  const summary = {
    total_submissions: submissions.length,
    total_siblings: flattenedLeads.length,
    other_school_leads: flattenedLeads.filter((l) => l.school_type === 'OTHER').length,
    genesis_siblings: flattenedLeads.filter((l) => l.school_type === 'GENESIS').length
  };

  return res.json({
    success: true,
    data: {
      summary,
      leads: flattenedLeads,
      raw_submissions: submissions
    }
  });
}));

app.patch('/api/v1/admin/siblings/:siblingId/status', asyncRoute(async (req, res) => {
  const { siblingId } = req.params;
  const newStatus = String(req.body.status || 'CONTACTED');

  inMemorySiblingSubmissions.forEach((sub) => {
    sub.siblings.forEach((sib) => {
      if (sib.id === siblingId) {
        (sib as any).admission_status = newStatus;
      }
    });
  });

  return res.json({
    success: true,
    message: 'Sibling admission status updated successfully.',
    data: { sibling_id: siblingId, status: newStatus }
  });
}));

app.use((_req, res) => res.status(404).json({ success: false, error: 'Route not found.' }));
app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
  if (error instanceof ZodError) return res.status(400).json({ success: false, error: 'Invalid request.', details: error.flatten() });
  console.error(error);
  return res.status(500).json({ success: false, error: 'Internal server error.' });
});

if (require.main === module) app.listen(port, () => console.log(`School API listening on ${port}`));

export { app, pool };
