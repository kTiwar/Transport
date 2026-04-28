import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useLayoutStore } from '../../store/layoutStore';
import {
  LayoutDashboard, Files, Map, AlertTriangle,
  Users, Activity, LogOut, ChevronsLeft, ChevronsRight, X,
  Download, Truck, Building2, Navigation, BrainCircuit, MapPin, Library,
} from 'lucide-react';

const NAV = [
  { to: '/dashboard',     icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/files',         icon: Files,           label: 'EDI Files' },
  { to: '/mappings',      icon: Map,             label: 'Mappings' },
  { to: '/import-orders', icon: Download,        label: 'Import Orders' },
  { to: '/tms-orders',    icon: Truck,           label: 'TMS Orders' },
  { to: '/route-planner', icon: Navigation,      label: 'Route planner' },
  { to: '/planning',      icon: BrainCircuit,    label: 'OR-Tools Planning' },
  { to: '/errors',        icon: AlertTriangle,   label: 'Errors' },
  { to: '/partners',      icon: Users,           label: 'Partners' },
  { to: '/masters', icon: Library, label: 'Master data' },
  { to: '/address-master', icon: MapPin, label: 'Address master' },
  { to: '/company-information', icon: Building2,  label: 'Company' },
  { to: '/monitoring',    icon: Activity,        label: 'Monitoring' },
];

interface SidebarProps {
  mobileOpen?: boolean;
  onMobileClose?: () => void;
}

export default function Sidebar({ mobileOpen = false, onMobileClose }: SidebarProps) {
  const { logout, user } = useAuthStore();
  const { sidebarCollapsed, toggleSidebar } = useLayoutStore();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/login'); };

  const collapsed = sidebarCollapsed;
  const sidebarWidth = collapsed ? 'var(--sidebar-collapsed-w)' : 'var(--sidebar-w)';

  return (
    <aside
      className={`layout-sidebar${mobileOpen ? ' mobile-open' : ''}`}
      style={{
        width: sidebarWidth,
        background: 'var(--bg1)',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* ── Logo + toggle ──────────────────────────────────────────────────── */}
      <div style={{
        padding: collapsed ? '16px 0' : '16px 14px',
        borderBottom: '1px solid var(--border)',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        justifyContent: collapsed ? 'center' : 'space-between',
        minHeight: 64,
      }}>
        {/* Logo icon — always visible */}
        <div style={{
          width: 36, height: 36,
          background: 'linear-gradient(135deg, var(--cyan), var(--purple))',
          borderRadius: 8,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: "'Exo 2', sans-serif",
          fontWeight: 900, fontSize: 18, color: '#fff',
          flexShrink: 0,
        }}>T</div>

        {/* Title — only when expanded */}
        {!collapsed && (
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <div style={{ fontFamily: "'Exo 2', sans-serif", fontWeight: 700, fontSize: 13, lineHeight: 1.2, whiteSpace: 'nowrap' }}>
              TMS <span style={{ color: 'var(--cyan)' }}>EDI</span>
            </div>
            <div style={{ fontSize: 10, color: 'var(--muted)', fontFamily: "'Fira Code', monospace", whiteSpace: 'nowrap' }}>
              Integration Platform
            </div>
          </div>
        )}

        {/* Desktop collapse toggle */}
        <button
          className="sidebar-toggle-btn"
          onClick={onMobileClose ?? toggleSidebar}
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {onMobileClose
            ? <X size={14} />
            : collapsed
              ? <ChevronsRight size={14} />
              : <ChevronsLeft size={14} />
          }
        </button>
      </div>

      {/* ── Nav ───────────────────────────────────────────────────────────── */}
      <nav style={{ flex: 1, padding: collapsed ? '12px 8px' : '12px 10px', overflowY: 'auto', overflowX: 'hidden' }}>
        {NAV.map(({ to, icon: Icon, label }) => (
          <div key={to} className="sidebar-tooltip-wrapper">
            <NavLink
              to={to}
              className={({ isActive }) =>
                `sidebar-nav-item${isActive ? ' active' : ''}`
              }
              style={{ justifyContent: collapsed ? 'center' : 'flex-start' }}
              title={collapsed ? label : undefined}
            >
              <Icon size={16} style={{ flexShrink: 0 }} />
              {!collapsed && <span>{label}</span>}
            </NavLink>
            {collapsed && <span className="tooltip">{label}</span>}
          </div>
        ))}
      </nav>

      {/* ── User + Logout ─────────────────────────────────────────────────── */}
      <div style={{
        padding: collapsed ? '12px 8px' : '14px 18px',
        borderTop: '1px solid var(--border)',
      }}>
        {/* Avatar row */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: collapsed ? 0 : 10,
          justifyContent: collapsed ? 'center' : 'flex-start',
          marginBottom: 10,
          overflow: 'hidden',
        }}>
          <div style={{
            width: 32, height: 32,
            background: 'linear-gradient(135deg, var(--purple), var(--cyan))',
            borderRadius: '50%',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 13, fontWeight: 700, color: '#fff',
            flexShrink: 0,
          }}>
            {user?.username?.[0]?.toUpperCase() ?? 'U'}
          </div>
          {!collapsed && (
            <div style={{ overflow: 'hidden' }}>
              <div style={{ fontSize: 12, fontWeight: 700, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {user?.username}
              </div>
              <div style={{ fontSize: 10, color: 'var(--muted)', fontFamily: "'Fira Code', monospace" }}>
                {user?.roles?.[0]?.replace('ROLE_', '') ?? 'USER'}
              </div>
            </div>
          )}
        </div>

        {/* Logout button */}
        <div className={collapsed ? 'sidebar-tooltip-wrapper' : ''} style={{ display: 'flex', justifyContent: collapsed ? 'center' : 'stretch' }}>
          <button
            onClick={handleLogout}
            title={collapsed ? 'Sign Out' : undefined}
            style={{
              width: collapsed ? 36 : '100%',
              height: collapsed ? 36 : 'auto',
              padding: collapsed ? 0 : '8px 12px',
              background: 'rgba(239,68,68,.1)',
              border: '1px solid rgba(239,68,68,.3)',
              color: 'var(--red)',
              fontSize: 12,
              fontFamily: "'Exo 2', sans-serif",
              display: 'flex', alignItems: 'center',
              justifyContent: 'center',
              gap: collapsed ? 0 : 8,
              borderRadius: 7,
            }}
          >
            <LogOut size={14} />
            {!collapsed && 'Sign Out'}
          </button>
          {collapsed && <span className="tooltip">Sign Out</span>}
        </div>
      </div>
    </aside>
  );
}
