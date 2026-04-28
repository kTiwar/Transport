import { useMonitoringStats } from '../hooks/useMonitoring';
import { useFiles } from '../hooks/useFiles';
import Card from '../components/common/Card';
import PageHeader from '../components/common/PageHeader';
import Spinner from '../components/common/Spinner';
import { StatusBadge, ModeBadge, FileTypeBadge } from '../components/common/StatusBadge';
import { fmtDate, fmtNumber, fmtPct, fmtBytes } from '../utils/format';
import { useNavigate } from 'react-router-dom';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from 'recharts';
import {
  FileCheck, AlertTriangle, Clock, TrendingUp,
  Cpu, Activity, Users, ArrowRight,
} from 'lucide-react';

// ── KPI Card ──────────────────────────────────────────────────────────────────

function KpiCard({
  label, value, delta, color, icon: Icon, suffix = '',
}: {
  label: string; value: string | number; delta?: string;
  color: string; icon: React.ElementType; suffix?: string;
}) {
  return (
    <div style={{
      background: 'var(--card)',
      border: `1px solid var(--border)`,
      borderTop: `3px solid ${color}`,
      borderRadius: 12, padding: '20px 22px',
    }}>
      <div style={{ display:'flex', alignItems:'flex-start', justifyContent:'space-between', marginBottom:14 }}>
        <div style={{ fontSize:11, fontWeight:700, color:'var(--muted)', textTransform:'uppercase',
          letterSpacing:'.6px', fontFamily:"'Exo 2',sans-serif" }}>{label}</div>
        <div style={{ width:34,height:34,borderRadius:9,background:`${color}18`,
          display:'flex',alignItems:'center',justifyContent:'center',color }}>
          <Icon size={16}/>
        </div>
      </div>
      <div style={{ fontFamily:"'Exo 2',sans-serif", fontSize:32, fontWeight:900, color, lineHeight:1 }}>
        {fmtNumber(value as number)}{suffix}
      </div>
      {delta && <div style={{ fontSize:11, color:'var(--muted)', marginTop:8 }}>{delta}</div>}
    </div>
  );
}

// ── Throughput mock data ───────────────────────────────────────────────────────

const HOURS = ['20h','21h','22h','23h','0h','1h','2h','3h','4h','5h','6h','7h','8h'];
const throughputData = HOURS.map((h, i) => ({
  hour: h,
  processed: [142,198,287,342,419,387,456,501,478,423,389,311,250][i],
  errors:    [3,5,2,8,4,6,3,9,5,2,7,4,6][i],
}));

const PIE_COLORS = ['var(--cyan)','var(--green)','var(--purple)','var(--orange)','var(--yellow)','var(--muted)'];

// ── Chart tooltip ─────────────────────────────────────────────────────────────

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div style={{ background:'var(--bg2)', border:'1px solid var(--border)', borderRadius:8, padding:'10px 14px' }}>
      <p style={{ fontSize:11, color:'var(--muted)', marginBottom:6, fontFamily:"'Fira Code',monospace" }}>{label}</p>
      {payload.map((p: any) => (
        <p key={p.name} style={{ fontSize:12, color:p.color ?? p.fill, fontWeight:600 }}>
          {p.name}: <span style={{color:'var(--text)'}}>{fmtNumber(p.value)}</span>
        </p>
      ))}
    </div>
  );
};

// ── Main component ─────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const { data: stats, isLoading: statsLoading } = useMonitoringStats();
  const { data: filesPage, isLoading: filesLoading } = useFiles(0, 8);
  const navigate = useNavigate();

  const recentFiles = filesPage?.content ?? [];

  return (
    <div className="fade-in">
      <PageHeader
        title="Operations"
        highlight="Dashboard"
        subtitle="Real-time view of EDI file processing, partner activity, and system health."
      />

      <div style={{ padding: '24px 32px' }}>

        {/* KPI Row */}
        <div style={{ display:'grid', gridTemplateColumns:'repeat(4,1fr)', gap:20, marginBottom:28 }}>
          {statsLoading ? (
            <div style={{ gridColumn:'span 4', display:'flex', justifyContent:'center', padding:40 }}>
              <Spinner />
            </div>
          ) : (
            <>
              <KpiCard label="Files Received" icon={TrendingUp}
                value={stats?.totalFilesReceived ?? 0} color="var(--cyan)"
                delta="Total in system" />
              <KpiCard label="Files Processed" icon={FileCheck}
                value={stats?.totalFilesProcessed ?? 0} color="var(--green)"
                delta={`${fmtPct(stats?.successRatePercent ?? 0)} success rate`} />
              <KpiCard label="Failed Files" icon={AlertTriangle}
                value={stats?.totalFilesFailed ?? 0} color="var(--red)"
                delta={`${stats?.openErrors ?? 0} open errors`} />
              <KpiCard label="Pending Files" icon={Clock}
                value={stats?.totalFilesPending ?? 0} color="var(--yellow)"
                delta="Awaiting processing" />
            </>
          )}
        </div>

        {/* Charts Row */}
        <div style={{ display:'grid', gridTemplateColumns:'2fr 1fr', gap:20, marginBottom:28 }}>

          {/* Throughput Chart */}
          <Card title="Processing Throughput (Last 13h)" icon={<Activity size={14}/>}>
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={throughputData}>
                <defs>
                  <linearGradient id="gradCyan" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"   stopColor="var(--cyan)"  stopOpacity={0.3}/>
                    <stop offset="95%"  stopColor="var(--cyan)"  stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="gradRed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%"   stopColor="var(--red)"   stopOpacity={0.3}/>
                    <stop offset="95%"  stopColor="var(--red)"   stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="hour" tick={{ fill:'var(--muted)', fontSize:10, fontFamily:"'Fira Code'" }} />
                <YAxis tick={{ fill:'var(--muted)', fontSize:10 }} />
                <Tooltip content={<CustomTooltip/>}/>
                <Area type="monotone" dataKey="processed" stroke="var(--cyan)"
                  fill="url(#gradCyan)" strokeWidth={2} name="Processed" />
                <Area type="monotone" dataKey="errors" stroke="var(--red)"
                  fill="url(#gradRed)" strokeWidth={2} name="Errors" />
              </AreaChart>
            </ResponsiveContainer>
          </Card>

          {/* Partner Volume */}
          <Card title="Partner Volume" icon={<Users size={14}/>}>
            {stats?.partnerVolumes?.length ? (
              <>
                <ResponsiveContainer width="100%" height={180}>
                  <PieChart>
                    <Pie data={stats.partnerVolumes} dataKey="fileCount"
                      nameKey="partnerCode" cx="50%" cy="50%"
                      outerRadius={70} innerRadius={40}
                      paddingAngle={3} strokeWidth={0}>
                      {stats.partnerVolumes.map((_, i) => (
                        <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip content={<CustomTooltip/>}/>
                  </PieChart>
                </ResponsiveContainer>
                <div style={{ display:'flex', flexDirection:'column', gap:6 }}>
                  {stats.partnerVolumes.slice(0,4).map((p, i) => (
                    <div key={p.partnerCode} style={{ display:'flex', alignItems:'center', gap:8, fontSize:11 }}>
                      <div style={{ width:8,height:8,borderRadius:2,background:PIE_COLORS[i],flexShrink:0 }} />
                      <span style={{ flex:1, color:'var(--text2)' }}>{p.partnerCode}</span>
                      <span style={{ fontFamily:"'Fira Code',monospace", color:'var(--muted)' }}>
                        {fmtNumber(p.fileCount)}
                      </span>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div style={{ height:220, display:'flex', alignItems:'center', justifyContent:'center', color:'var(--muted)', fontSize:12 }}>
                No data yet
              </div>
            )}
          </Card>
        </div>

        {/* Error Types + Recent Files */}
        <div style={{ display:'grid', gridTemplateColumns:'1fr 2fr', gap:20 }}>

          {/* Error distribution */}
          <Card title="Error Distribution" icon={<AlertTriangle size={14}/>} accent="var(--red)">
            {stats?.errorTypeCounts?.length ? (
              <div style={{ display:'flex', flexDirection:'column', gap:10 }}>
                {stats.errorTypeCounts.slice(0,6).map((e) => (
                  <div key={e.errorType}>
                    <div style={{ display:'flex', justifyContent:'space-between', fontSize:11, marginBottom:4 }}>
                      <span style={{ color:'var(--text2)' }}>
                        {e.errorType.replace(/_/g,' ')}
                      </span>
                      <span style={{ fontFamily:"'Fira Code',monospace", color:'var(--muted)' }}>
                        {e.count}
                      </span>
                    </div>
                    <div style={{ height:5, background:'var(--bg3)', borderRadius:3, overflow:'hidden' }}>
                      <div style={{
                        height:'100%', width:`${e.percentage}%`,
                        background:'linear-gradient(90deg,var(--red),var(--orange))',
                        borderRadius:3, transition:'width 1s ease',
                      }} />
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding:'20px 0', textAlign:'center', color:'var(--green)', fontSize:13 }}>
                ✓ No errors!
              </div>
            )}
          </Card>

          {/* Recent Files */}
          <Card title="Recent Files" icon={<Cpu size={14}/>} noPad>
            {filesLoading ? (
              <div style={{ padding:30, display:'flex', justifyContent:'center' }}><Spinner/></div>
            ) : (
              <>
                <table style={{ width:'100%', borderCollapse:'collapse', fontSize:12 }}>
                  <thead>
                    <tr>
                      {['#','File','Partner','Type','Mode','Status','Received'].map((h) => (
                        <th key={h} style={{
                          padding:'10px 14px', textAlign:'left',
                          borderBottom:'1px solid var(--border)',
                          fontSize:10, fontWeight:700, letterSpacing:'.6px',
                          color:'var(--cyan)', textTransform:'uppercase',
                          fontFamily:"'Exo 2',sans-serif",
                          background:'var(--bg2)',
                        }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {recentFiles.map((f) => (
                      <tr
                        key={f.entryNo}
                        onClick={() => navigate(`/files/${f.entryNo}`)}
                        style={{ cursor:'pointer' }}
                        onMouseEnter={(e) => (e.currentTarget.style.background='rgba(255,255,255,.02)')}
                        onMouseLeave={(e) => (e.currentTarget.style.background='transparent')}
                      >
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)', color:'var(--muted)',fontFamily:"'Fira Code',monospace" }}>
                          {f.entryNo}
                        </td>
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)', maxWidth:160, overflow:'hidden',textOverflow:'ellipsis',whiteSpace:'nowrap' }}>
                          {f.fileName}
                        </td>
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)', color:'var(--text2)' }}>
                          {f.partnerCode}
                        </td>
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)' }}>
                          <FileTypeBadge type={f.fileType}/>
                        </td>
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)' }}>
                          <ModeBadge mode={f.processingMode}/>
                        </td>
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)' }}>
                          <StatusBadge status={f.status}/>
                        </td>
                        <td style={{ padding:'9px 14px', borderBottom:'1px solid var(--border)', color:'var(--muted)', fontSize:11, fontFamily:"'Fira Code',monospace" }}>
                          {fmtDate(f.receivedTimestamp)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div style={{ padding:'10px 14px', borderTop:'1px solid var(--border)', display:'flex', justifyContent:'flex-end' }}>
                  <button
                    onClick={() => navigate('/files')}
                    style={{
                      background:'none', border:'none', color:'var(--cyan)',
                      fontSize:12, fontFamily:"'Exo 2',sans-serif", fontWeight:700,
                      cursor:'pointer', display:'flex', alignItems:'center', gap:6,
                    }}
                  >
                    View all files <ArrowRight size={13}/>
                  </button>
                </div>
              </>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
