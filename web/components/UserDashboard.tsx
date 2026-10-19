'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  Users, UserCheck, ShieldCheck, Archive, UserPlus, Trash2, KeyRound,
  CheckCircle, AlertCircle, Loader2, Copy, X, Clock, Film
} from 'lucide-react';
import { SessionUser, isAdminRole } from '../lib/session';

interface SafeUser {
  id: string;
  email: string;
  name: string;
  role: string;
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
  deleted_by_email: string;
  deleted_at: number;
  reason: string;
}

interface UserDashboardProps {
  session: SessionUser;
  onSessionUpdate: (user: SessionUser) => void;
  discoveriesCount: number;
}

function RoleBadge({ role }: { role: string }) {
  const colors: Record<string, { bg: string; fg: string }> = {
    administrator: { bg: 'rgba(0, 229, 255, 0.18)', fg: '#00E5FF' },
    manager: { bg: 'rgba(179, 136, 255, 0.18)', fg: '#B388FF' },
    'content inputer': { bg: 'rgba(0, 230, 118, 0.18)', fg: '#00E676' }
  };
  const c = colors[role.toLowerCase()] || { bg: 'rgba(144, 164, 174, 0.18)', fg: '#CFD8DC' };
  return (
    <span style={{ backgroundColor: c.bg, color: c.fg, padding: '2px 8px', borderRadius: '6px', fontSize: '9px', fontWeight: 'bold', letterSpacing: '0.5px', textTransform: 'uppercase', whiteSpace: 'nowrap' }}>
      {role}
    </span>
  );
}

function StatCard({ icon, label, value, accent }: { icon: React.ReactNode; label: string; value: number | string; accent: string }) {
  return (
    <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '16px', padding: '16px', display: 'flex', alignItems: 'center', gap: '12px' }}>
      <div style={{ width: '42px', height: '42px', borderRadius: '12px', backgroundColor: `${accent}1A`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        {icon}
      </div>
      <div>
        <div style={{ fontSize: '22px', fontWeight: 'bold', color: '#F5F6F8', lineHeight: 1.1 }}>{value}</div>
        <div style={{ fontSize: '10px', color: '#8E93A4', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 600, marginTop: '2px' }}>{label}</div>
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  backgroundColor: '#06080D',
  border: '1px solid #222634',
  borderRadius: '10px',
  padding: '10px 12px',
  fontSize: '13px',
  color: '#FFFFFF',
  outline: 'none'
};

export default function UserDashboard({ session, onSessionUpdate, discoveriesCount }: UserDashboardProps) {
  const isAdmin = isAdminRole(session.role);
  const [users, setUsers] = useState<SafeUser[]>([]);
  const [deletedRecords, setDeletedRecords] = useState<DeletedRecordRow[]>([]);
  const [mode, setMode] = useState<'cloud' | 'local'>('local');
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Add user modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newName, setNewName] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newPass, setNewPass] = useState('');
  const [newRole, setNewRole] = useState('Standard User');
  const [enable2FA, setEnable2FA] = useState(false);
  const [addBusy, setAddBusy] = useState(false);
  const [created2FA, setCreated2FA] = useState<{ secret: string; recoveryCodes: string[] } | null>(null);

  // 2FA setup modal for own account
  const [twoFASetup, setTwoFASetup] = useState<{ secret: string; recoveryCodes: string[]; otpauthUrl: string } | null>(null);
  const [twoFAToken, setTwoFAToken] = useState('');
  const [twoFABusy, setTwoFABusy] = useState(false);
  const [showDisable2FA, setShowDisable2FA] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const res = await fetch('/api/admin/data');
      const data = await res.json();
      if (res.ok) {
        setUsers(data.users || []);
        setDeletedRecords(data.deletedRecords || []);
        setMode(data.mode || 'local');
      }
    } catch {
      setError('Could not reach the authentication service.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (!notice) return;
    const t = setTimeout(() => setNotice(null), 4000);
    return () => clearTimeout(t);
  }, [notice]);

  const flashNotice = (msg: string) => {
    setNotice(msg);
    setError(null);
  };

  async function handleAddUser(e: React.FormEvent) {
    e.preventDefault();
    if (!newName.trim() || !newEmail.trim() || !newPass) {
      setError('Name, email, and password are required.');
      return;
    }
    setAddBusy(true);
    setError(null);
    try {
      const res = await fetch('/api/admin/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: newName, email: newEmail, password: newPass, role: newRole, twoFactor: enable2FA })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to create user.');
      setCreated2FA(data.twoFactor || null);
      setNewName(''); setNewEmail(''); setNewPass(''); setNewRole('Standard User'); setEnable2FA(false);
      await loadData();
      flashNotice(`Created ${data.user.name} (${data.user.role}) with salt & pepper protection.`);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setAddBusy(false);
    }
  }

  async function handleDeleteUser(user: SafeUser) {
    const reason = window.prompt(`Enter audit deletion reason for ${user.email}:`, 'Administrative account deactivation');
    if (!reason) return;
    try {
      const res = await fetch('/api/admin/users', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: user.id, reason, deletedBy: session.email })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to delete user.');
      await loadData();
      flashNotice(`${user.email} archived to the Deleted Records Database.`);
    } catch (err: any) {
      setError(err.message);
    }
  }

  async function startTwoFASetup() {
    setError(null);
    try {
      const res = await fetch('/api/auth/2fa/setup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: session.email })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to start 2FA setup.');
      setTwoFASetup({ secret: data.secret, recoveryCodes: data.recoveryCodes, otpauthUrl: data.otpauthUrl });
      setTwoFAToken('');
    } catch (err: any) {
      setError(err.message);
    }
  }

  async function confirmTwoFAEnable() {
    if (!twoFASetup) return;
    if (!twoFAToken.trim()) {
      setError('Enter the current 6-digit code from your authenticator app.');
      return;
    }
    setTwoFABusy(true);
    setError(null);
    try {
      const res = await fetch('/api/auth/2fa/enable', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: session.email, token: twoFAToken.trim() })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to enable 2FA.');
      setTwoFASetup(null);
      setTwoFAToken('');
      onSessionUpdate({ ...session, two_factor_enabled: true });
      await loadData();
      flashNotice('Two-step verification activated for your account.');
    } catch (err: any) {
      setError(err.message);
    } finally {
      setTwoFABusy(false);
    }
  }

  async function disableTwoFA() {
    setError(null);
    try {
      const res = await fetch('/api/auth/2fa/disable', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: session.email })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to disable 2FA.');
      setShowDisable2FA(false);
      onSessionUpdate({ ...session, two_factor_enabled: false });
      await loadData();
      flashNotice('Two-step verification disabled.');
    } catch (err: any) {
      setError(err.message);
    }
  }

  const copyText = (text: string) => {
    navigator.clipboard?.writeText(text);
    flashNotice('Copied to clipboard.');
  };

  const activeCount = users.filter(u => u.is_active !== false).length;
  const twofaCount = users.filter(u => u.two_factor_enabled).length;
  const initials = session.name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Welcome banner */}
      <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '22px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '18px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{ width: '54px', height: '54px', borderRadius: '14px', background: 'linear-gradient(135deg, #00E5FF, #0070F3)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px', fontWeight: 'bold', color: '#06080D', flexShrink: 0 }}>
            {initials}
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
              <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: '#FFFFFF' }}>Welcome back, {session.name}</h2>
              <RoleBadge role={session.role} />
              {session.isCreatorAdmin && (
                <span style={{ backgroundColor: 'rgba(255, 179, 0, 0.15)', color: '#FFB300', padding: '2px 8px', borderRadius: '6px', fontSize: '9px', fontWeight: 'bold', letterSpacing: '0.5px' }}>CREATOR</span>
              )}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '6px', fontSize: '11.5px', color: '#8E93A4', flexWrap: 'wrap' }}>
              <span>{session.email}</span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Clock style={{ width: '12px', height: '12px' }} />
                Last login: {new Date(session.last_login_at).toLocaleString()}
              </span>
            </div>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '10px', fontWeight: 'bold', padding: '4px 10px', borderRadius: '999px', backgroundColor: mode === 'cloud' ? 'rgba(0, 230, 118, 0.12)' : 'rgba(255, 179, 0, 0.12)', color: mode === 'cloud' ? '#00E676' : '#FFB300', border: `1px solid ${mode === 'cloud' ? 'rgba(0,230,118,0.4)' : 'rgba(255,179,0,0.4)'}` }}>
            {mode === 'cloud' ? 'SUPABASE CLOUD' : 'LOCAL SECURE STORE'}
          </span>
        </div>
      </div>

      {/* Notices */}
      {notice && (
        <div style={{ backgroundColor: 'rgba(0, 230, 118, 0.12)', border: '1px solid #00E676', borderRadius: '12px', padding: '12px 14px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <CheckCircle style={{ width: '16px', height: '16px', color: '#00E676', flexShrink: 0 }} />
          <span style={{ fontSize: '12px', color: '#00E676' }}>{notice}</span>
        </div>
      )}
      {error && (
        <div style={{ backgroundColor: 'rgba(255, 82, 82, 0.12)', border: '1px solid #FF5252', borderRadius: '12px', padding: '12px 14px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <AlertCircle style={{ width: '16px', height: '16px', color: '#FF5252', flexShrink: 0 }} />
          <span style={{ fontSize: '12px', color: '#FF5252' }}>{error}</span>
        </div>
      )}

      {/* Stat cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '14px' }}>
        <StatCard icon={<Users style={{ width: '20px', height: '20px', color: '#00E5FF' }} />} label="Registered Users" value={loading ? '—' : users.length} accent="#00E5FF" />
        <StatCard icon={<UserCheck style={{ width: '20px', height: '20px', color: '#00E676' }} />} label="Active Accounts" value={loading ? '—' : activeCount} accent="#00E676" />
        <StatCard icon={<ShieldCheck style={{ width: '20px', height: '20px', color: '#FFB300' }} />} label="2FA Protected" value={loading ? '—' : twofaCount} accent="#FFB300" />
        <StatCard icon={<Archive style={{ width: '20px', height: '20px', color: '#FF5252' }} />} label="Archived Records" value={loading ? '—' : deletedRecords.length} accent="#FF5252" />
        <StatCard icon={<Film style={{ width: '20px', height: '20px', color: '#B388FF' }} />} label="Shared Discoveries" value={discoveriesCount} accent="#B388FF" />
      </div>

      {/* Profile & security */}
      <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '22px 24px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <div style={{ fontSize: '12px', fontWeight: 'bold', letterSpacing: '1px', textTransform: 'uppercase', color: '#00E5FF' }}>Account & Security</div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '14px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '12px' }}>
            <span style={{ color: '#8E93A4' }}>Membership role: <span style={{ color: '#F5F6F8', fontWeight: 600 }}>{session.role}</span></span>
            <span style={{ color: '#8E93A4' }}>Member since: <span style={{ color: '#F5F6F8' }}>{new Date(session.created_at).toLocaleDateString()}</span></span>
            <span style={{ color: '#8E93A4' }}>Password protection: <span style={{ color: '#00E676', fontWeight: 600 }}>Per-user salt + server pepper + PBKDF2 HMAC-SHA256</span></span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '12px', color: session.two_factor_enabled ? '#00E676' : '#8E93A4', fontWeight: 600 }}>
              2FA: {session.two_factor_enabled ? 'ACTIVE (TOTP)' : 'OFF'}
            </span>
            {session.two_factor_enabled ? (
              <button
                onClick={() => setShowDisable2FA(true)}
                style={{ padding: '8px 14px', backgroundColor: 'transparent', border: '1px solid #FF5252', borderRadius: '10px', color: '#FF5252', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                Disable 2FA
              </button>
            ) : (
              <button
                onClick={startTwoFASetup}
                style={{ padding: '8px 14px', backgroundColor: '#00E5FF', border: 'none', borderRadius: '10px', color: '#06080D', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                <KeyRound style={{ width: '14px', height: '14px' }} />
                Enable 2FA
              </button>
            )}
          </div>
        </div>
      </div>

      {/* User directory */}
      <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '22px 24px', display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
          <div>
            <div style={{ fontSize: '15px', fontWeight: 'bold', color: '#F5F6F8' }}>User Directory & Access Control</div>
            <div style={{ fontSize: '11.5px', color: '#8E93A4', marginTop: '2px' }}>
              {loading ? 'Loading identities...' : `${users.length} active registered identities across 4 role tiers`}
            </div>
          </div>
          {isAdmin && (
            <button
              onClick={() => { setShowAddModal(true); setCreated2FA(null); }}
              style={{ padding: '8px 14px', backgroundColor: '#00E5FF', border: 'none', borderRadius: '10px', color: '#06080D', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <UserPlus style={{ width: '14px', height: '14px' }} />
              Add User
            </button>
          )}
        </div>

        {!isAdmin && (
          <div style={{ fontSize: '11.5px', color: '#8E93A4', backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '10px', padding: '10px 12px' }}>
            Read-only view. User management actions require an Administrator role.
          </div>
        )}

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '12px', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #222634' }}>
                {['User', 'Role', '2FA', 'Created', 'Last Login', 'Status', ...(isAdmin ? ['Actions'] : [])].map(h => (
                  <th key={h} style={{ padding: '10px 12px', color: '#8E93A4', fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.8px', fontWeight: 'bold', whiteSpace: 'nowrap' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} style={{ padding: '24px', textAlign: 'center', color: '#8E93A4' }}>Loading user directory...</td></tr>
              ) : users.length === 0 ? (
                <tr><td colSpan={7} style={{ padding: '24px', textAlign: 'center', color: '#8E93A4' }}>No registered users found.</td></tr>
              ) : users.map(u => {
                const isSelf = u.email.toLowerCase() === session.email.toLowerCase();
                return (
                  <tr key={u.id} style={{ borderBottom: '1px solid #1A1E2A' }}>
                    <td style={{ padding: '12px', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ fontWeight: 'bold', color: '#FFFFFF' }}>{u.name}</span>
                        {isSelf && <span style={{ fontSize: '9px', fontWeight: 'bold', color: '#00E5FF' }}>(YOU)</span>}
                      </div>
                      <div style={{ fontSize: '11px', color: '#8E93A4', marginTop: '2px' }}>{u.email}</div>
                    </td>
                    <td style={{ padding: '12px' }}><RoleBadge role={u.role} /></td>
                    <td style={{ padding: '12px', whiteSpace: 'nowrap' }}>
                      {u.two_factor_enabled
                        ? <span style={{ color: '#00E676', fontWeight: 600, fontSize: '11px' }}>🔒 Enabled</span>
                        : <span style={{ color: '#8E93A4', fontSize: '11px' }}>🔓 Off</span>}
                    </td>
                    <td style={{ padding: '12px', color: '#8E93A4', whiteSpace: 'nowrap' }}>{new Date(u.created_at).toLocaleDateString()}</td>
                    <td style={{ padding: '12px', color: '#8E93A4', whiteSpace: 'nowrap' }}>{new Date(u.last_login_at).toLocaleString()}</td>
                    <td style={{ padding: '12px', whiteSpace: 'nowrap' }}>
                      <span style={{ color: u.is_active !== false ? '#00E676' : '#FF5252', fontSize: '11px', fontWeight: 600 }}>
                        {u.is_active !== false ? 'Active' : 'Deactivated'}
                      </span>
                    </td>
                    {isAdmin && (
                      <td style={{ padding: '12px', textAlign: 'right' }}>
                        {!isSelf ? (
                          <button
                            onClick={() => handleDeleteUser(u)}
                            title="Delete to Archive"
                            style={{ backgroundColor: 'rgba(255, 82, 82, 0.12)', border: 'none', borderRadius: '8px', padding: '6px', cursor: 'pointer', display: 'inline-flex' }}
                          >
                            <Trash2 style={{ width: '15px', height: '15px', color: '#FF5252' }} />
                          </button>
                        ) : (
                          <span style={{ fontSize: '10px', color: '#8E93A4' }}>—</span>
                        )}
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add User Modal */}
      {showAddModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', zIndex: 60 }}>
          <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', maxWidth: '440px', width: '100%', maxHeight: '90vh', overflowY: 'auto' }}>
            {created2FA ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 'bold', color: '#FFFFFF' }}>2FA Provisioned for New User</h3>
                  <button onClick={() => setShowAddModal(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#8E93A4' }}><X style={{ width: '18px', height: '18px' }} /></button>
                </div>
                <p style={{ margin: 0, fontSize: '12px', color: '#8E93A4', lineHeight: 1.5 }}>
                  Share this Base32 secret with the user to add to their authenticator app. These recovery codes are shown only once:
                </p>
                <div style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '10px', padding: '12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                  <div>
                    <div style={{ fontSize: '9px', fontWeight: 'bold', color: '#8E93A4', letterSpacing: '1px' }}>SECRET KEY (BASE32)</div>
                    <div style={{ fontFamily: 'monospace', fontSize: '13px', fontWeight: 'bold', color: '#00E676', wordBreak: 'break-all' }}>{created2FA.secret}</div>
                  </div>
                  <button onClick={() => copyText(created2FA.secret)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#00E5FF' }} title="Copy secret"><Copy style={{ width: '16px', height: '16px' }} /></button>
                </div>
                <div style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '10px', padding: '12px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ fontSize: '9px', fontWeight: 'bold', color: '#8E93A4', letterSpacing: '1px' }}>EMERGENCY RECOVERY CODES</div>
                  {created2FA.recoveryCodes.map(code => (
                    <div key={code} style={{ fontFamily: 'monospace', fontSize: '12px', fontWeight: 'bold', color: '#F5F6F8' }}>{code}</div>
                  ))}
                </div>
                <button
                  onClick={() => { setShowAddModal(false); setCreated2FA(null); }}
                  style={{ padding: '12px', backgroundColor: '#00E5FF', border: 'none', borderRadius: '10px', color: '#06080D', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  Done
                </button>
              </div>
            ) : (
              <form onSubmit={handleAddUser} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 'bold', color: '#FFFFFF' }}>Create User with Salt &amp; Pepper Protection</h3>
                <input type="text" placeholder="Full Name" required value={newName} onChange={e => setNewName(e.target.value)} style={inputStyle} />
                <input type="email" placeholder="Email Address" required value={newEmail} onChange={e => setNewEmail(e.target.value)} style={inputStyle} />
                <input type="password" placeholder="Password (Will be salted & peppered)" required value={newPass} onChange={e => setNewPass(e.target.value)} style={inputStyle} />
                <select value={newRole} onChange={e => setNewRole(e.target.value)} style={inputStyle}>
                  <option value="Standard User">Standard User</option>
                  <option value="Content Inputer">Content Inputer</option>
                  <option value="Manager">Manager</option>
                  <option value="Administrator">Administrator</option>
                </select>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '12px', color: '#8E93A4', cursor: 'pointer' }}>
                  <input type="checkbox" checked={enable2FA} onChange={e => setEnable2FA(e.target.checked)} />
                  <span>Enable Two-Step Verification (2FA TOTP)</span>
                </label>
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                  <button type="button" onClick={() => setShowAddModal(false)} style={{ padding: '10px 16px', backgroundColor: 'rgba(255,255,255,0.08)', border: 'none', borderRadius: '10px', color: '#F5F6F8', fontSize: '12px', cursor: 'pointer' }}>Cancel</button>
                  <button type="submit" disabled={addBusy} style={{ padding: '10px 16px', backgroundColor: '#00E5FF', border: 'none', borderRadius: '10px', color: '#06080D', fontSize: '12px', fontWeight: 'bold', cursor: addBusy ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    {addBusy && <Loader2 style={{ width: '14px', height: '14px' }} className="animate-spin" />}
                    Create Account
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* 2FA Setup Modal */}
      {twoFASetup && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', zIndex: 60 }}>
          <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', maxWidth: '440px', width: '100%', maxHeight: '90vh', overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 'bold', color: '#FFFFFF' }}>Set Up Two-Factor Authentication</h3>
              <button onClick={() => setTwoFASetup(null)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#8E93A4' }}><X style={{ width: '18px', height: '18px' }} /></button>
            </div>

            <div>
              <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#00E5FF', marginBottom: '4px' }}>Step 1: Add the secret to your authenticator app</div>
              <div style={{ fontSize: '11.5px', color: '#8E93A4', lineHeight: 1.5, marginBottom: '8px' }}>
                Open Google Authenticator or Authy, tap &quot;+&quot;, select &quot;Enter a setup key&quot;, and paste the key below:
              </div>
              <div style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '10px', padding: '12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '8px' }}>
                <div>
                  <div style={{ fontSize: '9px', fontWeight: 'bold', color: '#8E93A4', letterSpacing: '1px' }}>SECRET KEY (BASE32)</div>
                  <div style={{ fontFamily: 'monospace', fontSize: '13px', fontWeight: 'bold', color: '#00E676', wordBreak: 'break-all' }}>{twoFASetup.secret}</div>
                </div>
                <button onClick={() => copyText(twoFASetup.secret)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#00E5FF', flexShrink: 0 }} title="Copy secret"><Copy style={{ width: '16px', height: '16px' }} /></button>
              </div>
            </div>

            <div>
              <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#00E5FF', marginBottom: '4px' }}>Step 2: Save your emergency recovery codes</div>
              <div style={{ fontSize: '11.5px', color: '#8E93A4', lineHeight: 1.5, marginBottom: '8px' }}>
                If you ever lose your phone, each code below can be used once to sign in:
              </div>
              <div style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '10px', padding: '12px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {twoFASetup.recoveryCodes.map((code, i) => (
                  <div key={code} style={{ display: 'flex', gap: '8px' }}>
                    <span style={{ fontSize: '11px', color: '#8E93A4' }}>{i + 1}.</span>
                    <span style={{ fontFamily: 'monospace', fontSize: '12px', fontWeight: 'bold', color: '#F5F6F8' }}>{code}</span>
                  </div>
                ))}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#00E5FF', marginBottom: '4px' }}>Step 3: Confirm with a 6-digit code</div>
              <input
                type="text"
                value={twoFAToken}
                onChange={e => setTwoFAToken(e.target.value)}
                placeholder="Current code from authenticator"
                style={{ ...inputStyle, fontFamily: 'monospace', textAlign: 'center', letterSpacing: '2px' }}
              />
            </div>

            <button
              onClick={confirmTwoFAEnable}
              disabled={twoFABusy}
              style={{ padding: '12px', backgroundColor: '#00E5FF', border: 'none', borderRadius: '10px', color: '#06080D', fontSize: '12px', fontWeight: 'bold', cursor: twoFABusy ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
            >
              {twoFABusy && <Loader2 style={{ width: '14px', height: '14px' }} className="animate-spin" />}
              Activate 2-Step Verification
            </button>
          </div>
        </div>
      )}

      {/* Disable 2FA confirmation */}
      {showDisable2FA && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', zIndex: 60 }}>
          <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', maxWidth: '380px', width: '100%', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 'bold', color: '#FFFFFF' }}>Disable Two-Step Verification?</h3>
            <p style={{ margin: 0, fontSize: '12px', color: '#8E93A4', lineHeight: 1.5 }}>
              Your account will no longer require an authenticator code at sign-in. Stored recovery codes will be cleared.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button onClick={() => setShowDisable2FA(false)} style={{ padding: '10px 16px', backgroundColor: 'rgba(255,255,255,0.08)', border: 'none', borderRadius: '10px', color: '#F5F6F8', fontSize: '12px', cursor: 'pointer' }}>Cancel</button>
              <button onClick={disableTwoFA} style={{ padding: '10px 16px', backgroundColor: '#FF5252', border: 'none', borderRadius: '10px', color: '#FFFFFF', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}>Disable 2FA</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
