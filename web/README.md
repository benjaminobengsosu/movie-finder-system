# Movie Finder Web Application (Next.js & Supabase)

A unified web companion to the Movie Finder Android native application. Shares the exact same Supabase PostgreSQL backend, cryptographic salting and peppering pipeline, two-factor authentication, four user role tiers, and Gemini 3.5 Flash visual movie scene recognition.

---

## 🚀 Quick Start Guide

### 1. Install Dependencies
Navigate into the `web` directory and install packages:
```bash
cd web
npm install
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env.local`:
```bash
cp .env.example .env.local
```
Fill in your API keys:
- `GEMINI_API_KEY`: Your Google AI Studio API Key.
- `NEXT_PUBLIC_SUPABASE_URL`: Your Supabase Project URL (e.g. `https://xxxx.supabase.co`).
- `NEXT_PUBLIC_SUPABASE_ANON_KEY`: Your Supabase Public Anon Key.
- `TMDB_API_KEY`: (Optional) The Movie Database API key for posters.

### 3. Initialize Shared Database
Ensure you ran the `supabase_schema.sql` script located in the root project directory in your Supabase SQL Editor.

### 4. Run Locally
```bash
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## ☁️ Deploying to Vercel or AWS Amplify

### Deploy to Vercel (Recommended, 1-Click)
1. Push this project to GitHub.
2. Go to [vercel.com](https://vercel.com) and import the repository.
3. Set the **Root Directory** to `web`.
4. Add your Environment Variables (`GEMINI_API_KEY`, `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`).
5. Click **Deploy**.

### Deploy to AWS Amplify
1. Open the **AWS Amplify Console**.
2. Connect your Git repository, choose the `web` branch/folder.
3. Add the build settings for Next.js SSR.
4. Save and deploy.

---

## 🔒 Security Architecture
- **Password Salting & Pepper:** Uses PBKDF2 HMAC-SHA256 with 10,000 iterations and a secret application pepper (`AUTH_PASSWORD_PEPPER`).
- **2FA (Two-Step Verification):** Standard RFC 6238 TOTP compatible with Google Authenticator, Authy, and Microsoft Authenticator + emergency recovery codes.
- **Role-Based Access Control:** 4 roles (`Administrator`, `Manager`, `Content Inputer`, `Standard User`).
- **Deleted Records Archive:** Any deleted user or movie is archived to the `deleted_records_archive` table with timestamp and audit reason.
