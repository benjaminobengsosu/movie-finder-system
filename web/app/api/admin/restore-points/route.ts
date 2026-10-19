import { NextRequest, NextResponse } from 'next/server';
import { insertRestorePoint, authMode } from '../../../../lib/serverAuth';
import { randomUUID } from 'crypto';

export async function POST(req: NextRequest) {
  try {
    const { name, createdBy } = await req.json();
    if (!name?.trim()) {
      return NextResponse.json({ error: 'Restore point name is required.' }, { status: 400 });
    }

    const point = await insertRestorePoint({
      id: randomUUID(),
      name: name.trim(),
      theme: 'Dark Obsidian',
      maintenance_mode: false,
      guest_mode_enabled: true,
      announcement_text: 'Web Configuration Snapshot',
      security_strictness: 'High',
      created_at: Date.now(),
      created_by_email: createdBy || 'admin@moviefinder.com'
    });

    return NextResponse.json({ restorePoint: point, mode: authMode });
  } catch (error: any) {
    return NextResponse.json({ error: error?.message || 'Failed to create restore point.' }, { status: 500 });
  }
}
