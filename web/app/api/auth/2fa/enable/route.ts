import { NextRequest, NextResponse } from 'next/server';
import { authenticator } from 'otplib';
import { findUserByEmail, updateUserFields } from '../../../../../lib/serverAuth';

authenticator.options = { window: 1 };

export async function POST(req: NextRequest) {
  try {
    const { email, token } = await req.json();
    if (!email || !token) {
      return NextResponse.json({ error: 'Email and verification code are required.' }, { status: 400 });
    }

    const user = await findUserByEmail(email);
    if (!user) {
      return NextResponse.json({ error: 'Account not found.' }, { status: 404 });
    }
    if (!user.two_factor_secret) {
      return NextResponse.json({ error: 'No pending 2FA setup found. Start the setup again.' }, { status: 400 });
    }

    const code = String(token).replace(/\D/g, '');
    const valid = authenticator.verify({ token: code, secret: user.two_factor_secret });
    if (!valid) {
      return NextResponse.json({ error: 'Invalid verification code. Codes rotate every 30 seconds.' }, { status: 401 });
    }

    await updateUserFields(user.id, { two_factor_enabled: true });
    return NextResponse.json({ success: true, two_factor_enabled: true });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to enable 2FA.' }, { status: 500 });
  }
}
