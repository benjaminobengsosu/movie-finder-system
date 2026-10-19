'use client';

import React, { useState, useEffect } from 'react';
import CameraViewfinder from '../components/CameraViewfinder';
import AdminPanel from '../components/AdminPanel';
import AuthScreen from '../components/AuthScreen';
import UserDashboard from '../components/UserDashboard';
import { supabase, MovieMatch } from '../lib/supabase';
import { SessionUser, getSession, setSession, clearSession, isAdminRole } from '../lib/session';
import { Camera, Upload, Film, Bookmark, Shield, Sparkles, ExternalLink, Key, AlertCircle, CheckCircle, LayoutDashboard, LogOut } from 'lucide-react';

const roleColor = (role: string) => {
  switch ((role || '').toLowerCase()) {
    case 'administrator': return '#00E5FF';
    case 'manager': return '#B388FF';
    case 'content inputer': return '#00E676';
    case 'guest': return '#8E93A4';
    default: return '#CFD8DC';
  }
};

export default function MovieFinderWeb() {
  const [activeTab, setActiveTab] = useState<'analyzer' | 'watchlist' | 'dashboard' | 'admin'>('analyzer');
  const [session, setSessionState] = useState<SessionUser | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [showCamera, setShowCamera] = useState(false);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [contextClues, setContextClues] = useState('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [movieResult, setMovieResult] = useState<any>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [watchlist, setWatchlist] = useState<MovieMatch[]>([]);
  
  // Custom API key stored in localStorage if not set in .env.local
  const [userApiKey, setUserApiKey] = useState('');
  const [showApiKeyModal, setShowApiKeyModal] = useState(false);

  useEffect(() => {
    setSessionState(getSession());
    setAuthReady(true);
    loadWatchlist();
    const savedKey = localStorage.getItem('MOVIE_FINDER_GEMINI_KEY');
    if (savedKey) setUserApiKey(savedKey);
  }, []);

  useEffect(() => {
    if (!authReady) return;
    const allowed = ['analyzer', 'watchlist'];
    if (session && !session.isGuest) allowed.push('dashboard');
    if (isAdminRole(session?.role)) allowed.push('admin');
    if (!allowed.includes(activeTab)) setActiveTab('analyzer');
  }, [session, authReady]);

  const handleAuthenticated = (user: SessionUser) => {
    setSession(user);
    setSessionState(user);
    setActiveTab('analyzer');
  };

  const handleLogout = () => {
    clearSession();
    setSessionState(null);
    setActiveTab('analyzer');
  };

  const handleSessionUpdate = (user: SessionUser) => {
    setSession(user);
    setSessionState(user);
  };

  async function loadWatchlist() {
    try {
      const { data } = await supabase.from('movie_matches').select('*').order('created_at', { ascending: false });
      if (data) setWatchlist(data as MovieMatch[]);
    } catch (e) {
      console.warn('Supabase offline or initial setup:', e);
    }
  }

  const saveApiKey = (key: string) => {
    setUserApiKey(key);
    localStorage.setItem('MOVIE_FINDER_GEMINI_KEY', key);
    setShowApiKeyModal(false);
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      setSelectedImage(event.target?.result as string);
      setMovieResult(null);
      setErrorMessage(null);
    };
    reader.readAsDataURL(file);
  };

  const handleIdentify = async () => {
    const hasImage = !!selectedImage;
    const hasText = !!contextClues.trim();
    if (!hasImage && !hasText) {
      setErrorMessage("Please capture/upload a scene picture or enter description clues.");
      return;
    }

    setIsAnalyzing(true);
    setMovieResult(null);
    setErrorMessage(null);

    try {
      const res = await fetch('/api/identify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageBase64: selectedImage,
          contextPrompt: contextClues.trim(),
          userApiKey: userApiKey.trim()
        })
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.error || `Server returned error (${res.status})`);
      }

      setMovieResult(data);

      // Save to shared Supabase movie_matches table
      if (data.title && data.title.toLowerCase() !== 'unknown') {
        try {
          await supabase.from('movie_matches').insert([{
            title: data.title,
            year: data.year || '',
            director: data.director || '',
            imdb_url: data.imdbUrl || 'https://www.imdb.com',
            confidence: data.confidence || 'High',
            rationale: data.rationale || '',
            actors: data.actors || '',
            genre: data.genre || '',
            search_type: selectedImage ? 'IMAGE' : 'TEXT',
            poster_path: data.poster_path || '',
            is_in_watchlist: false,
            created_at: Date.now()
          }]);
          loadWatchlist();
        } catch (e) {
          console.warn('Saved locally, Supabase write bypassed:', e);
        }
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to identify movie scene');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const canSearch = !!selectedImage || !!contextClues.trim();

  if (!authReady) {
    return (
      <div style={{ backgroundColor: '#06080D', minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8E93A4', fontFamily: 'system-ui, sans-serif', fontSize: '13px' }}>
        Loading Movie Finder Web...
      </div>
    );
  }

  if (!session) {
    return <AuthScreen onAuthenticated={handleAuthenticated} />;
  }

  return (
    <div style={{ backgroundColor: '#06080D', minHeight: '100vh', color: '#F5F6F8', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      {/* Top Header */}
      <header style={{ backgroundColor: '#10121A', borderBottom: '1px solid #222634', padding: '14px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', position: 'sticky', top: 0, zIndex: 40 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '12px', background: 'linear-gradient(135deg, #00E5FF, #0070F3)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Film style={{ width: '22px', height: '22px', color: '#06080D' }} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '18px', fontWeight: 'bold', color: '#FFFFFF' }}>Movie Finder</span>
              <span style={{ fontSize: '10px', backgroundColor: 'rgba(0, 229, 255, 0.15)', color: '#00E5FF', padding: '2px 6px', borderRadius: '4px', fontFamily: 'monospace', fontWeight: 'bold' }}>WEB CLOUD</span>
            </div>
            <p style={{ margin: 0, fontSize: '12px', color: '#8E93A4' }}>Discover the movie behind the scene</p>
          </div>
        </div>

        {/* Tab Controls, Session & Settings */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', backgroundColor: '#06080D', padding: '4px', borderRadius: '12px', border: '1px solid #222634', gap: '4px' }}>
            {([
              { key: 'analyzer' as const, label: 'Analyzer', icon: Camera, activeColor: '#00E5FF' },
              { key: 'watchlist' as const, label: `Watchlist (${watchlist.length})`, icon: Bookmark, activeColor: '#00E5FF' },
              ...(session && !session.isGuest ? [{ key: 'dashboard' as const, label: 'Dashboard', icon: LayoutDashboard, activeColor: '#B388FF' }] : []),
              ...(isAdminRole(session.role) ? [{ key: 'admin' as const, label: 'Admin & Security Hub', icon: Shield, activeColor: '#FFB300' }] : [])
            ]).map(tab => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.key;
              return (
                <button
                  key={tab.key}
                  onClick={() => setActiveTab(tab.key)}
                  style={{
                    padding: '6px 14px',
                    borderRadius: '8px',
                    fontSize: '12px',
                    fontWeight: 'bold',
                    backgroundColor: isActive ? tab.activeColor : 'transparent',
                    color: isActive ? '#06080D' : '#8E93A4',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px'
                  }}
                >
                  <Icon style={{ width: '14px', height: '14px' }} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </div>

          {/* Signed-in identity chip */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 8px 4px 4px', backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '12px' }}>
            <div style={{ width: '28px', height: '28px', borderRadius: '8px', background: 'linear-gradient(135deg, #00E5FF, #0070F3)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '11px', fontWeight: 'bold', color: '#06080D', flexShrink: 0 }}>
              {session.name.split(' ').map(p => p[0]).slice(0, 2).join('').toUpperCase()}
            </div>
            <div style={{ lineHeight: 1.2 }}>
              <div style={{ fontSize: '11px', fontWeight: 'bold', color: '#FFFFFF', maxWidth: '140px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {session.name}
              </div>
              <div style={{ fontSize: '9px', fontWeight: 'bold', letterSpacing: '0.5px', color: roleColor(session.role), textTransform: 'uppercase' }}>
                {session.role}{session.isGuest ? ' Mode' : ''}
              </div>
            </div>
            <button
              onClick={handleLogout}
              title={session.isGuest ? 'Exit guest mode' : 'Sign out'}
              style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#8E93A4', display: 'flex', padding: '4px' }}
            >
              <LogOut style={{ width: '15px', height: '15px' }} />
            </button>
          </div>

          <button
            onClick={() => setShowApiKeyModal(true)}
            style={{
              padding: '8px 12px',
              borderRadius: '10px',
              fontSize: '12px',
              backgroundColor: userApiKey ? 'rgba(0, 230, 118, 0.15)' : 'rgba(255, 255, 255, 0.08)',
              color: userApiKey ? '#00E676' : '#8E93A4',
              border: `1px solid ${userApiKey ? '#00E676' : '#222634'}`,
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
            title="Configure Gemini API Key"
          >
            <Key style={{ width: '14px', height: '14px' }} />
            <span>{userApiKey ? 'API Key Set' : 'Configure Key'}</span>
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main style={{ maxWidth: '1100px', margin: '0 auto', padding: '28px 20px', display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {activeTab === 'analyzer' && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px', alignItems: 'start' }}>
            
            {/* Input Box */}
            <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', display: 'flex', flexDirection: 'column', gap: '20px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#00E5FF' }}>
                <Sparkles style={{ width: '18px', height: '18px' }} />
                <span style={{ fontSize: '13px', fontWeight: 'bold', letterSpacing: '1px', textTransform: 'uppercase' }}>Scene Capture & Clues</span>
              </div>

              {/* Camera viewfinder or Image Preview or Chooser */}
              {showCamera ? (
                <CameraViewfinder
                  onCapture={(dataUrl) => {
                    setSelectedImage(dataUrl);
                    setShowCamera(false);
                    setErrorMessage(null);
                  }}
                  onClose={() => setShowCamera(false)}
                />
              ) : selectedImage ? (
                <div style={{ position: 'relative', width: '100%', height: '240px', backgroundColor: '#000000', borderRadius: '14px', overflow: 'hidden', border: '1px solid #00E5FF' }}>
                  <img src={selectedImage} alt="Captured Scene" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                  <button
                    onClick={() => setSelectedImage(null)}
                    style={{ position: 'absolute', top: '10px', right: '10px', backgroundColor: 'rgba(0,0,0,0.8)', border: '1px solid #222634', color: '#FFFFFF', padding: '6px 12px', borderRadius: '8px', fontSize: '11px', fontWeight: 'bold' }}
                  >
                    Change Picture
                  </button>
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                  <button
                    onClick={() => setShowCamera(true)}
                    style={{
                      padding: '24px 16px',
                      backgroundColor: '#06080D',
                      border: '2px dashed #222634',
                      borderRadius: '16px',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '8px',
                      cursor: 'pointer'
                    }}
                  >
                    <div style={{ width: '44px', height: '44px', borderRadius: '50%', backgroundColor: 'rgba(0, 229, 255, 0.1)', color: '#00E5FF', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Camera style={{ width: '22px', height: '22px' }} />
                    </div>
                    <span style={{ fontSize: '13px', fontWeight: 'bold', color: '#FFFFFF' }}>Live Camera</span>
                    <span style={{ fontSize: '11px', color: '#8E93A4' }}>Point at TV or Screen</span>
                  </button>

                  <label
                    style={{
                      padding: '24px 16px',
                      backgroundColor: '#06080D',
                      border: '2px dashed #222634',
                      borderRadius: '16px',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '8px',
                      cursor: 'pointer'
                    }}
                  >
                    <div style={{ width: '44px', height: '44px', borderRadius: '50%', backgroundColor: 'rgba(255, 255, 255, 0.08)', color: '#FFFFFF', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <Upload style={{ width: '22px', height: '22px' }} />
                    </div>
                    <span style={{ fontSize: '13px', fontWeight: 'bold', color: '#FFFFFF' }}>Upload Still</span>
                    <span style={{ fontSize: '11px', color: '#8E93A4' }}>Photo or Screenshot</span>
                    <input type="file" accept="image/*" onChange={handleFileUpload} style={{ display: 'none' }} />
                  </label>
                </div>
              )}

              {/* Text Clues */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <label style={{ fontSize: '12px', color: '#8E93A4', fontWeight: '500' }}>
                  Dialogue, character names, or scene description (Optional or standalone):
                </label>
                <textarea
                  rows={3}
                  value={contextClues}
                  onChange={e => {
                    setContextClues(e.target.value);
                    setErrorMessage(null);
                  }}
                  placeholder="e.g. Man in trenchcoat spinning a brass top in an airport..."
                  style={{
                    width: '100%',
                    backgroundColor: '#06080D',
                    border: '1px solid #222634',
                    borderRadius: '12px',
                    padding: '12px',
                    fontSize: '13px',
                    color: '#FFFFFF',
                    outline: 'none',
                    resize: 'none'
                  }}
                />
              </div>

              {/* Error banner */}
              {errorMessage && (
                <div style={{ backgroundColor: 'rgba(255, 82, 82, 0.15)', border: '1px solid #FF5252', borderRadius: '10px', padding: '12px', display: 'flex', alignItems: 'flex-start', gap: '10px' }}>
                  <AlertCircle style={{ width: '18px', height: '18px', color: '#FF5252', flexShrink: 0 }} />
                  <div style={{ fontSize: '12px', color: '#FF5252', lineHeight: '1.4' }}>
                    {errorMessage}
                    {errorMessage.includes('API Key') && (
                      <button
                        onClick={() => setShowApiKeyModal(true)}
                        style={{ marginLeft: '8px', textDecoration: 'underline', fontWeight: 'bold', color: '#00E5FF' }}
                      >
                        Enter API Key Now
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* Action Button */}
              <button
                onClick={handleIdentify}
                disabled={isAnalyzing || !canSearch}
                style={{
                  width: '100%',
                  padding: '16px',
                  backgroundColor: canSearch ? '#00E5FF' : '#222634',
                  color: canSearch ? '#06080D' : '#8E93A4',
                  borderRadius: '12px',
                  fontWeight: 'bold',
                  fontSize: '13px',
                  letterSpacing: '1px',
                  textTransform: 'uppercase',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  cursor: canSearch ? 'pointer' : 'not-allowed',
                  transition: '0.2s all'
                }}
              >
                {isAnalyzing ? (
                  <span>Analyzing with Gemini AI...</span>
                ) : (
                  <>
                    <Sparkles style={{ width: '16px', height: '16px' }} />
                    <span>Identify Movie Scene</span>
                  </>
                )}
              </button>
            </div>

            {/* Results Box */}
            <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', minHeight: '340px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              {movieResult ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                    {movieResult.poster_path && (
                      <img
                        src={movieResult.poster_path}
                        alt={movieResult.title}
                        style={{ width: '90px', height: '135px', objectFit: 'cover', borderRadius: '10px', border: '1px solid #222634', flexShrink: 0 }}
                      />
                    )}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <span style={{ fontSize: '10px', fontWeight: 'bold', backgroundColor: 'rgba(0, 229, 255, 0.15)', color: '#00E5FF', padding: '2px 8px', borderRadius: '4px', alignSelf: 'flex-start', fontFamily: 'monospace' }}>
                        {movieResult.confidence || 'HIGH'} CONFIDENCE
                      </span>
                      <h3 style={{ margin: '4px 0', fontSize: '22px', fontWeight: 'bold', color: '#FFFFFF' }}>{movieResult.title}</h3>
                      <p style={{ margin: 0, fontSize: '13px', color: '#8E93A4' }}>{movieResult.year} • Directed by {movieResult.director}</p>
                      <p style={{ margin: 0, fontSize: '12px', color: '#00E676', fontWeight: '500' }}>{movieResult.genre}</p>
                      {movieResult.imdbUrl && (
                        <a
                          href={movieResult.imdbUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', color: '#FFB300', fontSize: '12px', marginTop: '6px', textDecoration: 'none' }}
                        >
                          <span>Open IMDb Page</span>
                          <ExternalLink style={{ width: '12px', height: '12px' }} />
                        </a>
                      )}
                    </div>
                  </div>

                  <div style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '12px', padding: '14px', fontSize: '12px' }}>
                    <p style={{ margin: '0 0 6px 0', fontSize: '10px', fontWeight: 'bold', color: '#00E5FF', textTransform: 'uppercase', letterSpacing: '1px' }}>
                      AI Recognition Rationale:
                    </p>
                    <p style={{ margin: 0, color: '#8E93A4', lineHeight: '1.6' }}>{movieResult.rationale}</p>
                    {movieResult.actors && (
                      <p style={{ margin: '8px 0 0 0', color: '#F5F6F8' }}>
                        <strong style={{ color: '#00E5FF' }}>Actors identified:</strong> {movieResult.actors}
                      </p>
                    )}
                  </div>
                </div>
              ) : (
                <div style={{ textAlign: 'center', padding: '40px 20px', color: '#8E93A4', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
                  <Film style={{ width: '48px', height: '48px', color: '#222634' }} />
                  <p style={{ margin: 0, fontSize: '14px', fontWeight: 'bold', color: '#FFFFFF' }}>No Movie Identified Yet</p>
                  <p style={{ margin: 0, fontSize: '12px', maxWidth: '300px', lineHeight: '1.5' }}>
                    Take a live camera snapshot of your TV or upload a picture, or enter text clues to identify the movie.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'watchlist' && (
          <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h2 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: '#00E5FF' }}>Shared Watchlist & Discoveries</h2>
            {watchlist.length === 0 ? (
              <p style={{ fontSize: '12px', color: '#8E93A4', textAlign: 'center', padding: '30px 0' }}>No movies saved yet. Identified films will automatically appear here.</p>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '16px' }}>
                {watchlist.map(item => (
                  <div key={item.id} style={{ backgroundColor: '#06080D', border: '1px solid #222634', borderRadius: '12px', padding: '12px', display: 'flex', gap: '12px', alignItems: 'center' }}>
                    {item.poster_path ? (
                      <img src={item.poster_path} alt={item.title} style={{ width: '45px', height: '65px', objectFit: 'cover', borderRadius: '6px' }} />
                    ) : (
                      <div style={{ width: '45px', height: '65px', backgroundColor: '#10121A', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#8E93A4' }}>
                        <Film style={{ width: '20px', height: '20px' }} />
                      </div>
                    )}
                    <div>
                      <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 'bold', color: '#FFFFFF' }}>{item.title}</h4>
                      <p style={{ margin: '4px 0 0 0', fontSize: '11px', color: '#8E93A4' }}>{item.year} • {item.genre}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'dashboard' && session && !session.isGuest && (
          <UserDashboard session={session} onSessionUpdate={handleSessionUpdate} discoveriesCount={watchlist.length} />
        )}

        {activeTab === 'admin' && isAdminRole(session.role) && <AdminPanel session={session} />}
      </main>

      {/* API Key Modal */}
      {showApiKeyModal && (
        <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', zIndex: 100 }}>
          <div style={{ backgroundColor: '#10121A', border: '1px solid #222634', borderRadius: '18px', padding: '24px', maxWidth: '440px', width: '100%', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Key style={{ width: '20px', height: '20px', color: '#00E5FF' }} />
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: '#FFFFFF' }}>Gemini API Key Settings</h3>
            </div>
            <p style={{ margin: 0, fontSize: '12px', color: '#8E93A4', lineHeight: '1.5' }}>
              Enter your Google AI Studio API key. This will be stored locally in your browser and used to power movie identification on localhost or Vercel:
            </p>
            <input
              type="password"
              placeholder="AIzaSy..."
              value={userApiKey}
              onChange={e => setUserApiKey(e.target.value)}
              style={{
                width: '100%',
                backgroundColor: '#06080D',
                border: '1px solid #222634',
                borderRadius: '10px',
                padding: '12px',
                fontSize: '13px',
                color: '#FFFFFF',
                fontFamily: 'monospace',
                outline: 'none'
              }}
            />
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                onClick={() => setShowApiKeyModal(false)}
                style={{ padding: '8px 16px', borderRadius: '8px', fontSize: '12px', color: '#8E93A4', backgroundColor: 'transparent' }}
              >
                Cancel
              </button>
              <button
                onClick={() => saveApiKey(userApiKey)}
                style={{ padding: '8px 16px', borderRadius: '8px', fontSize: '12px', fontWeight: 'bold', backgroundColor: '#00E5FF', color: '#06080D' }}
              >
                Save Key
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
