import { useState } from 'react';
import { usePartners, useCreatePartner, useUpdatePartner } from '../hooks/usePartners';
import PageHeader from '../components/common/PageHeader';
import Card from '../components/common/Card';
import Btn from '../components/common/Btn';
import Modal from '../components/common/Modal';
import Spinner from '../components/common/Spinner';
import EmptyState from '../components/common/EmptyState';
import { Badge, ModeBadge, FileTypeBadge } from '../components/common/StatusBadge';
import { fmtDate } from '../utils/format';
import type { Partner, FileType, ProcessingMode } from '../types';
import { Plus, Edit, Network, Mail, Zap } from 'lucide-react';

export default function PartnersPage() {
  const [formOpen, setFormOpen]         = useState(false);
  const [editTarget, setEditTarget]     = useState<Partner|null>(null);
  const { data: partners = [], isLoading } = usePartners();

  const handleEdit = (p: Partner) => { setEditTarget(p); setFormOpen(true); };
  const handleNew  = () => { setEditTarget(null); setFormOpen(true); };

  return (
    <div className="fade-in">
      <PageHeader
        title="EDI"
        highlight="Partners"
        subtitle="Manage external logistics partners, SFTP/FTP connections, and processing defaults."
        actions={
          <Btn variant="primary" icon={<Plus size={13}/>} onClick={handleNew}>Add Partner</Btn>
        }
      />

      <div style={{ padding:'24px 32px' }}>
        {isLoading ? (
          <div style={{ display:'flex', justifyContent:'center', padding:60 }}><Spinner size={36}/></div>
        ) : partners.length === 0 ? (
          <EmptyState icon="🤝" title="No partners yet" message="Create your first EDI partner to start receiving files."/>
        ) : (
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(340px,1fr))', gap:18 }}>
            {partners.map((p) => (
              <PartnerCard key={p.partnerId} partner={p} onEdit={() => handleEdit(p)}/>
            ))}
          </div>
        )}
      </div>

      <PartnerModal open={formOpen} onClose={() => setFormOpen(false)} partner={editTarget}/>
    </div>
  );
}

// ── Partner Card ──────────────────────────────────────────────────────────────

function PartnerCard({ partner: p, onEdit }: { partner: Partner; onEdit: () => void }) {
  return (
    <div style={{
      background:'var(--card)', border:'1px solid var(--border)', borderRadius:12,
      overflow:'hidden', transition:'border-color .2s',
    }}
      onMouseEnter={(e) => (e.currentTarget.style.borderColor='var(--cyan)')}
      onMouseLeave={(e) => (e.currentTarget.style.borderColor='var(--border)')}
    >
      {/* Header */}
      <div style={{
        padding:'16px 18px', borderBottom:'1px solid var(--border)',
        display:'flex', alignItems:'center', gap:12,
        background:'linear-gradient(135deg,var(--bg2),var(--bg1))',
      }}>
        <div style={{
          width:44, height:44, borderRadius:10,
          background:'linear-gradient(135deg,var(--cyan),var(--purple))',
          display:'flex', alignItems:'center', justifyContent:'center',
          fontFamily:"'Exo 2',sans-serif", fontWeight:900, fontSize:18, color:'#000',
          flexShrink:0,
        }}>
          {p.partnerCode[0]}
        </div>
        <div style={{ flex:1 }}>
          <div style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:700, fontSize:14 }}>
            {p.partnerName}
          </div>
          <div style={{ fontSize:11, fontFamily:"'Fira Code',monospace", color:'var(--cyan)' }}>
            {p.partnerCode}
          </div>
        </div>
        <Badge color={p.active ? 'var(--green)' : 'var(--muted)'}>
          {p.active ? 'Active' : 'Inactive'}
        </Badge>
      </div>

      {/* Details */}
      <div style={{ padding:'14px 18px' }}>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:10, marginBottom:12 }}>
          <div>
            <div style={{ fontSize:9, color:'var(--muted)', textTransform:'uppercase', letterSpacing:'.5px',
              fontFamily:"'Exo 2',sans-serif", fontWeight:700, marginBottom:4 }}>Default Format</div>
            <FileTypeBadge type={p.defaultFormat}/>
          </div>
          <div>
            <div style={{ fontSize:9, color:'var(--muted)', textTransform:'uppercase', letterSpacing:'.5px',
              fontFamily:"'Exo 2',sans-serif", fontWeight:700, marginBottom:4 }}>Processing Mode</div>
            <ModeBadge mode={p.processingMode}/>
          </div>
          <div>
            <div style={{ fontSize:9, color:'var(--muted)', textTransform:'uppercase', letterSpacing:'.5px',
              fontFamily:"'Exo 2',sans-serif", fontWeight:700, marginBottom:4 }}>SLA</div>
            <span style={{ fontSize:12, fontFamily:"'Fira Code',monospace" }}>{p.slaHours}h</span>
          </div>
          {p.contactEmail && (
            <div>
              <div style={{ fontSize:9, color:'var(--muted)', textTransform:'uppercase', letterSpacing:'.5px',
                fontFamily:"'Exo 2',sans-serif", fontWeight:700, marginBottom:4 }}>Contact</div>
              <div style={{ fontSize:11, color:'var(--text2)', display:'flex', alignItems:'center', gap:4 }}>
                <Mail size={11}/> {p.contactEmail}
              </div>
            </div>
          )}
        </div>

        <div style={{ fontSize:10, color:'var(--muted)', fontFamily:"'Fira Code',monospace", marginBottom:12 }}>
          Created {fmtDate(p.createdAt)}
        </div>

        <div style={{ display:'flex', gap:8 }}>
          <Btn size="sm" icon={<Edit size={12}/>} onClick={onEdit}>Edit</Btn>
          <Btn size="sm" icon={<Network size={12}/>} variant="ghost">Test Connection</Btn>
        </div>
      </div>
    </div>
  );
}

// ── Partner Form Modal ────────────────────────────────────────────────────────

function PartnerModal({
  open, onClose, partner,
}: {
  open: boolean; onClose: () => void; partner: Partner|null;
}) {
  const isEdit = !!partner;

  const [code, setCode]           = useState(partner?.partnerCode ?? '');
  const [name, setName]           = useState(partner?.partnerName ?? '');
  const [format, setFormat]       = useState<FileType>(partner?.defaultFormat ?? 'XML');
  const [mode, setMode]           = useState<ProcessingMode>(partner?.processingMode ?? 'AUTO');
  const [sla, setSla]             = useState(String(partner?.slaHours ?? 24));
  const [email, setEmail]         = useState(partner?.contactEmail ?? '');
  const [active, setActive]       = useState(partner?.active ?? true);

  const { mutate: create, isPending: creating } = useCreatePartner();
  const { mutate: update, isPending: updating } = useUpdatePartner();

  const isPending = creating || updating;

  const handleSubmit = () => {
    const payload: Partial<Partner> = {
      partnerCode: code, partnerName: name, defaultFormat: format,
      processingMode: mode, slaHours: Number(sla), contactEmail: email, active,
    };
    if (isEdit) {
      update({ id: partner.partnerId, data: payload }, { onSuccess: onClose });
    } else {
      create(payload, { onSuccess: onClose });
    }
  };

  const labelStyle = {
    display:'block' as const, fontSize:11, fontWeight:700 as const, color:'var(--text2)',
    fontFamily:"'Exo 2',sans-serif", letterSpacing:'.5px' as const,
    textTransform:'uppercase' as const, marginBottom:6,
  };

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? `Edit Partner — ${partner?.partnerCode}` : 'Add New Partner'}>
      <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <div>
            <label style={labelStyle}>Partner Code</label>
            <input value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} placeholder="ACME-001" disabled={isEdit}/>
          </div>
          <div>
            <label style={labelStyle}>Partner Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="ACME Logistics Ltd."/>
          </div>
        </div>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <div>
            <label style={labelStyle}>Default Format</label>
            <select value={format} onChange={(e) => setFormat(e.target.value as FileType)}>
              {['XML','JSON','CSV','TXT','EDIFACT','X12','EXCEL'].map((t) => <option key={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label style={labelStyle}>Processing Mode</label>
            <select value={mode} onChange={(e) => setMode(e.target.value as ProcessingMode)}>
              <option value="AUTO">AUTO</option>
              <option value="MANUAL">MANUAL</option>
              <option value="SCHEDULED">SCHEDULED</option>
            </select>
          </div>
        </div>
        <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
          <div>
            <label style={labelStyle}>SLA (hours)</label>
            <input type="number" value={sla} onChange={(e) => setSla(e.target.value)} min={1} max={168}/>
          </div>
          <div>
            <label style={labelStyle}>Contact Email</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="edi@partner.com"/>
          </div>
        </div>
        <label style={{ display:'flex', alignItems:'center', gap:8, fontSize:12, cursor:'pointer' }}>
          <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} style={{ width:'auto' }}/>
          <span style={{ fontFamily:"'Exo 2',sans-serif", fontWeight:600 }}>Partner Active</span>
        </label>
        <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
          <Btn variant="ghost" onClick={onClose}>Cancel</Btn>
          <Btn variant="primary" loading={isPending} disabled={!code.trim() || !name.trim()}
            icon={<Zap size={13}/>} onClick={handleSubmit}>
            {isEdit ? 'Save Changes' : 'Create Partner'}
          </Btn>
        </div>
      </div>
    </Modal>
  );
}
