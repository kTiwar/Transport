import { Link } from 'react-router-dom';
import { Library, MapPin, Users, ArrowRight } from 'lucide-react';

const REF_CATS: { code: string; label: string; hint: string }[] = [
  { code: 'CURRENCY', label: 'Currencies', hint: 'Billing & FX' },
  { code: 'INCOTERM', label: 'Incoterms', hint: 'Delivery terms' },
  { code: 'TRANSPORT_MODE', label: 'Transport modes', hint: 'FTL, LTL, …' },
  { code: 'SERVICE_LEVEL', label: 'Service levels', hint: 'STD, express, …' },
  { code: 'EQUIPMENT_TYPE', label: 'Equipment types', hint: 'Dry, reefer, …' },
  { code: 'LOCATION_TYPE', label: 'Location types', hint: 'WH, hub, port' },
  { code: 'CHARGE_TYPE', label: 'Charge types', hint: 'Fuel, toll, …' },
  { code: 'DOCUMENT_TYPE', label: 'Document types', hint: 'CMR, POD, …' },
  { code: 'UOM', label: 'Units of measure', hint: 'KG, PAL, …' },
];

export default function MastersHubPage() {
  return (
    <div style={{ padding: '24px 28px', fontFamily: "'Exo 2',sans-serif" }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
        <Library size={22} color="var(--green)" />
        <h1 style={{ margin: 0, fontSize: 20, color: 'var(--text)', fontWeight: 700 }}>Master data</h1>
      </div>
      <p style={{ margin: '0 0 24px', color: 'var(--muted)', fontSize: 13, maxWidth: 800 }}>
        Reference lists and parties used across TMS, EDI, and routing. Keep codes stable for integrations.
      </p>

      <h2 style={{ fontSize: 13, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 12 }}>
        Core
      </h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: 12, marginBottom: 28 }}>
        <Link
          to="/address-master"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: 14,
            borderRadius: 10,
            border: '1px solid var(--border)',
            background: 'var(--bg2)',
            textDecoration: 'none',
            color: 'var(--text)',
          }}
        >
          <MapPin size={18} color="#10b981" />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 14 }}>Address master</div>
            <div style={{ fontSize: 11, color: 'var(--muted)' }}>Locations, geo, contacts</div>
          </div>
          <ArrowRight size={14} color="var(--muted)" />
        </Link>
        <Link
          to="/masters/parties"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: 14,
            borderRadius: 10,
            border: '1px solid var(--border)',
            background: 'var(--bg2)',
            textDecoration: 'none',
            color: 'var(--text)',
          }}
        >
          <Users size={18} color="var(--cyan)" />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 14 }}>Parties</div>
            <div style={{ fontSize: 11, color: 'var(--muted)' }}>Customers, carriers, suppliers, agents</div>
          </div>
          <ArrowRight size={14} color="var(--muted)" />
        </Link>
      </div>

      <h2 style={{ fontSize: 13, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: '.06em', marginBottom: 12 }}>
        Reference lists
      </h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 10 }}>
        {REF_CATS.map((c) => (
          <Link
            key={c.code}
            to={`/masters/ref/${c.code}`}
            style={{
              padding: '12px 14px',
              borderRadius: 8,
              border: '1px solid var(--border)',
              background: 'var(--bg3)',
              textDecoration: 'none',
              color: 'var(--text)',
              fontSize: 13,
            }}
          >
            <div style={{ fontWeight: 600 }}>{c.label}</div>
            <div style={{ fontSize: 10, color: 'var(--muted)', marginTop: 4 }}>{c.hint}</div>
          </Link>
        ))}
      </div>
    </div>
  );
}
