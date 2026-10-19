import { NextRequest, NextResponse } from 'next/server';
import { authenticator } from 'otplib';
import { findUserByEmail, updateUserFields, authMode, safeUser } from '../../../../lib/serverAuth';
import { verifyPassword } from '../../../../lib/crypto';

authenticator.options = { window: 1 };

export async function POST(req: NextRequest) {
  try {
    const { email, password, totpCode, recoveryCode } = await req.json();

    if (!email || !password) {
      return NextResponse.json({ error: 'Email and password are required.' }, { status: 400 });
    }

    const user = await findUserByEmail(email);
    if (!user) {
      return NextResponse.json({ error: 'No account found with that email address.' }, { status: 404 });
    }

    if (user.is_active === false) {
      return NextResponse.json({ error: 'This account has been deactivated by an administrator.' }, { status: 403 });
    }

    if (!verifyPassword(password, user.salt, user.password_hash)) {
      return NextResponse.json({ error: 'Incorrect password. Please try again.' }, { status: 401 });
    }

    if (user.two_factor_enabled) {
      if (!totpCode && !recoveryCode) {
        return NextResponse.json({ requires2FA: true, email: user.email });
      }

      if (totpCode) {
        const token = String(totpCode).replace(/\D/g, '');
        const valid = !!user.two_factor_secret && authenticator.verify({ token, secret: user.two_factor_secret });
        if (!valid) {
          return NextResponse.json({ error: 'Invalid two-step verification code. Codes rotate every 30 seconds.' }, { status: 401 });
        }
      } else if (recoveryCode) {
        let codes: string[] = [];
        try {
          codes = JSON.parse(user.recovery_codes || '[]');
        } catch {
          codes = [];
        }
        const normalized = String(recoveryCode).trim().toUpperCase();
        const idx = codes.findIndex(c => c.toUpperCase() === normalized);
        if (idx === -1) {
          return NextResponse.json({ error: 'Invalid or already-used emergency recovery code.' }, { status: 401 });
        }
        codes.splice(idx, 1);
        await updateUserFields(user.id, { recovery_codes: JSON.stringify(codes) });
      }
    }

    const updated = await updateUserFields(user.id, { last_login_at: Date.now() });
    return NextResponse.json({ user: safeUser(updated || user), mode: authMode });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Sign-in failed.' }, { status: 500 });
  }
}
