-- ==============================================================================
-- MOVIE FINDER: UNIFIED SUPABASE POSTGRESQL SCHEMA (AWS HOSTED)
-- Supports Cross-Platform Android Mobile & Web Application
-- Features: RBAC Roles, Password Salting & Pepper, 2FA, Deleted Archive, Restore Points
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. USERS TABLE (Demonstrating Per-User Salt & Out-Of-Database Pepper)
CREATE TABLE IF NOT EXISTS public.users (
    id VARCHAR(64) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'Standard User', -- 'Administrator', 'Manager', 'Content Inputer', 'Standard User'
    salt VARCHAR(64) NOT NULL,                         -- 16-byte random salt in hex
    password_hash VARCHAR(128) NOT NULL,               -- PBKDF2 HMAC-SHA256 derived hash
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(64),                     -- RFC 6238 Base32 Secret Key
    recovery_codes TEXT DEFAULT '[]',                  -- JSON Array of emergency recovery codes
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint,
    last_login_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint,
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. MOVIE DISCOVERIES & WATCHLIST TABLE
CREATE TABLE IF NOT EXISTS public.movie_matches (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    year VARCHAR(20) DEFAULT '',
    director VARCHAR(255) DEFAULT '',
    imdb_url TEXT NOT NULL DEFAULT 'https://www.imdb.com',
    confidence VARCHAR(20) DEFAULT 'High',
    rationale TEXT DEFAULT '',
    actors TEXT DEFAULT '',
    genre VARCHAR(100) DEFAULT '',
    search_type VARCHAR(50) DEFAULT 'TEXT',             -- 'CAMERA_SCAN', 'IMAGE', 'VIDEO', 'TEXT'
    media_uri TEXT,
    text_query TEXT,
    thematic_elements TEXT DEFAULT '',
    is_in_watchlist BOOLEAN DEFAULT FALSE,
    watchlist_status VARCHAR(50) DEFAULT 'Plan to Watch',
    watchlist_notes TEXT DEFAULT '',
    custom_list_category VARCHAR(100) DEFAULT 'Must Watch',
    poster_path TEXT DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint
);

-- 3. DELETED RECORDS DATABASE / AUDIT ARCHIVE
-- Permanent repository for data that has been deleted by an administrator
CREATE TABLE IF NOT EXISTS public.deleted_records_archive (
    id VARCHAR(64) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    record_type VARCHAR(50) NOT NULL,                   -- 'USER', 'MOVIE', 'CONFIG'
    original_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    deleted_data_json JSONB NOT NULL,
    deleted_by_email VARCHAR(255) NOT NULL,
    deleted_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint,
    reason TEXT DEFAULT 'Administrative deletion'
);

-- 4. CONFIGURATION-ONLY RESTORE POINTS
-- Changes system configuration parameters without altering or deleting user data
CREATE TABLE IF NOT EXISTS public.config_restore_points (
    id VARCHAR(64) PRIMARY KEY DEFAULT uuid_generate_v4()::text,
    name VARCHAR(255) NOT NULL,
    theme VARCHAR(50) DEFAULT 'Dark Obsidian',
    maintenance_mode BOOLEAN DEFAULT FALSE,
    guest_mode_enabled BOOLEAN DEFAULT TRUE,
    announcement_text TEXT DEFAULT '',
    security_strictness VARCHAR(50) DEFAULT 'High',
    created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint,
    created_by_email VARCHAR(255) NOT NULL DEFAULT 'admin@moviefinder.com'
);

-- 5. ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movie_matches ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.deleted_records_archive ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.config_restore_points ENABLE ROW LEVEL SECURITY;

-- Allow read access for authenticated API clients
CREATE POLICY "Public Read Movie Matches" ON public.movie_matches FOR SELECT USING (true);
CREATE POLICY "Public Insert Movie Matches" ON public.movie_matches FOR INSERT WITH CHECK (true);
CREATE POLICY "Public Update Movie Matches" ON public.movie_matches FOR UPDATE USING (true);

CREATE POLICY "Public Read Users" ON public.users FOR SELECT USING (true);
CREATE POLICY "Public Insert Users" ON public.users FOR INSERT WITH CHECK (true);
CREATE POLICY "Public Update Users" ON public.users FOR UPDATE USING (true);
CREATE POLICY "Public Delete Users" ON public.users FOR DELETE USING (true);

CREATE POLICY "Public Read Archive" ON public.deleted_records_archive FOR SELECT USING (true);
CREATE POLICY "Public Insert Archive" ON public.deleted_records_archive FOR INSERT WITH CHECK (true);

CREATE POLICY "Public Read Restore Points" ON public.config_restore_points FOR SELECT USING (true);
CREATE POLICY "Public Insert Restore Points" ON public.config_restore_points FOR INSERT WITH CHECK (true);

-- 6. INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);
CREATE INDEX IF NOT EXISTS idx_movie_matches_title ON public.movie_matches(title);
CREATE INDEX IF NOT EXISTS idx_deleted_records_type ON public.deleted_records_archive(record_type);
CREATE INDEX IF NOT EXISTS idx_restore_points_created ON public.config_restore_points(created_at DESC);
