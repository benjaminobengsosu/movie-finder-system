import { NextRequest, NextResponse } from 'next/server';
import { authenticator } from 'otplib';
import { findUserByEmail, updateUserFields } from '../../../../../lib/serverAuth';
import { generateRecoveryCodes } from '../../../../../lib/crypto';

export async function POST(req: NextRequest) {
  try {
    const { email } = await req.json();
    if (!email) {
      return NextResponse.json({ error: 'Email is required.' }, { status: 400 });
    }

    const user = await findUserByEmail(email);
    if (!user) {
      return NextResponse.json({ error: 'Account not found.' }, { status: 404 });
    }
    if (user.two_factor_enabled) {
      return NextResponse.json({ error: 'Two-step verification is already active for this account.' }, { status: 409 });
    }

    const secret = authenticator.generateSecret();
    const recoveryCodes = generateRecoveryCodes();
    await updateUserFields(user.id, {
      two_factor_secret: secret,
      recovery_codes: JSON.stringify(recoveryCodes)
    });

    return NextResponse.json({
      secret,
      recoveryCodes,
      otpauthUrl: authenticator.keyuri(user.email, 'Movie Finder', secret)
    });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to start 2FA setup.' }, { status: 500 });
  }
}
