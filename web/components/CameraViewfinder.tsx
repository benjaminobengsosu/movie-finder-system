'use client';

import React, { useRef, useState, useEffect } from 'react';
import { Camera, RefreshCw, X, Zap } from 'lucide-react';

interface CameraViewfinderProps {
  onCapture: (base64: string) => void;
  onClose: () => void;
}

export default function CameraViewfinder({ onCapture, onClose }: CameraViewfinderProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [facingMode, setFacingMode] = useState<'environment' | 'user'>('environment');
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);

  useEffect(() => {
    async function startCamera() {
      try {
        if (stream) {
          stream.getTracks().forEach(t => t.stop());
        }
        const s = await navigator.mediaDevices.getUserMedia({
          video: {
            facingMode,
            width: { ideal: 1920 },
            height: { ideal: 1080 }
          }
        });
        setStream(s);
        if (videoRef.current) {
          videoRef.current.srcObject = s;
        }
        setHasPermission(true);
      } catch (err) {
        console.error('Camera access error:', err);
        setHasPermission(false);
      }
    }
    startCamera();

    return () => {
      if (stream) {
        stream.getTracks().forEach(t => t.stop());
      }
    };
  }, [facingMode]);

  const captureFrame = () => {
    if (!videoRef.current) return;
    const video = videoRef.current;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL('image/jpeg', 0.92);
    onCapture(dataUrl);
  };

  const toggleCamera = () => {
    setFacingMode(prev => (prev === 'environment' ? 'user' : 'environment'));
  };

  return (
    <div className="relative w-full h-[450px] bg-black rounded-2xl overflow-hidden border border-electricCyan/30 shadow-2xl flex flex-col justify-between">
      {/* Top HUD */}
      <div className="absolute top-0 left-0 right-0 p-4 bg-gradient-to-b from-black/80 to-transparent z-10 flex items-center justify-between">
        <div className="flex items-center space-x-2">
          <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-pulse" />
          <span className="text-xs font-mono font-bold tracking-widest text-electricCyan">LIVE CINEMA VIEWFINDER</span>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={toggleCamera}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition"
            title="Switch Camera"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            onClick={onClose}
            className="p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition"
            title="Close Camera"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Video Feed */}
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        className="w-full h-full object-cover"
      />

      {/* Target Reticle Overlay */}
      <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
        <div className="w-64 h-40 border-2 border-dashed border-electricCyan/60 rounded-xl relative">
          <span className="absolute -top-3 left-4 bg-black/80 px-2 py-0.5 text-[10px] font-mono text-electricCyan">
            ALIGN FILM SCENE
          </span>
          <div className="absolute top-0 left-0 w-3 h-3 border-t-2 border-l-2 border-electricCyan" />
          <div className="absolute top-0 right-0 w-3 h-3 border-t-2 border-r-2 border-electricCyan" />
          <div className="absolute bottom-0 left-0 w-3 h-3 border-b-2 border-l-2 border-electricCyan" />
          <div className="absolute bottom-0 right-0 w-3 h-3 border-b-2 border-r-2 border-electricCyan" />
        </div>
      </div>

      {/* Bottom Shutter Controls */}
      <div className="absolute bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-black/90 to-transparent flex items-center justify-center z-10">
        <button
          onClick={captureFrame}
          className="flex items-center justify-center w-16 h-16 rounded-full bg-electricCyan text-obsidian shadow-lg shadow-electricCyan/40 hover:scale-105 active:scale-95 transition"
          title="Take Snapshot"
        >
          <Camera className="w-7 h-7" />
        </button>
      </div>

      {hasPermission === false && (
        <div className="absolute inset-0 bg-black/90 flex flex-col items-center justify-center p-6 text-center z-20">
          <Zap className="w-10 h-10 text-alertRed mb-3" />
          <p className="text-sm font-bold text-white mb-1">Camera Permission Required</p>
          <p className="text-xs text-subtleGrey max-w-xs mb-4">
            Please allow browser camera permissions in order to scan movie scenes directly from your screen or TV.
          </p>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-white/10 hover:bg-white/20 rounded-lg text-xs text-white"
          >
            Use File Upload Instead
          </button>
        </div>
      )}
    </div>
  );
}
