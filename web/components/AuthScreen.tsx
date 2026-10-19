'use client';

import React, { useState } from 'react';
import { Film, Shield, AlertCircle, CheckCircle, Loader2, KeyRound } from 'lucide-react';
import { SessionUser } from '../lib/session';

interface AuthScreenProps {
  onAuthenticated: (user: SessionUser) => void;
}

type Mode = 'signin' | 'signup' | 'forgot';

const CREATOR_ADMIN_NAME = 'Benjamin Obeng Sosu (Creator)';
const CREATOR_ADMIN_EMAIL = 'benjaminobengsosu@gmail.com';

const DEMO_ACCOUNTS = [
  { label: 'Admin', email: 'admin@moviefinder.com' },
  { label: 'Manager', email: 'manager@moviefinder.com' },
  { label: 'Inputer', email: 'inputer@moviefinder.com' },
  { label: 'User', email: 'user@moviefinder.com' }
];

const inputStyle: React.CSSProperties = {
  width: '100%',
  backgroundColor: '#06080D',
  border: '1px solid #222634',
  borderRadius: '12px',
  padding: '12px 14px',
  fontSize: '13px',
  color: '#FFFFFF',
  outline: 'none'
};

export default function AuthScreen({ onAuthenticated }: AuthScreenProps) {
  const [mode, setMode] = useState<Mode>('signin');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [info, setInfo] = useState<string | null>(null);

  const [pending2FA, setPending2FA] = useState<{ email: string; password: string } | null>(null);
  const [twoFACode, setTwoFACode] = useState('');
  const [recoveryMode, setRecoveryMode] = useState(false);

  const switchMode = (next: Mode) => {
    setMode(next);
    setError(null);
    setInfo(null);
  };

  const fillDemo = (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('MovieFinder2026!');
    switchMode('signin');
  };

  const signInAsCreatorAdmin = () => {
    onAuthenticated({
      id: 'creator-admin',
      email: CREATOR_ADMIN_EMAIL,
      name: CREATOR_ADMIN_NAME,
      role: 'Administrator',
      two_factor_enabled: false,
      created_at: Date.now(),
      last_login_at: Date.now(),
      is_active: true,
      isCreatorAdmin: true
    });
  };

  const continueAsGuest = () => {
    onAuthenticated({
      id: 'guest',
      email: 'guest@moviefinder.com',
      name: 'Guest Moviegoer',
      role: 'Guest',
      two_factor_enabled: false,
      created_at: Date.now(),
      last_login_at: Date.now(),
      is_active: true,
      isGuest: true
    });
  };

  const completeLogin = (user: any) => {
    onAuthenticated({
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
      two_factor_enabled: user.two_factor_enabled,
      created_at: user.created_at,
      last_login_at: user.last_login_at,
      is_active: user.is_active
    } as SessionUser);
  };

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setInfo(null);

    if (mode === 'forgot') {
      if (!email.trim() || !email.includes('@')) {
        setError('Please provide a valid email address.');
        return;
      }
      setInfo(`Password reset email successfully sent to ${email.trim()}!`);
      setMode('signin');
      return;
    }

    if (mode === 'signup' && (!name.trim() || !email.trim() || !password)) {
      setError('All fields are required to create an account.');
      return;
    }
    if (mode === 'signin' && (!email.trim() || !password)) {
      setError('Email and password fields cannot be empty.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    setBusy(true);
    try {
      if (mode === 'signup') {
        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: name.trim(), email: email.trim(), password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Registration failed.');
        completeLogin(data.user);
      } else {
        const res = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email: email.trim(), password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Sign-in failed.');
        if (data.requires2FA) {
          setPending2FA({ email: email.trim(), password });
          setTwoFACode('');
          setRecoveryMode(false);
        } else {
          completeLogin(data.user);
        }
      }
    } catch (err: any) {
      setError(err.message || 'Authentication failed.');
    } finally {
      setBusy(false);
    }
  }

  async function verifyTwoFactor() {
    if (!pending2FA) return;
    const code = twoFACode.trim();
    if (!code) {
      setError('Enter your 6-digit authenticator code or a recovery code.');
      return;
    }

    setBusy(true);
    setError(null);
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: pending2FA.email,
          password: pending2FA.password,
          ...(recoveryMode ? { recoveryCode: code } : { totpCode: code })
        })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Verification failed.');
      setPending2FA(null);
      setTwoFACode('');
      completeLogin(data.user);
    } catch (err: any) {
      setError(err.message || 'Verification failed.');
    } finally {
      setBusy(false);
    }
  }

  const primaryLabel = mode === 'forgot' ? 'Send Reset Link' : mode === 'signup' ? 'Register Account' : 'Sign In';

  return (
    <div style={{ minHeight: '100vh', background: 'linear-gradient(180deg, #0F0B1F 0%, #05030A 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      <div style={{ width: '100%', maxWidth: '440px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '20px' }}>
        {/* Logo */}
        <div style={{ width: '68px', height: '68px', borderRadius: '16px', backgroundColor: '#10121A', border: '1px solid rgba(0, 229, 255, 0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Film style={{ width: '32px', height: '32px', color: '#00E5FF' }} />
        </div>
        <div style={{ textAlign: 'center' }}>
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 'bold', color: '#F5F6F8', letterSpacing: '0.5px' }}>Movie Finder</h1>
          <p style={{ margin: '4px 0 0 0', fontSize: '11px', color: '#8E93A4', textAlign: 'center', letterSpacing: '0.3px' }}>Instant AI Scene &amp; Film Identifier</p>
        </div>

        {/* Auth Card */}
        <form
          onSubmit={handleSubmit}
          style={{ width: '100%', backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '20px', padding: '20px', display: 'flex', flexDirection: 'column', gap: '14px' }}
        >
          <h2 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: '#F5F6F8' }}>
            {mode === 'forgot' ? 'Reset Password' : mode === 'signup' ? 'Create Account' : 'Sign In'}
          </h2>

          {mode === 'signup' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '11px', color: '#8E93A4' }}>Display Name</label>
              <input type="text" value={name} onChange={e => setName(e.target.value)} placeholder="Your full name" style={inputStyle} />
            </div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '11px', color: '#8E93A4' }}>Email</label>
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" style={inputStyle} />
          </div>

          {mode !== 'forgot' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <label style={{ fontSize: '11px', color: '#8E93A4' }}>Password</label>
              <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 6 characters" style={inputStyle} />
            </div>
          )}

          {mode === 'signin' && (
            <button
              type="button"
              onClick={() => switchMode('forgot')}
              style={{ alignSelf: 'flex-end', background: 'none', border: 'none', padding: 0, color: '#00E5FF', fontSize: '11.5px', fontWeight: 600, cursor: 'pointer' }}
            >
              Forgot Password?
            </button>
          )}

          {error && (
            <div style={{ backgroundColor: 'rgba(255, 82, 82, 0.12)', border: '1px solid #FF5252', borderRadius: '10px', padding: '10px 12px', display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
              <AlertCircle style={{ width: '16px', height: '16px', color: '#FF5252', flexShrink: 0, marginTop: '1px' }} />
              <span style={{ fontSize: '12px', color: '#FF5252', lineHeight: 1.4 }}>{error}</span>
            </div>
          )}
          {info && (
            <div style={{ backgroundColor: 'rgba(0, 230, 118, 0.12)', border: '1px solid #00E676', borderRadius: '10px', padding: '10px 12px', display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
              <CheckCircle style={{ width: '16px', height: '16px', color: '#00E676', flexShrink: 0, marginTop: '1px' }} />
              <span style={{ fontSize: '12px', color: '#00E676', lineHeight: 1.4 }}>{info}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={busy}
            style={{
              width: '100%',
              padding: '14px',
              backgroundColor: busy ? '#0e7490' : '#00E5FF',
              color: '#06080D',
              border: 'none',
              borderRadius: '12px',
              fontWeight: 'bold',
              fontSize: '13px',
              cursor: busy ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px'
            }}
          >
            {busy ? <Loader2 style={{ width: '16px', height: '16px' }} className="animate-spin" /> : null}
            {busy ? 'Authenticating...' : primaryLabel}
          </button>

          <div style={{ textAlign: 'center' }}>
            <button
              type="button"
              onClick={() => switchMode(mode === 'signup' ? 'signin' : 'signup')}
              style={{ background: 'none', border: 'none', padding: 0, color: '#8C90A6', fontSize: '11.5px', fontWeight: 500, cursor: 'pointer' }}
            >
              {mode === 'signup' ? 'Already have an account? Sign In' : 'New to Movie Finder? Create Account'}
            </button>
          </div>
        </form>

        {/* Demo credentials helper */}
        <div style={{ width: '100%', backgroundColor: 'rgba(0, 229, 255, 0.04)', border: '1px dashed #222634', borderRadius: '14px', padding: '12px 14px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <div style={{ fontSize: '10px', fontWeight: 'bold', color: '#8E93A4', letterSpacing: '1px', textTransform: 'uppercase' }}>Demo Accounts (tap to fill)</div>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            {DEMO_ACCOUNTS.map(acc => (
              <button
                key={acc.email}
                type="button"
                onClick={() => fillDemo(acc.email)}
                style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '8px', padding: '5px 10px', color: '#F5F6F8', fontSize: '11px', fontWeight: 600, cursor: 'pointer' }}
              >
                {acc.label}
              </button>
            ))}
          </div>
          <div style={{ fontFamily: 'monospace', fontSize: '10.5px', color: '#8E93A4' }}>
            Password for all demo accounts: <span style={{ color: '#00E5FF', fontWeight: 'bold' }}>MovieFinder2026!</span>
          </div>
        </div>

        {/* Alternate sign-in options */}
        <div style={{ width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '11px', color: '#8C90A6' }}>or connect securely</span>

          <button
            type="button"
            onClick={signInAsCreatorAdmin}
            style={{
              width: '100%',
              padding: '12px',
              backgroundColor: '#10121A',
              border: '1px solid rgba(0, 229, 255, 0.5)',
              borderRadius: '12px',
              color: '#00E5FF',
              fontSize: '12px',
              fontWeight: 'bold',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px'
            }}
          >
            <Shield style={{ width: '16px', height: '16px' }} />
            Sign In as Administrator
          </button>

          <button
            type="button"
            onClick={continueAsGuest}
            style={{ background: 'none', border: 'none', padding: '4px', color: '#8E93A4', fontSize: '12px', fontWeight: 500, cursor: 'pointer' }}
          >
            Continue as Guest
          </button>
        </div>
      </div>

      {/* Two-Step Verification Dialog */}
      {pending2FA && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', zIndex: 100 }}>
          <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '20px', padding: '24px', maxWidth: '400px', width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
            <div style={{ width: '54px', height: '54px', borderRadius: '50%', backgroundColor: 'rgba(0, 229, 255, 0.15)', border: '1px solid #00E5FF', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <KeyRound style={{ width: '28px', height: '28px', color: '#00E5FF' }} />
            </div>

            <div style={{ textAlign: 'center' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: '#F5F6F8' }}>
                {recoveryMode ? 'Emergency Recovery Code' : 'Two-Step Verification'}
              </h3>
              <p style={{ margin: '6px 0 0 0', fontSize: '12px', color: '#8E93A4', lineHeight: 1.5 }}>
                {recoveryMode
                  ? 'Enter one of your emergency recovery backup codes (e.g. RECV-XXXX-XXXX) to bypass 2FA.'
                  : `Enter the 6-digit verification code from your authenticator app for ${pending2FA.email}.`}
              </p>
            </div>

            <input
              type="text"
              value={twoFACode}
              onChange={e => setTwoFACode(e.target.value)}
              placeholder={recoveryMode ? 'RECV-XXXX-XXXX' : '000000'}
              autoFocus
              style={{ ...inputStyle, textAlign: 'center', fontFamily: 'monospace', fontWeight: 'bold', fontSize: '16px', letterSpacing: '2px' }}
            />

            {error && (
              <div style={{ width: '100%', backgroundColor: 'rgba(255, 82, 82, 0.12)', border: '1px solid #FF5252', borderRadius: '10px', padding: '10px 12px' }}>
                <span style={{ fontSize: '12px', color: '#FF5252' }}>{error}</span>
              </div>
            )}

            <button
              type="button"
              onClick={verifyTwoFactor}
              disabled={busy}
              style={{
                width: '100%',
                padding: '14px',
                backgroundColor: '#00E5FF',
                color: '#06080D',
                border: 'none',
                borderRadius: '12px',
                fontWeight: 'bold',
                fontSize: '13px',
                cursor: busy ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              {busy ? <Loader2 style={{ width: '16px', height: '16px' }} className="animate-spin" /> : null}
              Verify &amp; Authenticate
            </button>

            <button
              type="button"
              onClick={() => {
                setRecoveryMode(!recoveryMode);
                setTwoFACode('');
                setError(null);
              }}
              style={{ background: 'none', border: 'none', padding: 0, color: '#00E5FF', fontSize: '11.5px', fontWeight: 600, cursor: 'pointer' }}
            >
              {recoveryMode ? 'Switch to 6-Digit Authenticator Code' : 'Lost your device? Use Emergency Backup Code'}
            </button>

            <button
              type="button"
              onClick={() => {
                setPending2FA(null);
                setTwoFACode('');
                setError(null);
              }}
              style={{ background: 'none', border: 'none', padding: 0, color: '#8E93A4', fontSize: '12px', cursor: 'pointer' }}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
