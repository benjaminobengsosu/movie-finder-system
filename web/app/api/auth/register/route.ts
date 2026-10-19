import { NextRequest, NextResponse } from 'next/server';
import { findUserByEmail, insertUser, buildUserRecord, authMode, safeUser } from '../../../../lib/serverAuth';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export async function POST(req: NextRequest) {
  try {
    const { name, email, password } = await req.json();

    if (!name?.trim() || !email?.trim() || !password) {
      return NextResponse.json({ error: 'Name, email, and password are all required.' }, { status: 400 });
    }
    if (!EMAIL_RE.test(email.trim())) {
      return NextResponse.json({ error: 'Please enter a valid email address.' }, { status: 400 });
    }
    if (String(password).length < 6) {
      return NextResponse.json({ error: 'Password must be at least 6 characters.' }, { status: 400 });
    }

    const existing = await findUserByEmail(email);
    if (existing) {
      return NextResponse.json({ error: 'An account with that email already exists.' }, { status: 409 });
    }

    const user = buildUserRecord({ name, email, password, role: 'Standard User' });
    await insertUser(user);

    return NextResponse.json({ user: safeUser(user), mode: authMode });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Registration failed.' }, { status: 500 });
  }
}
