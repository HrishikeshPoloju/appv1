import type { JwtPayload } from 'jsonwebtoken';

declare global {
  namespace Express {
    interface Request {
      user?: JwtPayload & { id: string; school_id: string; role: string; full_name: string };
    }
  }
}

export {};
