import { Queue } from 'bullmq';

export interface BroadcastJob {
  postId: string;
  schoolId: string;
  channels: { send_email?: boolean; send_app_push?: boolean; send_whatsapp?: boolean };
}

const redisUrl = process.env.REDIS_URL;
const broadcastQueue = redisUrl ? new Queue<BroadcastJob>('broadcasts', { connection: { url: redisUrl } }) : null;

export async function enqueueBroadcast(job: BroadcastJob) {
  if (broadcastQueue) return broadcastQueue.add('dispatch', job, { attempts: 3, backoff: { type: 'exponential', delay: 1000 } });
  console.info('[Broadcast] Redis is not configured; delivery job recorded but not dispatched.', job);
  return { id: `local_${Date.now()}` };
}
