import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from 'react';
import { MapPin, RefreshCw } from 'lucide-react';
import {
  addressMasterApi,
  type AddressMasterSummary,
  type AddressMasterImportResult,
  type AddressMasterUpsertPayload,
} from '../api/addressMasterApi';
import {
  addressLookupApi,
  type LookupCategory,
  type LookupDependencies,
  type LookupOption,
} from '../api/addressLookupApi';
import LookupDialog from '../components/addressMaster/LookupDialog';
import LookupField from '../components/addressMaster/LookupField';

type LookupKey = 'country' | 'state' | 'city' | 'postal' | 'addressType' | 'region' | 'zone';

type FormState = Record<LookupKey, LookupOption | null> & {
  editId: number | null;
  addressCode: string;
  line1: string;
  line2: string;
};

const LOOKUP_META: Record<LookupKey, { label: string; category: LookupCategory }> = {
  country: { label: 'Country', category: 'COUNTRY' },
  state: { label: 'State', category: 'STATE' },
  city: { label: 'City', category: 'CITY' },
  postal: { label: 'Postal Code', category: 'POSTAL_CODE' },
  addressType: { label: 'Address Type', category: 'ADDRESS_TYPE' },
  region: { label: 'Region', category: 'REGION' },
  zone: { label: 'Zone', category: 'ZONE' },
};

const INITIAL_FORM: FormState = {
  editId: null,
  addressCode: '',
  line1: '',
  line2: '',
  country: null,
  state: null,
  city: null,
  postal: null,
  addressType: null,
  region: null,
  zone: null,
};

export default function AddressMasterPage() {
  const [rows, setRows] = useState<AddressMasterSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [importing, setImporting] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | null>(null);

  const [form, setForm] = useState<FormState>(INITIAL_FORM);
  const [openLookup, setOpenLookup] = useState<LookupKey | null>(null);
  const [lookupRows, setLookupRows] = useState<LookupOption[]>([]);
  const [lookupLoading, setLookupLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const r = await addressMasterApi.list(0, 20, undefined);
      setRows(r.content);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const loadLookup = useCallback(
    async (key: LookupKey, q = '') => {
      const category = LOOKUP_META[key].category;
      setLookupLoading(true);
      try {
        const parentCode =
          key === 'state' ? form.country?.code :
          key === 'city' ? form.state?.code :
          key === 'postal' ? form.city?.code :
          key === 'region' ? form.country?.code :
          key === 'zone' ? form.region?.code :
          undefined;

        const list = await addressLookupApi.options({ category, q, parentCode, limit: 120 });
        setLookupRows(list);
      } finally {
        setLookupLoading(false);
      }
    },
    [form.country?.code, form.state?.code, form.city?.code, form.region?.code],
  );

  const resolveDependencies = useCallback(async (selected: LookupOption) => {
    if (selected.category === 'COUNTRY') {
      setForm((prev) => ({ ...prev, country: selected, state: null, city: null, postal: null, region: null, zone: null }));
      return;
    }

    const deps: LookupDependencies = await addressLookupApi.dependencies(selected.category, selected.code);
    setForm((prev) => ({
      ...prev,
      country: deps.country ?? prev.country,
      state: deps.state ?? prev.state,
      city: deps.city ?? prev.city,
      postal: deps.postal ?? prev.postal,
      region: deps.region ?? prev.region,
      zone: deps.zone ?? prev.zone,
      ...(selected.category === 'ADDRESS_TYPE' ? { addressType: selected } : {}),
      ...(selected.category === 'STATE' ? { state: selected, city: null, postal: null } : {}),
      ...(selected.category === 'CITY' ? { city: selected, postal: null } : {}),
      ...(selected.category === 'POSTAL_CODE' ? { postal: selected } : {}),
      ...(selected.category === 'REGION' ? { region: selected, zone: null } : {}),
      ...(selected.category === 'ZONE' ? { zone: selected } : {}),
    }));
  }, []);

  const onSelectLookup = async (opt: LookupOption) => {
    if (!openLookup) return;
    setOpenLookup(null);

    if (openLookup === 'addressType') {
      setForm((prev) => ({ ...prev, addressType: opt }));
      return;
    }

    await resolveDependencies(opt);
  };

  const toPayload = (): AddressMasterUpsertPayload => ({
    addressCode: form.addressCode.trim(),
    addressType: form.addressType?.code ?? null,
    entityType: null,
    entityId: null,
    addressLine1: form.line1.trim() || null,
    addressLine2: form.line2.trim() || null,
    addressLine3: null,
    landmark: null,
    city: form.city?.name ?? null,
    district: null,
    stateProvince: form.state?.name ?? null,
    postalCode: form.postal?.code ?? null,
    countryCode: form.country?.code ?? null,
    countryName: form.country?.name ?? null,
    latitude: null,
    longitude: null,
    timezone: null,
    isPrimary: false,
    isActive: true,
    validationStatus: null,
  });

  const onSave = async () => {
    if (!form.addressCode.trim()) {
      setFieldError('addressCode');
      setMsg('Address code is required.');
      return;
    }
    if (!form.addressType || !form.country || !form.state || !form.city || !form.postal) {
      setFieldError('addressType');
      setMsg('Select Address Type, Country, State, City and Postal Code.');
      return;
    }

    try {
      if (form.editId) {
        await addressMasterApi.update(form.editId, toPayload());
        setMsg('Address updated successfully.');
      } else {
        await addressMasterApi.create(toPayload());
        setMsg('Address created successfully.');
      }
      setFieldError(null);
      setForm(INITIAL_FORM);
      await load();
    } catch (err: any) {
      setFieldError(err?.response?.data?.field || null);
      setMsg(err?.response?.data?.message || err?.message || 'Save failed');
    }
  };

  const onActivate = async (addressId: number) => {
    try {
      await addressMasterApi.activate(addressId);
      setMsg('Address activated.');
      await load();
    } catch (err: any) {
      setMsg(err?.response?.data?.message || err?.message || 'Activate failed');
    }
  };

  const onDeactivate = async (addressId: number) => {
    try {
      await addressMasterApi.deactivate(addressId);
      setMsg('Address deactivated.');
      await load();
    } catch (err: any) {
      setMsg(err?.response?.data?.message || err?.message || 'Deactivate failed');
    }
  };

  const onDelete = async (addressId: number) => {
    if (!window.confirm('Delete this address record?')) return;
    try {
      await addressMasterApi.remove(addressId);
      if (form.editId === addressId) {
        setForm(INITIAL_FORM);
      }
      setMsg('Address deleted.');
      await load();
    } catch (err: any) {
      setMsg(err?.response?.data?.message || err?.message || 'Delete failed');
    }
  };

  const onEdit = async (addressId: number) => {
    try {
      const d = await addressMasterApi.get(addressId);
      const country = d.countryCode ? await addressLookupApi.byCode('COUNTRY', d.countryCode).catch(() => null) : null;
      const state = d.stateProvince
        ? await addressLookupApi.options({ category: 'STATE', q: d.stateProvince, parentCode: country?.code, limit: 20 }).then((x) => x[0] ?? null).catch(() => null)
        : null;
      const city = d.city
        ? await addressLookupApi.options({ category: 'CITY', q: d.city, parentCode: state?.code, limit: 20 }).then((x) => x[0] ?? null).catch(() => null)
        : null;
      const postal = d.postalCode ? await addressLookupApi.byCode('POSTAL_CODE', d.postalCode).catch(() => null) : null;
      const addressType = d.addressType ? await addressLookupApi.byCode('ADDRESS_TYPE', d.addressType).catch(() => null) : null;

      setForm({
        editId: d.addressId,
        addressCode: d.addressCode || '',
        line1: d.addressLine1 || '',
        line2: d.addressLine2 || '',
        country,
        state,
        city,
        postal,
        addressType,
        region: null,
        zone: null,
      });
      setMsg(`Editing ${d.addressCode}`);
    } catch (err: any) {
      setMsg(err?.response?.data?.message || err?.message || 'Edit load failed');
    }
  };

  const onPickExcel = async (e: ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    e.target.value = '';
    if (!f) return;
    setImporting(true);
    setMsg(null);
    try {
      const r: AddressMasterImportResult = await addressMasterApi.importExcel(f);
      setMsg(`Rows ${r.rowsRead}, inserted ${r.inserted}, updated ${r.updated}, skipped ${r.skipped}. ${r.errors?.join(' ') || ''}`);
      await load();
    } catch (err: any) {
      setMsg(err?.response?.data?.message || err?.message || 'Import failed');
    } finally {
      setImporting(false);
    }
  };

  const openTitle = useMemo(() => {
    if (!openLookup) return '';
    return `${LOOKUP_META[openLookup].label} lookup`;
  }, [openLookup]);

  return (
    <div style={{ padding: '24px 28px', fontFamily: "'Exo 2',sans-serif" }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
        <MapPin size={20} color="var(--green)" />
        <h1 style={{ margin: 0, fontSize: 20 }}>Address Master</h1>
      </div>

      <div style={{ border: '1px solid var(--border)', borderRadius: 10, background: 'var(--bg2)', padding: 16, marginBottom: 16 }}>
        <h2 style={{ margin: '0 0 12px', fontSize: 15 }}>Address Master Page (Business Central style)</h2>
        <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
          <button type="button" style={btn} onClick={onSave}>{form.editId ? 'Update Address' : 'Create Address'}</button>
          <button type="button" style={btn} onClick={() => setForm(INITIAL_FORM)}>Clear Form</button>
        </div>
        <div style={{ display: 'grid', gap: 12, gridTemplateColumns: 'repeat(auto-fit,minmax(280px,1fr))' }}>
          <div>
            <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Address Code *</div>
            <input value={form.addressCode} onChange={(e) => setForm((p) => ({ ...p, addressCode: e.target.value }))} style={{ ...textInput, border: fieldError === 'addressCode' ? '1px solid #ef4444' : textInput.border }} />
          </div>
          <LookupField label="Address Type" value={form.addressType} required onOpen={() => { setOpenLookup('addressType'); loadLookup('addressType'); }} onClear={() => setForm((p) => ({ ...p, addressType: null }))} />
          <LookupField label="Country" value={form.country} required onOpen={() => { setOpenLookup('country'); loadLookup('country'); }} onClear={() => setForm((p) => ({ ...p, country: null, state: null, city: null, postal: null, region: null, zone: null }))} />
          <LookupField label="State" value={form.state} required disabled={!form.country} onOpen={() => { setOpenLookup('state'); loadLookup('state'); }} onClear={() => setForm((p) => ({ ...p, state: null, city: null, postal: null }))} />
          <LookupField label="City" value={form.city} required disabled={!form.state} onOpen={() => { setOpenLookup('city'); loadLookup('city'); }} onClear={() => setForm((p) => ({ ...p, city: null, postal: null }))} />
          <LookupField label="Postal Code" value={form.postal} required disabled={!form.city} onOpen={() => { setOpenLookup('postal'); loadLookup('postal'); }} onClear={() => setForm((p) => ({ ...p, postal: null }))} />
          <LookupField label="Region" value={form.region} onOpen={() => { setOpenLookup('region'); loadLookup('region'); }} onClear={() => setForm((p) => ({ ...p, region: null, zone: null }))} />
          <LookupField label="Zone" value={form.zone} disabled={!form.region} onOpen={() => { setOpenLookup('zone'); loadLookup('zone'); }} onClear={() => setForm((p) => ({ ...p, zone: null }))} />
          <div>
            <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Address Line 1</div>
            <input value={form.line1} onChange={(e) => setForm((p) => ({ ...p, line1: e.target.value }))} style={textInput} />
          </div>
          <div>
            <div style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 4 }}>Address Line 2</div>
            <input value={form.line2} onChange={(e) => setForm((p) => ({ ...p, line2: e.target.value }))} style={textInput} />
          </div>
        </div>
        {!form.editId ? (
          <div
            style={{
              display: 'flex',
              gap: 8,
              marginTop: 16,
              paddingTop: 14,
              borderTop: '1px solid var(--border)',
              flexWrap: 'wrap',
            }}
          >
            <button type="button" style={btn} onClick={onSave}>Save</button>
            <button type="button" style={btn} onClick={() => setForm(INITIAL_FORM)}>Clear Form</button>
          </div>
        ) : null}
      </div>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 12 }}>
        <button type="button" style={btn} onClick={load}><RefreshCw size={12} /> Refresh list</button>
        <button type="button" style={btn} onClick={async () => {
          const blob = await addressMasterApi.downloadImportTemplate();
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'address-master-import-template.xlsx';
          a.click();
          URL.revokeObjectURL(url);
        }}>Download template</button>
        <input id="addr-xlsx-inp" type="file" accept=".xlsx" style={{ display: 'none' }} onChange={onPickExcel} />
        <label htmlFor="addr-xlsx-inp" style={{ ...btn, cursor: importing ? 'wait' : 'pointer', opacity: importing ? 0.6 : 1 }}>{importing ? 'Importing...' : 'Import Excel'}</label>
      </div>
      {msg ? <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 10 }}>{msg}</div> : null}

      <div style={{ border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden', background: 'var(--bg2)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead>
            <tr style={{ background: 'var(--bg3)', borderBottom: '1px solid var(--border)' }}>
              {['Code', 'Type', 'City', 'Postal', 'Country', 'Status', 'Action'].map((h) => <th key={h} style={th}>{h}</th>)}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} style={td}>Loading...</td></tr>
            ) : rows.length === 0 ? (
              <tr><td colSpan={7} style={td}>No address records found.</td></tr>
            ) : (
              rows.map((r) => (
                <tr key={r.addressId}>
                  <td style={td}>{r.addressCode}</td>
                  <td style={td}>{r.addressType || '-'}</td>
                  <td style={td}>{r.city || '-'}</td>
                  <td style={td}>{r.postalCode || '-'}</td>
                  <td style={td}>{r.countryCode || '-'}</td>
                  <td style={td}>{r.isActive ? 'Active' : 'Inactive'}</td>
                  <td style={td}>
                    <button type="button" style={btn} onClick={() => onEdit(r.addressId)}>Edit</button>
                    {r.isActive ? (
                      <button type="button" style={{ ...btn, marginLeft: 6 }} onClick={() => onDeactivate(r.addressId)}>Deactivate</button>
                    ) : (
                      <button type="button" style={{ ...btn, marginLeft: 6 }} onClick={() => onActivate(r.addressId)}>Activate</button>
                    )}
                    <button type="button" style={{ ...btn, marginLeft: 6 }} onClick={() => onDelete(r.addressId)}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <LookupDialog
        title={openTitle}
        open={!!openLookup}
        options={lookupRows}
        loading={lookupLoading}
        onClose={() => setOpenLookup(null)}
        onSearch={(q) => openLookup && loadLookup(openLookup, q)}
        onSelect={onSelectLookup}
      />
    </div>
  );
}

const textInput: React.CSSProperties = {
  width: '100%',
  border: '1px solid var(--border)',
  background: 'var(--bg3)',
  color: 'var(--text)',
  borderRadius: 6,
  padding: '8px 10px',
  fontSize: 12,
};

const btn: React.CSSProperties = {
  border: '1px solid var(--border)',
  background: 'var(--bg3)',
  color: 'var(--text)',
  borderRadius: 6,
  padding: '6px 10px',
  fontSize: 12,
  display: 'inline-flex',
  alignItems: 'center',
  gap: 6,
};

const th: React.CSSProperties = {
  textAlign: 'left',
  padding: '9px 10px',
};

const td: React.CSSProperties = {
  padding: '9px 10px',
  borderTop: '1px solid var(--border)',
};