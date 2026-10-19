import fs from 'fs/promises';
import path from 'path';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { randomUUID } from 'crypto';
import { generateSalt, hashPassword } from './crypto';

/**
 * Server-side authentication store for Movie Finder Web.
 * Primary source: shared Supabase PostgreSQL (same backend as the Android app).
 * Fallback: local JSON file database (web/.local-db/db.json) seeded with the same
 * demo identities as the Android app so login works out of the box in offline mode.
 */

export type AuthMode = 'cloud' | 'local';

export interface UserRecord {
  id: string;
  email: string;
  name: string;
  role: string;
  salt: string;
  password_hash: string;
  two_factor_enabled: boolean;
  two_factor_secret: string | null;
  recovery_codes: string;
  created_at: number;
  last_login_at: number;
  is_active: boolean;
}

export interface DeletedRecordRow {
  id: string;
  record_type: string;
  original_id: string;
  name: string;
  deleted_data_json: any;
  deleted_by_email: string;
  deleted_at: number;
  reason: string;
}

export interface RestorePointRow {
  id: string;
  name: string;
  theme: string;
  maintenance_mode: boolean;
  guest_mode_enabled: boolean;
  announcement_text: string;
  security_strictness: string;
  created_at: number;
  created_by_email: string;
}

const SUPABASE_URL = (process.env.NEXT_PUBLIC_SUPABASE_URL || '').trim();
const SUPABASE_ANON_KEY = (process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || '').trim();

const urlIsProjectUrl = /^https:\/\/[a-z0-9-]{6,}\.supabase\.(co|in)\/?$/i.test(SUPABASE_URL);
const keyIsReal = SUPABASE_ANON_KEY.startsWith('eyJ') && !SUPABASE_ANON_KEY.toLowerCase().includes('dummy');

export const authMode: AuthMode = urlIsProjectUrl && keyIsReal ? 'cloud' : 'local';

export const DEMO_PASSWORD = 'MovieFinder2026!';

let cloudClient: SupabaseClient | null = null;
function getCloud(): SupabaseClient {
  if (!cloudClient) cloudClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);
  return cloudClient;
}

let cloudSeeded = false;

/**
 * Android parity: a freshly deployed cloud database starts empty, so the demo
 * identities and a baseline restore point are inserted on first use. Upsert on
 * email keeps concurrent serverless cold starts from duplicating rows.
 */
async function ensureCloudSeeded(): Promise<void> {
  if (cloudSeeded) return;
  const cloud = getCloud();
  const { count, error } = await cloud.from('users').select('id', { count: 'exact', head: true });
  if (error) return;
  if (!count) {
    await cloud.from('users').upsert(buildDemoUsers(), { onConflict: 'email' });
  }
  const { count: pointCount, error: pointError } = await cloud
    .from('config_restore_points')
    .select('id', { count: 'exact', head: true });
  if (!pointError && !pointCount) {
    await cloud.from('config_restore_points').insert([buildBaselineRestorePoint()]);
  }
  cloudSeeded = true;
}

export function safeUser(u: UserRecord) {
  const { salt, password_hash, two_factor_secret, recovery_codes, ...safe } = u;
  return safe;
}

// ---------------------------------------------------------------------------
// Local JSON file database fallback
// ---------------------------------------------------------------------------

const LOCAL_DB_DIR = path.join(process.cwd(), '.local-db');
const LOCAL_DB_FILE = path.join(LOCAL_DB_DIR, 'db.json');

interface LocalDb {
  users: UserRecord[];
  deletedRecords: DeletedRecordRow[];
  restorePoints: RestorePointRow[];
}

const DEMO_USER_SPEC: Array<[string, string, string]> = [
  ['Administrator', 'admin@moviefinder.com', 'Administrator'],
  ['Operations Manager', 'manager@moviefinder.com', 'Manager'],
  ['Content Inputer', 'inputer@moviefinder.com', 'Content Inputer'],
  ['Standard User', 'user@moviefinder.com', 'Standard User']
];

function buildSeedUser(name: string, email: string, role: string): UserRecord {
  const salt = generateSalt();
  const now = Date.now();
  return {
    id: randomUUID(),
    email,
    name,
    role,
    salt,
    password_hash: hashPassword(DEMO_PASSWORD, salt),
    two_factor_enabled: false,
    two_factor_secret: null,
    recovery_codes: '[]',
    created_at: now,
    last_login_at: now,
    is_active: true
  };
}

function buildDemoUsers(): UserRecord[] {
  return DEMO_USER_SPEC.map(([name, email, role]) => buildSeedUser(name, email, role));
}

function buildBaselineRestorePoint(): RestorePointRow {
  return {
    id: randomUUID(),
    name: 'System Baseline Configuration',
    theme: 'Dark Obsidian',
    maintenance_mode: false,
    guest_mode_enabled: true,
    announcement_text: 'Welcome to Movie Finder Enterprise Security & Cloud Hub.',
    security_strictness: 'High',
    created_at: Date.now(),
    created_by_email: 'admin@moviefinder.com'
  };
}

async function seedLocalDb(): Promise<LocalDb> {
  const db: LocalDb = {
    users: buildDemoUsers(),
    deletedRecords: [],
    restorePoints: [buildBaselineRestorePoint()]
  };
  await writeLocalDb(db);
  return db;
}

let memoryDb: LocalDb | null = null;

async function writeLocalDb(db: LocalDb) {
  memoryDb = db;
  try {
    await fs.mkdir(LOCAL_DB_DIR, { recursive: true });
    await fs.writeFile(LOCAL_DB_FILE, JSON.stringify(db, null, 2), 'utf-8');
  } catch (err) {
    console.warn('Local DB persist skipped (read-only filesystem):', (err as Error).message);
  }
}

async function readLocalDb(): Promise<LocalDb> {
  try {
    const raw = await fs.readFile(LOCAL_DB_FILE, 'utf-8');
    const db = JSON.parse(raw) as LocalDb;
    let changed = false;
    if (!Array.isArray(db.users) || db.users.length === 0) {
      db.users = buildDemoUsers();
      changed = true;
    }
    if (!Array.isArray(db.deletedRecords)) { db.deletedRecords = []; changed = true; }
    if (!Array.isArray(db.restorePoints) || db.restorePoints.length === 0) {
      db.restorePoints = [buildBaselineRestorePoint()];
      changed = true;
    }
    if (changed) await writeLocalDb(db);
    return db;
  } catch {
    if (memoryDb) return memoryDb;
    return seedLocalDb();
  }
}

// ---------------------------------------------------------------------------
// Public store API (cloud primary, local fallback)
// ---------------------------------------------------------------------------

export async function listUsers(): Promise<UserRecord[]> {
  if (authMode === 'cloud') {
    await ensureCloudSeeded();
    const { data, error } = await getCloud().from('users').select('*').order('created_at', { ascending: false });
    if (!error && data) return data as UserRecord[];
    console.warn('Supabase users read failed, using local store:', error?.message);
  }
  const db = await readLocalDb();
  return [...db.users].sort((a, b) => b.created_at - a.created_at);
}

export async function findUserByEmail(email: string): Promise<UserRecord | null> {
  const needle = email.trim().toLowerCase();
  if (authMode === 'cloud') {
    await ensureCloudSeeded();
    const { data, error } = await getCloud().from('users').select('*').eq('email', needle).maybeSingle();
    if (!error && data) return data as UserRecord;
  }
  const db = await readLocalDb();
  return db.users.find(u => u.email.toLowerCase() === needle) || null;
}

export async function findUserById(id: string): Promise<UserRecord | null> {
  if (authMode === 'cloud') {
    const { data, error } = await getCloud().from('users').select('*').eq('id', id).maybeSingle();
    if (!error && data) return data as UserRecord;
  }
  const db = await readLocalDb();
  return db.users.find(u => u.id === id) || null;
}

export async function insertUser(user: UserRecord): Promise<UserRecord> {
  if (authMode === 'cloud') {
    const { error } = await getCloud().from('users').insert([user]);
    if (!error) return user;
    console.warn('Supabase user insert failed, using local store:', error?.message);
  }
  const db = await readLocalDb();
  db.users.push(user);
  await writeLocalDb(db);
  return user;
}

export async function updateUserFields(id: string, patch: Partial<UserRecord>): Promise<UserRecord | null> {
  if (authMode === 'cloud') {
    const { data, error } = await getCloud().from('users').update(patch).eq('id', id).select().maybeSingle();
    if (!error && data) return data as UserRecord;
    console.warn('Supabase user update failed, using local store:', error?.message);
  }
  const db = await readLocalDb();
  const idx = db.users.findIndex(u => u.id === id);
  if (idx === -1) return null;
  db.users[idx] = { ...db.users[idx], ...patch };
  await writeLocalDb(db);
  return db.users[idx];
}

export async function deleteUserRecord(id: string): Promise<boolean> {
  if (authMode === 'cloud') {
    const { error } = await getCloud().from('users').delete().eq('id', id);
    if (!error) return true;
    console.warn('Supabase user delete failed, using local store:', error?.message);
  }
  const db = await readLocalDb();
  const before = db.users.length;
  db.users = db.users.filter(u => u.id !== id);
  await writeLocalDb(db);
  return db.users.length < before;
}

export async function listDeletedRecords(): Promise<DeletedRecordRow[]> {
  if (authMode === 'cloud') {
    const { data, error } = await getCloud().from('deleted_records_archive').select('*').order('deleted_at', { ascending: false });
    if (!error && data) return data as DeletedRecordRow[];
  }
  const db = await readLocalDb();
  return [...db.deletedRecords].sort((a, b) => b.deleted_at - a.deleted_at);
}

export async function insertDeletedRecord(rec: DeletedRecordRow): Promise<DeletedRecordRow> {
  if (authMode === 'cloud') {
    const { error } = await getCloud().from('deleted_records_archive').insert([rec]);
    if (!error) return rec;
    console.warn('Supabase archive insert failed, using local store:', error?.message);
  }
  const db = await readLocalDb();
  db.deletedRecords.push(rec);
  await writeLocalDb(db);
  return rec;
}

export async function listRestorePoints(): Promise<RestorePointRow[]> {
  if (authMode === 'cloud') {
    const { data, error } = await getCloud().from('config_restore_points').select('*').order('created_at', { ascending: false });
    if (!error && data) return data as RestorePointRow[];
  }
  const db = await readLocalDb();
  return [...db.restorePoints].sort((a, b) => b.created_at - a.created_at);
}

export async function insertRestorePoint(p: RestorePointRow): Promise<RestorePointRow> {
  if (authMode === 'cloud') {
    const { error } = await getCloud().from('config_restore_points').insert([p]);
    if (!error) return p;
    console.warn('Supabase restore point insert failed, using local store:', error?.message);
  }
  const db = await readLocalDb();
  db.restorePoints.push(p);
  await writeLocalDb(db);
  return p;
}

export function buildUserRecord(input: {
  name: string;
  email: string;
  password: string;
  role?: string;
  two_factor_enabled?: boolean;
  two_factor_secret?: string | null;
  recovery_codes?: string;
}): UserRecord {
  const salt = generateSalt();
  const now = Date.now();
  return {
    id: randomUUID(),
    email: input.email.trim(),
    name: input.name.trim(),
    role: input.role || 'Standard User',
    salt,
    password_hash: hashPassword(input.password, salt),
    two_factor_enabled: input.two_factor_enabled ?? false,
    two_factor_secret: input.two_factor_secret ?? null,
    recovery_codes: input.recovery_codes ?? '[]',
    created_at: now,
    last_login_at: now,
    is_active: true
  };
}
