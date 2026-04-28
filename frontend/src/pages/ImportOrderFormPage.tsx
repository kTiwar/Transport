import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { importOrdersApi, ImportOrder } from '../api/importOrders';
import { ArrowLeft, Save, Loader2 } from 'lucide-react';

const TABS = ['General', 'Customer', 'Bill-to', 'Transport', 'Dates', 'Additional'];
const TRANSACTION_TYPES = ['INSERT_ORDER','UPDATE_ORDER','CANCEL_ORDER','INSERT_LINE','UPDATE_LINE','INSERT_CARGO','UPDATE_CARGO','INSERT_REFERENCE','UPDATE_REFERENCE','FINALIZE_ORDER'];
const STATUSES = ['READY_TO_PROCESS','RECEIVED','PROCESSED','ERROR','VOID'];
const SOURCES = ['ORDER_ENTRY','TRADELANE','COPY_ORDER','ORDER_IMPORT','API','IC_SYNC'];
const COD_TYPES = ['NONE','FULL_AMOUNT','TRANSPORT_COST'];
const TRACTION_ORDERS = ['','IMPORT','EXPORT','GENERAL'];

const inputStyle = (disabled?: boolean): React.CSSProperties => ({
  width: '100%', boxSizing: 'border-box' as const,
  background: disabled ? 'var(--bg3)' : 'var(--bg2)',
  border: '1px solid var(--border)', borderRadius: 6,
  padding: '7px 10px', color: 'var(--text)', fontSize: 12, outline: 'none',
  opacity: disabled ? 0.6 : 1,
});

const labelStyle: React.CSSProperties = {
  display: 'block', fontSize: 10, color: 'var(--muted)',
  textTransform: 'uppercase', letterSpacing: '.5px', marginBottom: 4,
};

const EMPTY: Partial<ImportOrder> = {
  communicationPartner: '', externalOrderNo: '',
  transactionType: 'INSERT_ORDER', status: 'READY_TO_PROCESS',
  source: 'ORDER_ENTRY', customerName: '', billToCustomerNo: '',
};

export default function ImportOrderFormPage() {
  const { entryNo } = useParams<{ entryNo: string }>();
  const navigate = useNavigate();
  const isEdit = !!entryNo;

  const [form, setForm] = useState<Partial<ImportOrder>>(EMPTY);
  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [toast, setToast] = useState<{msg:string;ok:boolean}|null>(null);
  const [transportTypeOptions, setTransportTypeOptions] = useState<string[]>([]);

  const showToast = (msg: string, ok = true) => {
    setToast({msg,ok});
    setTimeout(() => setToast(null), 3500);
  };

  useEffect(() => {
    if (!isEdit || !entryNo) return;
    importOrdersApi.get(parseInt(entryNo))
      .then(o => setForm(o))
      .finally(() => setLoading(false));
  }, [isEdit, entryNo]);

  useEffect(() => {
    const partner = (form.communicationPartner ?? '').trim();
    if (!partner) {
      setTransportTypeOptions([]);
      return;
    }
    importOrdersApi.getTransportTypes(partner)
      .then((values) => {
        const selected = (form.transportType ?? '').trim();
        const hasSelected = selected.length > 0 && values.some(v => v.toLowerCase() === selected.toLowerCase());
        setTransportTypeOptions(hasSelected ? values : (selected ? [selected, ...values] : values));
      })
      .catch(() => setTransportTypeOptions([]));
  }, [form.communicationPartner]);

  const set = (name: keyof ImportOrder, value: any) =>
    setForm(prev => ({...prev, [name]: value}));

  const handleSubmit = async () => {
    if (!form.communicationPartner || !form.externalOrderNo) {
      showToast('Communication Partner and External Order No. are required', false);
      return;
    }
    setSaving(true);
    try {
      if (isEdit && entryNo) {
        await importOrdersApi.update(parseInt(entryNo), form as ImportOrder);
        showToast('Order updated successfully');
        setTimeout(() => navigate(`/import-orders/${entryNo}`), 800);
      } else {
        const created = await importOrdersApi.create(form) as any;
        showToast('Order created successfully');
        setTimeout(() => navigate(`/import-orders/${created.entryNo || ''}`), 800);
      }
    } catch (err: any) {
      showToast(err?.response?.data?.message || (isEdit ? 'Update failed' : 'Create failed'), false);
    } finally { setSaving(false); }
  };

  // --- field helpers ---
  const TF = (name: keyof ImportOrder, label: string, type: string = 'text', disabled?: boolean, required?: boolean) => (
    <div style={{padding:'6px 0'}}>
      <label style={labelStyle}>{label}{required && <span style={{color:'#ef4444'}}> *</span>}</label>
      <input type={type} value={(form as any)[name] ?? ''} disabled={disabled}
        onChange={e => set(name, type==='number' ? (e.target.value===''?'':Number(e.target.value)) : e.target.value)}
        style={inputStyle(disabled)} />
    </div>
  );

  const CB = (name: keyof ImportOrder, label: string) => (
    <label style={{display:'flex',alignItems:'center',gap:8,cursor:'pointer',fontSize:12,color:'var(--text2)',padding:'8px 0'}}>
      <input type="checkbox" checked={!!(form as any)[name]}
        onChange={e => set(name, e.target.checked)}
        style={{width:14,height:14,accentColor:'var(--cyan)'}} />
      {label}
    </label>
  );

  const SEL = (name: keyof ImportOrder, label: string, options: string[], required?: boolean) => (
    <div style={{padding:'6px 0'}}>
      <label style={labelStyle}>{label}{required && <span style={{color:'#ef4444'}}> *</span>}</label>
      <select value={(form as any)[name] ?? ''} onChange={e => set(name, e.target.value)}
        style={inputStyle()}>
        <option value="">--- Select ---</option>
        {options.map(o => <option key={o} value={o}>{o||'(blank)'}</option>)}
      </select>
    </div>
  );

  const TRANSPORT_SEL = (name: keyof ImportOrder, label: string) => (
    <div style={{padding:'6px 0'}}>
      <label style={labelStyle}>{label}</label>
      <select
        value={(form as any)[name] ?? ''}
        onChange={e => set(name, e.target.value)}
        disabled={transportTypeOptions.length === 0}
        style={inputStyle(transportTypeOptions.length === 0)}
      >
        <option value="">{transportTypeOptions.length === 0 ? 'Select Communication Partner first' : '--- Select ---'}</option>
        {transportTypeOptions.map(o => <option key={o} value={o}>{o}</option>)}
      </select>
    </div>
  );

  const GR = ({children}: {children: React.ReactNode}) => (
    <div style={{display:'grid',gridTemplateColumns:'repeat(auto-fill,minmax(260px,1fr))',gap:'0 20px'}}>
      {children}
    </div>
  );

  const SEC = (title: string) => (
    <div style={{fontSize:10,fontWeight:700,color:'var(--cyan)',textTransform:'uppercase',letterSpacing:'.8px',marginTop:18,marginBottom:4,gridColumn:'span 2',borderBottom:'1px solid var(--border)',paddingBottom:4}}>
      {title}
    </div>
  );

  if (loading) return (
    <div style={{display:'flex',justifyContent:'center',alignItems:'center',height:300}}>
      <Loader2 size={28} color="var(--cyan)" />
    </div>
  );

  return (
    <div style={{padding:'24px 28px',fontFamily:"'Exo 2',sans-serif",height:'100%',display:'flex',flexDirection:'column'}}>
      {toast && (
        <div style={{position:'fixed',top:20,right:20,zIndex:9999,background:toast.ok?'#10b981':'#ef4444',color:'#fff',padding:'10px 18px',borderRadius:8,fontSize:13}}>
          {toast.msg}
        </div>
      )}

      <div style={{display:'flex',alignItems:'center',gap:12,marginBottom:20,flexShrink:0}}>
        <button onClick={() => navigate(isEdit ? `/import-orders/${entryNo}` : '/import-orders')}
          style={{background:'var(--bg3)',border:'1px solid var(--border)',borderRadius:6,padding:'6px 10px',color:'var(--text)',cursor:'pointer',display:'flex',alignItems:'center',gap:5,fontSize:12}}>
          <ArrowLeft size={12} /> Back
        </button>
        <h1 style={{margin:0,fontSize:18,color:'var(--text)'}}>
          {isEdit ? `Edit Order #${entryNo}` : 'Create New Import Order'}
        </h1>
        <div style={{flex:1}} />
        <button onClick={() => navigate(isEdit ? `/import-orders/${entryNo}` : '/import-orders')}
          style={{background:'var(--bg3)',border:'1px solid var(--border)',borderRadius:6,padding:'6px 14px',color:'var(--text)',cursor:'pointer',fontSize:12}}>
          Cancel
        </button>
        <button onClick={handleSubmit} disabled={saving}
          style={{background:'var(--cyan)',border:'none',borderRadius:6,padding:'6px 16px',color:'#000',fontWeight:700,cursor:'pointer',display:'flex',alignItems:'center',gap:6,fontSize:12}}>
          {saving ? <Loader2 size={13} /> : <Save size={13} />}
          {isEdit ? 'Update Order' : 'Create Order'}
        </button>
      </div>

      <div style={{display:'flex',gap:2,borderBottom:'1px solid var(--border)',flexShrink:0}}>
        {TABS.map((t,i) => (
          <button key={t} onClick={() => setActiveTab(i)} style={{
            background:activeTab===i?'var(--bg3)':'none',
            border:'none',borderBottom:activeTab===i?'2px solid var(--cyan)':'2px solid transparent',
            padding:'9px 16px',color:activeTab===i?'var(--cyan)':'var(--muted)',
            cursor:'pointer',fontSize:12,fontWeight:activeTab===i?700:400,
          }}>{t}</button>
        ))}
      </div>

      <div style={{flex:1,minHeight:0,background:'var(--bg2)',border:'1px solid var(--border)',borderTop:'none',borderRadius:'0 0 10px 10px',overflow:'auto',padding:'16px 20px'}}>
        {activeTab === 0 && <GR>
          {TF('entryNo','Entry No.','number',true)}
          {TF('externalOrderNo','External Order No.','text',false,true)}
          {TF('communicationPartner','Communication Partner','text',false,true)}
          {TF('importFileEntryNo','Import File Entry No.','number',true)}
          {SEL('transactionType','Transaction Type',TRANSACTION_TYPES,true)}
          {TF('externalCustomerNo','External Customer No.')}
          {TF('customerName','Customer Name','text',false,true)}
          {TF('shortcutReference1Code','Shortcut Reference 1 Code')}
          {TF('shortcutReference2Code','Shortcut Reference 2 Code')}
          {TF('shortcutReference3Code','Shortcut Reference 3 Code')}
          {TF('orderDescription','Order Description')}
          {TF('description2','Description 2')}
          {TRANSPORT_SEL('transportType','Transport Type')}
          {TF('tripTypeNo','Trip Type No.')}
          {TF('orderDate','Order Date','date')}
          {TF('collectionDate','Collection Date','datetime-local')}
          {TF('deliveryDate','Delivery Date','datetime-local')}
          {TF('office','Office')}
          {TF('salesResponsible','Sales Responsible')}
          {TF('originInfo','Origin Info')}
          {TF('countryOfOrigin','Country Of Origin')}
          {TF('destinationInfo','Destination Info')}
          {TF('countryOfDestination','Country Of Destination')}
          {CB('neutralShipment','Neutral Shipment')}
          {TF('nsAddName','NS Add Name')}
          {TF('nsAddStreet','NS Add Street')}
          {TF('nsAddCityPc','NS Add CityPC')}
          {SEL('cashOnDeliveryType','Cash on Delivery Type',COD_TYPES)}
          {TF('cashOnDeliveryAmount','Cash on Delivery Amount','number')}
          {SEL('status','Status',STATUSES,true)}
          {TF('errorMessage','Error Message','text',false)}
          {TF('tmsOrderNo','TMS Order No.','text',true)}
          {TF('carrierNo','Carrier No.')}
          {TF('carrierName','Carrier Name')}
          {TF('vesselNameImport','Vessel Name (Import)')}
          {TF('vesselNameExport','Vessel Name (Export)')}
          {TF('custServResponsible','Cust. Serv. Responsible')}
          {SEL('traction_order','Traction Order',TRACTION_ORDERS)}
          {TF('webPortalUser','Web Portal User','text',true)}
          {CB('overruleCustRefDuplicate','Overrule Cust Ref Duplicate')}
        </GR>}

        {activeTab === 1 && <GR>
          {TF('customerName','Customer Name','text',false,true)}
          {TF('customerName2','Customer Name 2')}
          {TF('externalCustomerNo','External Customer No.')}
          {TF('customerSearchName','Customer Search Name')}
          {TF('sellToAddress','Sell-to Address')}
          {TF('sellToAddress2','Sell-to Address 2')}
          {TF('sellToCity','Sell-to City')}
          {TF('sellToContact','Sell-to Contact')}
          {TF('sellToPostCode','Sell-to Post Code')}
          {TF('sellToCounty','Sell-to County')}
          {TF('sellToCountryRegionCode','Sell-to Country/Region Code')}
          {TF('vatRegistrationNo','VAT Registration No.')}
          {TF('boundName','Bound Name')}
        </GR>}

        {activeTab === 2 && <GR>
          {TF('billToCustomerNo','Bill-to Customer No.','text',false,true)}
          {TF('billToName','Bill-to Name')}
          {TF('billToName2','Bill-to Name 2')}
          {TF('billToAddress','Bill-to Address')}
          {TF('billToAddress2','Bill-to Address 2')}
          {TF('billToCity','Bill-to City')}
          {TF('billToContact','Bill-to Contact')}
          {TF('billToPostCode','Bill-to Post Code')}
          {TF('billToCounty','Bill-to County')}
          {TF('billToCountryRegionCode','Bill-to Country/Region Code')}
        </GR>}

        {activeTab === 3 && <GR>
          {TRANSPORT_SEL('transportType','Transport Type')}
          {TF('tradeLaneNo','Trade Lane No.')}
          {TF('transitTimeNo','Transit Time No.')}
          {TF('tripTypeNo','Trip Type No.')}
          {TF('carrierNo','Carrier No.')}
          {TF('carrierName','Carrier Name')}
          {TF('vesselNameImport','Vessel Name (Import)')}
          {TF('vesselNameExport','Vessel Name (Export)')}
          {TF('originPortName','Origin Port Name')}
          {TF('destinationPortName','Destination Port Name')}
          {TF('vesselEta','Vessel ETA','datetime-local')}
          {TF('vesselEtd','Vessel ETD','datetime-local')}
          {TF('sealNo','Seal No.')}
          {SEL('traction_order','Traction Order',TRACTION_ORDERS)}
          {TF('countryOfOrigin','Country Of Origin')}
          {TF('countryOfDestination','Country Of Destination')}
          {TF('originInfo','Origin Info')}
          {TF('destinationInfo','Destination Info')}
        </GR>}

        {activeTab === 4 && <GR>
          {TF('orderDate','Order Date','date')}
          {TF('collectionDate','Collection Date','datetime-local')}
          {TF('deliveryDate','Delivery Date','datetime-local')}
          {TF('firstOrderLineDate','First Order Line Date','date')}
          {TF('ata','ATA','datetime-local')}
          {TF('atd','ATD','datetime-local')}
          {TF('eta','ETA','datetime-local')}
          {TF('etd','ETD','datetime-local')}
          {TF('daysOfDemurrage','Days Of Demurrage','number')}
          {TF('daysOfDetention','Days Of Detention','number')}
          {TF('daysOfStorage','Days Of Storage','number')}
        </GR>}

        {activeTab === 5 && <GR>
          {SEC('Tariff')}
          {CB('properTariff','Proper Tariff')}
          {TF('tariffNo','Tariff No.')}
          {TF('tariffId','Tariff ID')}
          {CB('multipleTariff','Multiple Tariff')}
          {TF('executionTime','Execution Time','number')}
          {TF('temperature','Temperature','number')}
          {SEC('Logistics')}
          {CB('shippingRequired','Shipping Required')}
          {CB('recalcDistance','Recalc Distance')}
          {CB('tradelaneEqualsOrder','Tradelane = Order')}
          {SEL('source','Source',SOURCES)}
          {TF('noSeries','No. Series')}
          {TF('forwardingOrderNo','Forwarding Order No.')}
          {TF('bookingReference','Booking Reference')}
          {SEC('Container / Vessel')}
          {TF('containerNumber','Container Number')}
          {TF('containerType','Container Type')}
          {TF('containerTypeIsoCode','Container Type ISO Code')}
          {TF('carrierId','Carrier ID')}
          {TF('sealNumber','Seal Number')}
          {TF('importOrExport','Import or Export')}
          {TF('pickupPincode','Pickup Pincode')}
          {TF('pickupReference','Pickup Reference')}
          {TF('dropoffPincode','Dropoff Pincode')}
          {TF('dropoffReference','Dropoff Reference')}
          {CB('containerCancelled','Container Cancelled')}
          {TF('vesselName','Vessel Name')}
          {TF('closingDateTime','Closing DateTime','datetime-local')}
          {TF('depotOutFromDateTime','Depot Out From DateTime','datetime-local')}
          {TF('depotInFromDateTime','Depot In From DateTime','datetime-local')}
          {TF('vgmClosingDateTime','VGM Closing DateTime','datetime-local')}
          {TF('vgmWeight','VGM Weight','number')}
          {TF('originCountry','Origin Country')}
          {TF('destinationCountry','Destination Country')}
        </GR>}
      </div>

      <div style={{display:'flex',justifyContent:'flex-end',gap:10,marginTop:14,flexShrink:0}}>
        <button onClick={() => navigate(isEdit ? `/import-orders/${entryNo}` : '/import-orders')}
          style={{background:'var(--bg3)',border:'1px solid var(--border)',borderRadius:6,padding:'8px 18px',color:'var(--text)',cursor:'pointer',fontSize:12}}>
          Cancel
        </button>
        <button onClick={handleSubmit} disabled={saving}
          style={{background:'var(--cyan)',border:'none',borderRadius:6,padding:'8px 20px',color:'#000',fontWeight:700,cursor:'pointer',display:'flex',alignItems:'center',gap:6,fontSize:12}}>
          {saving ? <Loader2 size={13} /> : <Save size={13} />}
          {isEdit ? 'Update Order' : 'Create Order'}
        </button>
      </div>
    </div>
  );
}
