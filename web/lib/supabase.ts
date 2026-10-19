import { createClient } from '@supabase/supabase-js';

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL || 'https://aws-0-eu-west-1.pooler.supabase.com';
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key_for_offline_mode';

/**
 * Shared Supabase PostgreSQL client connected to AWS PostgreSQL database.
 * Matches schema used by Movie Finder Android native application.
 */
export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export interface MovieMatch {
  id?: number;
  title: string;
  year: string;
  director: string;
  imdb_url: string;
  confidence: string;
  rationale: string;
  actors: string;
  genre: string;
  search_type: string;
  media_uri?: string;
  text_query?: string;
  thematic_elements?: string;
  is_in_watchlist: boolean;
  watchlist_status: string;
  watchlist_notes: string;
  poster_path: string;
  created_at: number;
}

export interface AppUser {
  id: string;
  email: string;
  name: string;
  role: 'Administrator' | 'Manager' | 'Content Inputer' | 'Standard User';
  salt: string;
  password_hash: string;
  two_factor_enabled: boolean;
  two_factor_secret?: string;
  recovery_codes: string;
  created_at: number;
  last_login_at: number;
  is_active: boolean;
}

export interface DeletedRecord {
  id: string;
  record_type: 'USER' | 'MOVIE' | 'CONFIG';
  original_id: string;
  name: string;
  deleted_data_json: any;
  deleted_by_email: string;
  deleted_at: number;
  reason: string;
}

export interface ConfigRestorePoint {
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
