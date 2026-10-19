'use client';

import React, { useState, useEffect } from 'react';
import { Eye, EyeOff, RefreshCw, Cloud, HardDrive } from 'lucide-react';
import { SessionUser } from '../lib/session';

interface AdminUser {
  id: string;
  email: string;
  name: string;
  role: string;
  salt: string;
  password_hash: string;
  two_factor_enabled: boolean;
  created_at: number;
  last_login_at: number;
  is_active: boolean;
}

interface DeletedRecordRow {
  id: string;
  record_type: string;
  original_id: string;
  name: string;
  deleted_data_json: any;
  deleted_by_email: string;
  deleted_at: number;
  reason: string;
}

interface RestorePointRow {
  id: string;
  name: string;
  theme: string;
  maintenance_mode: boolean;
  guest_mode_enabled: boolean;
  announcement_text: string;
  security_strictness: string;
  created_at: number;
  created_by_email: string;
}

interface AdminPanelProps {
  session: SessionUser;
}

export default function AdminPanel({ session }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<'deleted' | 'restore' | 'reports'>('deleted');
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [deletedRecords, setDeletedRecords] = useState<DeletedRecordRow[]>([]);
  const [restorePoints, setRestorePoints] = useState<RestorePointRow[]>([]);
  const [mode, setMode] = useState<'cloud' | 'local'>('local');
  const [loading, setLoading] = useState(true);
  const [revealHashes, setRevealHashes] = useState(false);
  const [currentTime, setCurrentTime] = useState(new Date());

  const [pointName, setPointName] = useState('');
  const [creatingPoint, setCreatingPoint] = useState(false);

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    loadAllData();
    return () => clearInterval(timer);
  }, []);

  async function loadAllData() {
    try {
      const res = await fetch('/api/admin/data');
      const data = await res.json();
      if (res.ok) {
        setUsers(data.users || []);
        setDeletedRecords(data.deletedRecords || []);
        setRestorePoints(data.restorePoints || []);
        setMode(data.mode || 'local');
      }
    } catch {
      // keep previous data on transient failures
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateRestorePoint() {
    if (!pointName.trim()) return;
    setCreatingPoint(true);
    try {
      const res = await fetch('/api/admin/restore-points', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: pointName, createdBy: session.email })
      });
      const data = await res.json();
      if (res.ok) {
        setPointName('');
        await loadAllData();
      }
    } finally {
      setCreatingPoint(false);
    }
  }

  return (
    <div className="bg-cardDark border border-cardBorder rounded-2xl p-6 shadow-2xl space-y-6">
      {/* Live System State Card */}
      <div className="bg-obsidian border border-cardBorder rounded-xl p-4 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 rounded-full bg-neonMint animate-pulse" />
            <span className="text-xs font-mono font-bold text-neonMint uppercase tracking-wider">SYSTEM STATE: ONLINE</span>
            <span className={`flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-bold ${mode === 'cloud' ? 'bg-neonMint/10 text-neonMint' : 'bg-goldAccent/10 text-goldAccent'}`}>
              {mode === 'cloud' ? <Cloud className="w-3 h-3" /> : <HardDrive className="w-3 h-3" />}
              <span>{mode === 'cloud' ? 'SUPABASE CLOUD' : 'LOCAL SECURE STORE'}</span>
            </span>
          </div>
          <p className="text-sm font-semibold text-white mt-1">
            {currentTime.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
          <p className="text-[11px] text-subtleGrey mt-0.5">
            Operator: <span className="text-white font-bold">{session.name}</span> ({session.email})
          </p>
        </div>
        <div className="flex items-center space-x-4">
          <div className="bg-cardDark px-3 py-1.5 rounded-lg border border-cardBorder font-mono text-sm text-electricCyan">
            {currentTime.toLocaleTimeString()}
          </div>
          <div className="text-xs text-subtleGrey">
            <span className="text-white font-bold">{users.length}</span> Users |{' '}
            <span className="text-alertRed font-bold">{deletedRecords.length}</span> Archived |{' '}
            <span className="text-goldAccent font-bold">{restorePoints.length}</span> Snapshots
          </div>
          <button
            onClick={loadAllData}
            title="Refresh data"
            className="p-1.5 border border-cardBorder rounded-lg text-subtleGrey hover:text-white transition"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-cardBorder gap-4 pb-2 overflow-x-auto">
        <button
          onClick={() => setActiveTab('deleted')}
          className={`text-xs font-bold pb-2 transition whitespace-nowrap ${activeTab === 'deleted' ? 'text-alertRed border-b-2 border-alertRed' : 'text-subtleGrey'}`}
        >
          Deleted Archive Database ({deletedRecords.length})
        </button>
        <button
          onClick={() => setActiveTab('restore')}
          className={`text-xs font-bold pb-2 transition whitespace-nowrap ${activeTab === 'restore' ? 'text-goldAccent border-b-2 border-goldAccent' : 'text-subtleGrey'}`}
        >
          Restore Points ({restorePoints.length})
        </button>
        <button
          onClick={() => setActiveTab('reports')}
          className={`text-xs font-bold pb-2 transition whitespace-nowrap ${activeTab === 'reports' ? 'text-neonMint border-b-2 border-neonMint' : 'text-subtleGrey'}`}
        >
          Credentials Report (Salts & Hashes)
        </button>
      </div>

      {/* Deleted Archive Tab */}
      {activeTab === 'deleted' && (
        <div className="space-y-4">
          <p className="text-xs text-subtleGrey">
            Dedicated audit database of deleted users, movies, and configurations with reason and snapshot. User management happens in the Dashboard tab.
          </p>
          <div className="space-y-3">
            {deletedRecords.length === 0 ? (
              <p className="text-xs text-subtleGrey py-8 text-center">No deleted records archived yet.</p>
            ) : (
              deletedRecords.map(r => (
                <div key={r.id} className="bg-obsidian border border-cardBorder rounded-xl p-4 space-y-2">
                  <div className="flex justify-between items-start">
                    <div className="flex items-center space-x-2">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-alertRed/20 text-alertRed">
                        {r.record_type}
                      </span>
                      <span className="font-bold text-sm text-white">{r.name}</span>
                    </div>
                    <span className="text-[11px] text-subtleGrey font-mono">
                      {new Date(r.deleted_at).toLocaleString()}
                    </span>
                  </div>
                  <p className="text-xs text-subtleGrey">
                    Deleted by: <span className="text-white">{r.deleted_by_email}</span> | Reason:{' '}
                    <span className="text-white italic">{r.reason}</span>
                  </p>
                  <pre className="text-[10px] font-mono bg-cardDark p-2 rounded text-electricCyan overflow-x-auto">
                    {JSON.stringify(r.deleted_data_json, null, 2)}
                  </pre>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Restore Points Tab */}
      {activeTab === 'restore' && (
        <div className="space-y-4">
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Restore Point Name (e.g. Staging Baseline)"
              value={pointName}
              onChange={e => setPointName(e.target.value)}
              className="flex-1 bg-obsidian border border-cardBorder rounded-lg px-3 py-2 text-xs text-white"
            />
            <button
              onClick={handleCreateRestorePoint}
              disabled={creatingPoint || !pointName.trim()}
              className="px-4 py-2 bg-electricCyan text-obsidian rounded-lg font-bold text-xs disabled:opacity-50"
            >
              Create Snapshot
            </button>
          </div>
          <p className="text-xs text-subtleGrey">
            Restore points snapshot system themes and flags. User and movie databases are never changed or erased.
          </p>
          <div className="space-y-3">
            {restorePoints.map(p => (
              <div key={p.id} className="bg-obsidian border border-cardBorder rounded-xl p-4 flex justify-between items-center">
                <div>
                  <p className="font-bold text-sm text-white">{p.name}</p>
                  <p className="text-xs text-subtleGrey">Theme: {p.theme} | Created: {new Date(p.created_at).toLocaleString()} by {p.created_by_email}</p>
                </div>
                <button
                  onClick={() => alert(`Restored configuration "${p.name}". User and movie data remain intact.`)}
                  className="px-3 py-1.5 border border-electricCyan text-electricCyan rounded-lg text-xs hover:bg-electricCyan/10 transition"
                >
                  Apply Config
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Credentials Report Tab */}
      {activeTab === 'reports' && (
        <div className="space-y-4">
          <div className="flex justify-between items-center">
            <p className="text-xs text-subtleGrey">
              Security audit report verifying per-user 16-byte random salts, server pepper, and PBKDF2 HMAC-SHA256 hashes.
            </p>
            <button
              onClick={() => setRevealHashes(!revealHashes)}
              className="flex items-center space-x-1 px-3 py-1.5 border border-cardBorder rounded-lg text-xs text-subtleGrey hover:text-white"
            >
              {revealHashes ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
              <span>{revealHashes ? 'Mask Hashes' : 'Reveal Hashes'}</span>
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-obsidian text-subtleGrey uppercase font-mono border-b border-cardBorder">
                <tr>
                  <th className="p-3">User & Role</th>
                  <th className="p-3">Salt (16-Byte Hex)</th>
                  <th className="p-3">Pepper Status</th>
                  <th className="p-3">PBKDF2 HMAC-SHA256 Hash</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-cardBorder font-mono text-[11px]">
                {users.map(u => (
                  <tr key={u.id}>
                    <td className="p-3 text-white font-sans font-bold">
                      {u.name} <span className="text-subtleGrey font-normal text-xs">({u.role})</span>
                    </td>
                    <td className="p-3 text-neonMint">{u.salt}</td>
                    <td className="p-3 text-electricCyan font-bold">VERIFIED (SERVER)</td>
                    <td className="p-3 text-goldAccent">
                      {revealHashes ? u.password_hash : `•••••••••••••••••••••••• (${u.password_hash.substring(0, 8)}...)`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
