import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { useAuthStore } from '../store/authStore';
import Spinner from '../components/common/Spinner';
import { Eye, EyeOff, Zap } from 'lucide-react';

export default function LoginPage() {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [showPwd, setShowPwd]   = useState(false);
  const [error, setError]       = useState('');
  const { setTokens }           = useAuthStore();
  const navigate                = useNavigate();

  const { mutate, isPending } = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      setTokens(data.accessToken, data.refreshToken);
      navigate('/dashboard');
    },
    onError: (err: unknown) => {
      const ax = err as { response?: { status?: number }; message?: string };
      if (!ax.response) {
        setError('Cannot reach the API — ensure the backend is running (default port 8080) and restart the dev server after proxy changes.');
        return;
      }
      setError('Invalid credentials — please try again');
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (!username.trim() || !password.trim()) {
      setError('Username and password are required');
      return;
    }
    mutate({ username, password });
  };

  return (
    <div style={{
      minHeight: '100vh', background: 'var(--bg0)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      position: 'relative', overflow: 'hidden',
    }}>
      {/* Background grid */}
      <div style={{
        position: 'absolute', inset: 0, opacity: .03,
        backgroundImage: 'linear-gradient(var(--border) 1px,transparent 1px),linear-gradient(90deg,var(--border) 1px,transparent 1px)',
        backgroundSize: '40px 40px',
      }} />

      {/* Glow blobs */}
      <div style={{ position:'absolute', top:'20%', left:'10%', width:400, height:400,
        background:'radial-gradient(circle,rgba(0,212,255,.06) 0%,transparent 70%)', pointerEvents:'none' }} />
      <div style={{ position:'absolute', bottom:'15%', right:'8%', width:500, height:500,
        background:'radial-gradient(circle,rgba(139,92,246,.05) 0%,transparent 70%)', pointerEvents:'none' }} />

      {/* Card */}
      <div className="fade-in" style={{
        background: 'var(--bg1)',
        border: '1px solid var(--border)',
        borderRadius: 16, padding: '40px 40px',
        width: 420, position: 'relative', zIndex: 1,
        boxShadow: '0 24px 80px rgba(0,0,0,.6)',
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{
            width: 52, height: 52, margin: '0 auto 14px',
            background: 'linear-gradient(135deg, var(--cyan), var(--purple))',
            borderRadius: 12,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 24, fontWeight: 900, color: '#fff',
            fontFamily: "'Exo 2', sans-serif",
          }}>T</div>
          <h1 style={{ fontSize: 22, fontFamily: "'Exo 2', sans-serif", fontWeight: 900, marginBottom: 6 }}>
            TMS <span style={{ color: 'var(--cyan)' }}>EDI</span> Platform
          </h1>
          <p style={{ fontSize: 12, color: 'var(--muted)' }}>Enterprise Integration Portal — Sign In</p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display:'block', fontSize:11, fontWeight:700, color:'var(--text2)',
              fontFamily:"'Exo 2',sans-serif", letterSpacing:'.5px', textTransform:'uppercase', marginBottom:6 }}>
              Username
            </label>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="admin"
              autoFocus
              style={{ fontSize: 13 }}
            />
          </div>

          <div style={{ marginBottom: 20, position: 'relative' }}>
            <label style={{ display:'block', fontSize:11, fontWeight:700, color:'var(--text2)',
              fontFamily:"'Exo 2',sans-serif", letterSpacing:'.5px', textTransform:'uppercase', marginBottom:6 }}>
              Password
            </label>
            <input
              type={showPwd ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{ fontSize: 13, paddingRight: 40 }}
            />
            <button
              type="button"
              onClick={() => setShowPwd(!showPwd)}
              style={{
                position: 'absolute', right: 10, top: 30,
                background: 'none', border: 'none',
                color: 'var(--muted)', cursor: 'pointer', padding: 4,
              }}
            >
              {showPwd ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>

          {error && (
            <div style={{
              background: 'rgba(239,68,68,.1)', border: '1px solid rgba(239,68,68,.3)',
              color: 'var(--red)', borderRadius: 8, padding: '8px 12px',
              fontSize: 12, marginBottom: 16,
            }}>{error}</div>
          )}

          <button
            type="submit"
            disabled={isPending}
            style={{
              width: '100%', padding: '12px',
              background: 'linear-gradient(135deg, var(--cyan), var(--cyan2))',
              color: '#000', border: 'none', borderRadius: 9,
              fontFamily: "'Exo 2', sans-serif", fontWeight: 700, fontSize: 14,
              letterSpacing: '.5px', cursor: isPending ? 'wait' : 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
              opacity: isPending ? .8 : 1,
              transition: 'all .2s',
            }}
          >
            {isPending ? <Spinner size={16} color="#000" /> : <Zap size={16} />}
            {isPending ? 'Authenticating…' : 'Sign In'}
          </button>
        </form>

        {/* Demo hint */}
        <div style={{
          marginTop: 20, padding: '10px 14px',
          background: 'rgba(0,212,255,.05)', border: '1px solid rgba(0,212,255,.15)',
          borderRadius: 8, fontSize: 11, color: 'var(--muted)',
          fontFamily: "'Fira Code', monospace",
        }}>
          <span style={{ color: 'var(--cyan)' }}>demo:</span> admin / Admin@2026!
        </div>
      </div>
    </div>
  );
}
