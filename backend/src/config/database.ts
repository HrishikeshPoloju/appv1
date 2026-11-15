import dotenv from 'dotenv';
import { Pool, QueryResultRow } from 'pg';

dotenv.config();

export const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  host: process.env.DATABASE_URL ? undefined : process.env.DB_HOST ?? 'localhost',
  port: Number(process.env.DB_PORT ?? 5432),
  user: process.env.DATABASE_URL ? undefined : process.env.DB_USER ?? 'postgres',
  password: process.env.DATABASE_URL ? undefined : process.env.DB_PASSWORD ?? 'postgres',
  database: process.env.DATABASE_URL ? undefined : process.env.DB_NAME ?? 'school_app_db',
  ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : undefined,
  max: Number(process.env.DB_POOL_SIZE ?? 20),
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 5_000,
});

export function query<T extends QueryResultRow = QueryResultRow>(text: string, params: unknown[] = []) {
  return pool.query<T>(text, params);
}
