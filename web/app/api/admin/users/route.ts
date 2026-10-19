import { NextRequest, NextResponse } from 'next/server';
import { authenticator } from 'otplib';
import { randomUUID } from 'crypto';
import { findUserByEmail, findUserById, insertUser, buildUserRecord, deleteUserRecord, insertDeletedRecord, authMode, safeUser } from '../../../../lib/serverAuth';
import { generateRecoveryCodes } from '../../../../lib/crypto';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const VALID_ROLES = ['Administrator', 'Manager', 'Content Inputer', 'Standard User'];

export async function POST(req: NextRequest) {
  try {
    const { name, email, password, role = 'Standard User', twoFactor = false } = await req.json();

    if (!name?.trim() || !email?.trim() || !password) {
      return NextResponse.json({ error: 'Name, email, and password are all required.' }, { status: 400 });
    }
    if (!EMAIL_RE.test(email.trim())) {
      return NextResponse.json({ error: 'Please enter a valid email address.' }, { status: 400 });
    }
    if (String(password).length < 6) {
      return NextResponse.json({ error: 'Password must be at least 6 characters.' }, { status: 400 });
    }
    if (!VALID_ROLES.includes(role)) {
      return NextResponse.json({ error: 'Invalid role specified.' }, { status: 400 });
    }

    const existing = await findUserByEmail(email);
    if (existing) {
      return NextResponse.json({ error: `User with email ${email} already exists.` }, { status: 409 });
    }

    const twoFactorSecret = twoFactor ? authenticator.generateSecret() : null;
    const recoveryCodes = twoFactor ? JSON.stringify(generateRecoveryCodes()) : '[]';

    const user = buildUserRecord({
      name,
      email,
      password,
      role,
      two_factor_enabled: twoFactor,
      two_factor_secret: twoFactorSecret,
      recovery_codes: recoveryCodes
    });
    await insertUser(user);

    return NextResponse.json({
      user: safeUser(user),
      mode: authMode,
      twoFactor: twoFactor
        ? { secret: twoFactorSecret, recoveryCodes: JSON.parse(recoveryCodes) as string[] }
        : null
    });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to create user.' }, { status: 500 });
  }
}

export async function DELETE(req: NextRequest) {
  try {
    const { id, reason, deletedBy } = await req.json();
    if (!id) {
      return NextResponse.json({ error: 'User id is required.' }, { status: 400 });
    }

    const user = await findUserById(id);
    if (!user) {
      return NextResponse.json({ error: 'User not found (may already be deleted).' }, { status: 404 });
    }

    await insertDeletedRecord({
      id: randomUUID(),
      record_type: 'USER',
      original_id: user.id,
      name: `${user.name} (${user.email})`,
      deleted_data_json: user,
      deleted_by_email: deletedBy || 'admin@moviefinder.com',
      deleted_at: Date.now(),
      reason: reason?.trim() || 'Administrative deletion'
    });

    const removed = await deleteUserRecord(user.id);
    if (!removed) {
      return NextResponse.json({ error: 'Failed to delete user.' }, { status: 500 });
    }

    return NextResponse.json({ success: true, archivedEmail: user.email, mode: authMode });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to delete user.' }, { status: 500 });
  }
}
