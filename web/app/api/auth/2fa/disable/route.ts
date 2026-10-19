import { NextRequest, NextResponse } from 'next/server';
import { findUserByEmail, updateUserFields } from '../../../../../lib/serverAuth';

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

    await updateUserFields(user.id, {
      two_factor_enabled: false,
      two_factor_secret: null,
      recovery_codes: '[]'
    });

    return NextResponse.json({ success: true, two_factor_enabled: false });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to disable 2FA.' }, { status: 500 });
  }
}
