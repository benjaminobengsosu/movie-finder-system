import { NextResponse } from 'next/server';
import { listUsers, listDeletedRecords, listRestorePoints, authMode } from '../../../../lib/serverAuth';

// Salt and hash are included for the Credentials Report demo (matching the Android app's
// assignment-inspection view), but live TOTP secrets and recovery codes never leave the server.
function reportUser(u: any) {
  const { two_factor_secret, recovery_codes, ...rest } = u;
  return rest;
}

export async function GET() {
  try {
    const [users, deletedRecords, restorePoints] = await Promise.all([
      listUsers(),
      listDeletedRecords(),
      listRestorePoints()
    ]);
    return NextResponse.json({
      users: users.map(reportUser),
      deletedRecords,
      restorePoints,
      mode: authMode
    });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to load admin data.' }, { status: 500 });
  }
}
