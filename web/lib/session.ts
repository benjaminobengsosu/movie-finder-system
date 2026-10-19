export interface SessionUser {
  id: string;
  email: string;
  name: string;
  role: string;
  two_factor_enabled: boolean;
  created_at: number;
  last_login_at: number;
  is_active: boolean;
  isGuest?: boolean;
  isCreatorAdmin?: boolean;
}

const SESSION_KEY = 'mf_web_session';

export function getSession(): SessionUser | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as SessionUser) : null;
  } catch {
    return null;
  }
}

export function setSession(user: SessionUser) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

export function isAdminRole(role?: string) {
  return role === 'Administrator';
}

export function isGuestSession(user: SessionUser | null) {
  return !!user?.isGuest;
}
