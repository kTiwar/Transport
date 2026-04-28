import { useState, useEffect } from 'react';
import { Outlet, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { Menu } from 'lucide-react';
import Sidebar from './Sidebar';
import { useAuthStore } from '../../store/authStore';
import { useLayoutStore } from '../../store/layoutStore';

export default function Layout() {
  const { isAuthenticated } = useAuthStore();
  const { sidebarCollapsed } = useLayoutStore();
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const onResize = () => {
      const mobile = window.innerWidth <= 768;
      setIsMobile(mobile);
      if (!mobile) setMobileOpen(false);
    };
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  const sidebarWidth = sidebarCollapsed
    ? 'var(--sidebar-collapsed-w)'
    : 'var(--sidebar-w)';

  return (
    <div className="layout-root">
      {/* Mobile overlay */}
      {isMobile && mobileOpen && (
        <div
          onClick={() => setMobileOpen(false)}
          style={{
            position: 'fixed', inset: 0,
            background: 'rgba(0,0,0,.55)',
            zIndex: 199,
            backdropFilter: 'blur(2px)',
          }}
        />
      )}

      <Sidebar
        mobileOpen={isMobile && mobileOpen}
        onMobileClose={isMobile ? () => setMobileOpen(false) : undefined}
      />

      <main
        className="layout-main"
        style={{
          marginLeft: isMobile ? 0 : sidebarWidth,
        }}
      >
        {/* Mobile top bar */}
        {isMobile && (
          <div style={{
            position: 'sticky', top: 0, zIndex: 50,
            background: 'var(--bg1)',
            borderBottom: '1px solid var(--border)',
            padding: '10px 16px',
            display: 'flex', alignItems: 'center', gap: 12,
          }}>
            <button
              onClick={() => setMobileOpen(true)}
              style={{
                background: 'rgba(0,212,255,.08)',
                border: '1px solid rgba(0,212,255,.2)',
                color: 'var(--cyan)',
                borderRadius: 8,
                padding: '6px 8px',
                display: 'flex', alignItems: 'center',
              }}
            >
              <Menu size={18} />
            </button>
            <span style={{
              fontFamily: "'Exo 2', sans-serif",
              fontWeight: 700, fontSize: 14,
            }}>
              TMS <span style={{ color: 'var(--cyan)' }}>EDI</span>
            </span>
          </div>
        )}

        <Outlet />
      </main>

      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: 'var(--bg2)',
            color: 'var(--text)',
            border: '1px solid var(--border)',
            fontFamily: "'Nunito', sans-serif",
            fontSize: 13,
          },
          success: { iconTheme: { primary: 'var(--green)', secondary: 'var(--bg0)' } },
          error:   { iconTheme: { primary: 'var(--red)',   secondary: 'var(--bg0)' } },
        }}
      />
    </div>
  );
}
