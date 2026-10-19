# Supabase Cloud Architecture & Enterprise Security System for Movie Finder

This implementation plan establishes a unified authentication and data persistence architecture powered by Supabase PostgreSQL, enabling real-time synchronization between the Android application and a shared web frontend. It introduces cryptographic password salting and peppering (PBKDF2/HMAC-SHA256), two-factor authentication (RFC 6238 TOTP + emergency backup codes), four distinct user roles with strict permission gates, an administrative user management panel with a dedicated deleted-records archive, configuration restore points, live system telemetry, and a comprehensive security credentials audit report.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The plan unifies local Room persistence with cloud Supabase PostgreSQL storage through a repository synchronization pattern. If network connectivity is offline or Supabase credentials are in initial setup, the app gracefully operates using local Room storage with zero disruption to the Gemini AI movie recognition pipeline.

- **Confirmed Decision 1: Authentication & Cryptography Architecture**: Passwords will be secured using PBKDF2 with HMAC-SHA256. Every user account receives a unique 16-byte cryptographically random salt (`SecureRandom`) stored in the user record, combined with a secret server/app pepper (`AUTH_PASSWORD_PEPPER`) applied prior to key derivation.
- **Confirmed Decision 2: Two-Step Verification (2FA)**: Standard RFC 6238 TOTP (compatible with Google Authenticator, Authy, and Microsoft Authenticator) paired with 5 single-use emergency recovery codes (`RECV-XXXX-XXXX`) generated during 2FA onboarding.
- **Confirmed Decision 3: Four User Roles**:
  1. `Administrator`: Full system access, add/delete users, trigger configuration restore points, review deleted data archives, inspect credentials report.
  2. `Manager`: Operational administration, remote config announcements, analytics, support tickets, view-only reports.
  3. `Content Inputer / Analyst`: Movie scene curation, manual scene tagging, test scans, review submission.
  4. `Standard User`: Core movie scene recognition (CameraX & photo upload), personal watchlists, profile management.
- **Confirmed Decision 4: Cross-Platform Web Compatibility**: We will supply a complete, production-ready `supabase_schema.sql` script containing all tables, constraints, Row Level Security (RLS) policies, and four seeded role accounts so your web app and mobile app share the exact same PostgreSQL backend seamlessly.

---

## 1. Overview & Core Concept

### What It Delivers
A unified security and cloud-connected hub for Movie Finder that satisfies all academic and enterprise security requirements while connecting the native Android application and a shared web application via Supabase.

### Target Personas & Roles
- **System Administrator (`admin@moviefinder.com`)**: Controls user access, monitors deleted records, inspects audit logs, and creates configuration restore points.
- **Operations Manager (`manager@moviefinder.com`)**: Oversees app health, reviews curated movie lists, and inspects analytics.
- **Content Inputer (`inputer@moviefinder.com`)**: Validates AI movie identification and curates cinema tags.
- **Standard User (`user@moviefinder.com`)**: Discovers movies using CameraX and image uploads, maintains a personal watchlist.

### Key Value
Cross-platform synchronization over Supabase REST, multi-tiered security demonstrating salting and peppering, two-step verification, deleted data preservation, and non-destructive configuration restore points.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[ Sign In Screen ] ──(Email + Password)──► [ Verify Salt & Pepper ]
                                                    │
                                             (2FA Enabled?)
                                             ├── YES ──► [ TOTP / Backup Code Verification ] ──► [ Logged In ]
                                             └── NO ───► [ Logged In ]
                                                                │
                                            ┌───────────────────┴───────────────────┐
                                            ▼                                       ▼
                                   [ Standard User UI ]                   [ Admin Security Hub ]
                                   • Live Camera / Photo Finder           • System State & Date/Theme
                                   • Personal Watchlist                   • Add / Delete Users
                                   • Account 2FA Settings                 • Deleted Records Database
                                                                          • Config Restore Points
                                                                          • Credentials Report Tab
```

### Visual Identity & Theme Tokens
- **Design Stance**: Obsidian-black luxury dark theme with high-contrast electric-cyan and neon-mint accents, clean line icons, and glassy layered cards.
- **Theme Support**: Real-time Theme selector (Dark Obsidian, Midnight Indigo, High Contrast Cinema, Light Canvas) that updates live and participates in configuration restore points.
- **Touch Targets & Polish**: Minimum 48.dp touch targets on all role chips, 2FA toggles, and user deletion buttons; smooth staggered animations for modal sheets.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Hybrid Supabase Cloud + Local Room Architecture
- **Chosen Approach**: Repository Pattern with Supabase REST client as primary cloud backend and Room SQLite as local offline cache.
- **Why**: Allows instant, responsive offline use while synchronizing automatically with the cloud Supabase database whenever connected, ensuring the web app sees the identical state.

### Decision 2: Cryptographic Salting & Peppering Pipeline
- **Chosen Approach**: PBKDF2 with HMAC-SHA256, 10,000 iterations, 16-byte random salt per user, and `AUTH_PASSWORD_PEPPER` from app config.
- **Why**: Demonstrates best-practice defense against dictionary and rainbow-table attacks. If the database is compromised, passwords cannot be cracked without the out-of-database pepper.

### Decision 3: Configuration-Only Restore Points
- **Chosen Approach**: Restore points serialize and restore `SystemConfiguration` (active theme, maintenance mode, security strictness, guest access, announcements) without touching the `users` or `movie_matches` tables.
- **Why**: Strictly adheres to the requirement that restoring a configuration snapshot never reverts or erases user records or cinema discoveries.

### Decision 4: Dedicated Deleted Data Database / Archive
- **Chosen Approach**: When an Admin deletes a user, the entity is removed from the active `users` table and inserted into the `deleted_records_archive` table with timestamp, admin ID, and reason.
- **Why**: Provides a permanent audit trail, forensic review, and compliance verification.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                         MOVIE FINDER APPLICATION                       │
│                                                                        │
│   ┌────────────────────┐   ┌───────────────────┐   ┌───────────────┐   │
│   │   User Interface   │   │  Security Hub UI  │   │  CameraX & AI │   │
│   │  (MovieFinderApp)  │   │ (AdminComponents) │   │ (Gemini/TMDB) │   │
│   └─────────┬──────────┘   └─────────┬─────────┘   └───────┬───────┘   │
│             │                        │                     │           │
│             ▼                        ▼                     ▼           │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │               MovieViewModel & SecurityCoordinator             │   │
│   │  • AuthManager (Salt + Pepper + Hash)                          │   │
│   │  • TwoFactorAuthManager (TOTP + Backup Codes)                  │   │
│   │  • RoleManager (Admin, Manager, Inputer, User)                 │   │
│   │  • RestorePointManager (Configuration Snapshots)               │   │
│   └────────────────────────────────┬───────────────────────────────┘   │
│                                    │                                   │
│                                    ▼                                   │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                        Data Repository                         │   │
│   │       (Syncs Cloud Supabase & Local Room Database)             │   │
│   └───────────────┬────────────────────────────────┬───────────────┘   │
└───────────────────┼────────────────────────────────┼───────────────────┘
                    │                                │
                    ▼                                ▼
┌─────────────────────────────────────┐   ┌──────────────────────────────┐
│     LOCAL STORAGE (Room SQLite)     │   │     CLOUD BACKEND (Supabase) │
│  • movie_matches                    │   │  • PostgreSQL Database (AWS) │
│  • app_users                        │   │  • Shared with Web App       │
│  • deleted_records_archive          │   │  • Automated Daily Backups   │
│  • config_restore_points            │   │  • RLS Policies              │
└─────────────────────────────────────┘   └──────────────────────────────┘
```

### Data Models

1. **`AppUser` Entity**:
   - `id`: String (UUID)
   - `email`: String (Unique)
   - `name`: String
   - `role`: String (`Administrator`, `Manager`, `Content Inputer`, `Standard User`)
   - `salt`: String (Hex-encoded 16-byte random salt)
   - `passwordHash`: String (PBKDF2 HMAC-SHA256 derived key)
   - `twoFactorEnabled`: Boolean
   - `twoFactorSecret`: String (Base32 encoded TOTP key)
   - `recoveryCodes`: String (JSON array of 5 hashed single-use codes)
   - `createdAt`: Long
   - `lastLoginAt`: Long

2. **`DeletedRecord` Entity (Recycle / Audit Database)**:
   - `id`: String (UUID)
   - `recordType`: String (`USER`, `MOVIE`, `CONFIG`)
   - `originalId`: String
   - `deletedDataJson`: String (Full snapshot of the deleted record)
   - `deletedByEmail`: String
   - `deletedAt`: Long
   - `reason`: String

3. **`ConfigRestorePoint` Entity**:
   - `id`: String (UUID)
   - `name`: String
   - `theme`: String
   - `maintenanceMode`: Boolean
   - `guestModeEnabled`: Boolean
   - `announcementText`: String
   - `securityStrictness`: String
   - `createdAt`: Long

---

## 5. Verification Plan

1. **Compilation Check**: Run `compile_applet` to confirm clean compilation with zero Kotlin/Gradle errors.
2. **Automated Unit Tests**: Execute `gradle :app:testDebugUnitTest` to verify cryptographic salting, peppering, TOTP validation, role permission checks, and Room database migrations.
3. **Interactive CUJ Verification**:
   - Sign in as Admin (`admin@moviefinder.com`), Manager, Content Inputer, and Standard User to confirm role restrictions.
   - Test password verification with salt and pepper logic.
   - Test 2FA setup, TOTP validation, and backup recovery code usage.
   - Add a user, delete a user, and confirm the deleted user appears in the Deleted Records Archive.
   - Create and apply a configuration restore point and verify user and movie data remain unchanged.
   - Inspect the Credentials Report tab to verify display of usernames, roles, salts, and password verification status.
