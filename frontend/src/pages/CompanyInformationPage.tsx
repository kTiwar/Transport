import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
  PageHeader,
  SecondaryActionBar,
  FormSection,
  FormField,
  ToggleSwitch,
  DropdownField,
  LookupField,
  ImageUploader,
  validateEmail,
  validateIban,
  validatePhone,
} from '../components/companyInformation';

const IC_INBOX = [
  { value: 'File Location', label: 'File Location' },
  { value: 'Database', label: 'Database' },
  { value: 'Email', label: 'Email' },
];

const POSTING_GROUPS = [
  { value: '', label: '' },
  { value: 'DOMESTIC', label: 'DOMESTIC' },
  { value: 'FOREIGN', label: 'FOREIGN' },
];

const LOCATIONS = [
  { value: 'SIMPLE 1', label: 'SIMPLE 1' },
  { value: 'MAIN', label: 'MAIN' },
];

const TIME_BUCKET = [
  { value: 'Day', label: 'Day' },
  { value: 'Week', label: 'Week' },
  { value: 'Month', label: 'Month' },
];

const COUNTRY_OPTIONS = [
  { value: '', label: '' },
  { value: 'DK', label: 'DK' },
  { value: 'GB', label: 'GB' },
  { value: 'US', label: 'US' },
  { value: 'DE', label: 'DE' },
];

export type CompanyForm = {
  name: string;
  address: string;
  address2: string;
  city: string;
  postCode: string;
  countryRegionCode: string;
  contactName: string;
  phoneNo: string;
  vatRegistrationNo: string;
  gln: string;
  industrialClassification: string;
  faxNo: string;
  email: string;
  homePage: string;
  icPartnerCode: string;
  icInboxType: string;
  icInboxDetails: string;
  autoSendTransactions: boolean;
  allowBlankPaymentInfo: boolean;
  bankName: string;
  bankBranchNo: string;
  bankAccountNo: string;
  paymentRoutingNo: string;
  giroNo: string;
  swiftCode: string;
  iban: string;
  bankCreditorNo: string;
  bankAccountPostingGroup: string;
  shipToName: string;
  shipToAddress: string;
  shipToAddress2: string;
  shipToCity: string;
  shipToPostCode: string;
  shipToCountryRegionCode: string;
  shipToContact: string;
  locationCode: string;
  responsibilityCenter: string;
  checkAvailPeriodCalc: string;
  checkAvailTimeBucket: string;
  baseCalendarCode: string;
};

const INITIAL: CompanyForm = {
  name: 'Dynus',
  address: 'Egervej 7',
  address2: 'Stelby',
  city: 'Assens',
  postCode: 'DK-5610',
  countryRegionCode: 'DK',
  contactName: '',
  phoneNo: '5160 6051',
  vatRegistrationNo: '30359511',
  gln: '',
  industrialClassification: '',
  faxNo: '',
  email: 'info@dynus.dk',
  homePage: 'www.dynus.dk',
  icPartnerCode: '',
  icInboxType: 'File Location',
  icInboxDetails: '',
  autoSendTransactions: false,
  allowBlankPaymentInfo: false,
  bankName: 'Danske Bank',
  bankBranchNo: '1234',
  bankAccountNo: '1234567',
  paymentRoutingNo: '',
  giroNo: '',
  swiftCode: 'DABADKKK',
  iban: 'DK9130003159164772',
  bankCreditorNo: '',
  bankAccountPostingGroup: '',
  shipToName: '',
  shipToAddress: '',
  shipToAddress2: '',
  shipToCity: '',
  shipToPostCode: '',
  shipToCountryRegionCode: '',
  shipToContact: '',
  locationCode: 'SIMPLE 1',
  responsibilityCenter: '',
  checkAvailPeriodCalc: '6M',
  checkAvailTimeBucket: 'Day',
  baseCalendarCode: 'UK',
};

const textInp =
  'h-[26px] w-full max-w-[320px] border border-erp-field bg-white px-2 font-erp text-[13px] text-erp-text focus:border-[#107c10] focus:outline-none focus:ring-1 focus:ring-[#107c10] disabled:bg-[#f3f2f1] disabled:text-erp-muted';

const readOnlyBox =
  'min-h-[26px] w-full max-w-[320px] border border-erp-field bg-white px-2 py-0.5 font-erp text-[13px] text-erp-text';

export default function CompanyInformationPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<CompanyForm>(INITIAL);
  const [baseline, setBaseline] = useState<CompanyForm>(INITIAL);
  const [editing, setEditing] = useState(false);
  const [saved, setSaved] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [errors, setErrors] = useState<Record<string, string | null>>({});
  const [logoFile, setLogoFile] = useState<File | null>(null);
  const [logoPreview, setLogoPreview] = useState<string | null>(null);

  const scrollRef = useRef<HTMLDivElement>(null);

  const patch = useCallback(<K extends keyof CompanyForm>(key: K, value: CompanyForm[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
    setSaved(false);
  }, []);

  useEffect(() => {
    if (!logoFile) {
      setLogoPreview(null);
      return;
    }
    const url = URL.createObjectURL(logoFile);
    setLogoPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [logoFile]);

  const demoLookup = useCallback(() => {
    toast('Lookup (demo).');
  }, []);

  const runValidation = useCallback(() => {
    const next: Record<string, string | null> = {};
    next.email = validateEmail(form.email);
    next.iban = validateIban(form.iban);
    next.phoneNo = validatePhone(form.phoneNo);
    setErrors(next);
    return !Object.values(next).some(Boolean);
  }, [form.email, form.iban, form.phoneNo]);

  const handleSave = () => {
    if (!runValidation()) {
      toast.error('Fix validation errors before saving.');
      return;
    }
    setBaseline({ ...form });
    setSaved(true);
    setEditing(false);
    toast.success('Company information saved (simulation).');
  };

  const handleDiscard = () => {
    setForm({ ...baseline });
    setLogoFile(null);
    setErrors({});
    setSaved(true);
    setEditing(false);
    toast('Changes discarded.', { icon: '↩' });
  };

  const toggleFullscreen = () => {
    const el = document.documentElement;
    if (!document.fullscreenElement) {
      el.requestFullscreen?.().catch(() => toast.error('Fullscreen not available'));
    } else {
      document.exitFullscreen?.();
    }
  };

  const renderText = (key: keyof CompanyForm, label: string) => (
    <FormField key={key} label={label} searchQuery={searchQuery} error={errors[key as string] ?? null}>
      {editing ? (
        <input
          className={textInp}
          value={String(form[key])}
          onChange={(e) => patch(key, e.target.value as CompanyForm[typeof key])}
        />
      ) : (
        <div className={readOnlyBox}>{String(form[key] ?? '') || '\u00a0'}</div>
      )}
    </FormField>
  );

  const renderLookup = (key: keyof CompanyForm, label: string) => (
    <FormField key={key} label={label} searchQuery={searchQuery} error={errors[key as string] ?? null}>
      {editing ? (
        <LookupField
          value={String(form[key])}
          onChange={(v) => patch(key, v as CompanyForm[typeof key])}
          onLookup={demoLookup}
        />
      ) : (
        <div className={readOnlyBox}>{String(form[key] ?? '') || '\u00a0'}</div>
      )}
    </FormField>
  );

  const twoCols = (left: ReactNode, right: ReactNode) => (
    <div className="grid grid-cols-1 gap-x-10 gap-y-0 lg:grid-cols-2">
      <div className="min-w-0 space-y-0">{left}</div>
      <div className="min-w-0 space-y-0">{right}</div>
    </div>
  );

  return (
    <div className="erp-company-shell flex min-h-screen w-full min-w-0 flex-col bg-erp-canvas font-erp text-erp-text antialiased">
      <div className="sticky top-0 z-20 bg-white shadow-[0_1px_0_rgba(0,0,0,0.06)]">
        <PageHeader
          title="Company Information"
          onBack={() => navigate(-1)}
          editing={editing}
          onToggleEdit={() => setEditing((e) => !e)}
          onNew={() => toast('New record (demo).')}
          onDelete={() => toast.error('Delete blocked in demo.')}
          saved={saved}
          onExpand={toggleFullscreen}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
        <SecondaryActionBar
          scrollContainerRef={scrollRef}
          onNavigate={() => toast('Navigate (demo).')}
        />
      </div>

      <div className="mx-auto w-full max-w-[1200px] flex-1 px-3 pb-10 pt-2 sm:px-4">
        <div
          ref={scrollRef}
          className="max-h-[calc(100dvh-8.75rem)] min-h-[260px] overflow-y-auto border border-erp-rule bg-white shadow-[0_0.3px_2px_rgba(0,0,0,0.06)]"
        >
          <div id="section-general" className="scroll-mt-28">
            <FormSection title="General">
              {twoCols(
                <>
                  {renderText('name', 'Name')}
                  {renderText('address', 'Address')}
                  {renderText('address2', 'Address 2')}
                  {renderLookup('city', 'City')}
                  {renderLookup('postCode', 'Post Code')}
                  <FormField label="Country/Region Code" searchQuery={searchQuery}>
                    {editing ? (
                      <DropdownField
                        options={COUNTRY_OPTIONS}
                        value={form.countryRegionCode}
                        onChange={(v) => patch('countryRegionCode', v)}
                        disabled={!editing}
                      />
                    ) : (
                      <div className={readOnlyBox}>{form.countryRegionCode || '\u00a0'}</div>
                    )}
                  </FormField>
                  {renderText('contactName', 'Contact Name')}
                </>,
                <>
                  {renderText('phoneNo', 'Phone No.')}
                  {renderText('vatRegistrationNo', 'VAT Registration No.')}
                  {renderText('gln', 'GLN')}
                  {renderText('industrialClassification', 'Industrial Classification')}
                  <FormField label="Picture" searchQuery={searchQuery}>
                    <ImageUploader
                      previewUrl={logoPreview}
                      onFile={(f) => {
                        setLogoFile(f);
                        setSaved(false);
                      }}
                      disabled={!editing}
                    />
                  </FormField>
                </>,
              )}
            </FormSection>
          </div>

          <div id="section-communication" className="scroll-mt-28">
            <FormSection title="Communication">
              {twoCols(
                <>
                  {renderText('phoneNo', 'Phone No.')}
                  {renderText('faxNo', 'Fax No.')}
                  {renderText('email', 'Email')}
                  {renderText('homePage', 'Home Page')}
                </>,
                <>
                  {renderText('icPartnerCode', 'IC Partner Code')}
                  <FormField label="IC Inbox Type" searchQuery={searchQuery}>
                    {editing ? (
                      <DropdownField
                        options={IC_INBOX}
                        value={form.icInboxType}
                        onChange={(v) => patch('icInboxType', v)}
                        disabled={!editing}
                      />
                    ) : (
                      <div className={readOnlyBox}>{form.icInboxType || '\u00a0'}</div>
                    )}
                  </FormField>
                  {renderLookup('icInboxDetails', 'IC Inbox Details')}
                  <FormField label="Auto. Send Transactions" searchQuery={searchQuery}>
                    <ToggleSwitch
                      checked={form.autoSendTransactions}
                      onChange={(v) => patch('autoSendTransactions', v)}
                      disabled={!editing}
                      compact
                      aria-label="Auto. Send Transactions"
                    />
                  </FormField>
                </>,
              )}
            </FormSection>
          </div>

          <div id="section-payments" className="scroll-mt-28">
            <FormSection title="Payments">
              {twoCols(
                <>
                  <FormField label="Allow Blank Payment Info." searchQuery={searchQuery}>
                    <ToggleSwitch
                      checked={form.allowBlankPaymentInfo}
                      onChange={(v) => patch('allowBlankPaymentInfo', v)}
                      disabled={!editing}
                      compact
                      aria-label="Allow blank payment info"
                    />
                  </FormField>
                  {renderText('bankName', 'Bank Name')}
                  {renderText('bankBranchNo', 'Bank Branch No.')}
                  {renderText('bankAccountNo', 'Bank Account No.')}
                  {renderText('paymentRoutingNo', 'Payment Routing No.')}
                </>,
                <>
                  {renderText('giroNo', 'Giro No.')}
                  {renderText('swiftCode', 'SWIFT Code')}
                  {renderText('iban', 'IBAN')}
                  {renderText('bankCreditorNo', 'Bank Creditor No.')}
                  <FormField label="Bank Account Posting Group" searchQuery={searchQuery}>
                    {editing ? (
                      <DropdownField
                        options={POSTING_GROUPS}
                        value={form.bankAccountPostingGroup}
                        onChange={(v) => patch('bankAccountPostingGroup', v)}
                        disabled={!editing}
                      />
                    ) : (
                      <div className={readOnlyBox}>{form.bankAccountPostingGroup || '\u00a0'}</div>
                    )}
                  </FormField>
                </>,
              )}
            </FormSection>
          </div>

          <div id="section-shipping" className="scroll-mt-28">
            <FormSection title="Shipping">
              {twoCols(
                <>
                  {renderText('shipToName', 'Ship-to Name')}
                  {renderText('shipToAddress', 'Ship-to Address')}
                  {renderText('shipToAddress2', 'Ship-to Address 2')}
                  {renderText('shipToCity', 'Ship-to City')}
                  {renderText('shipToPostCode', 'Ship-to Post Code')}
                  {renderText('shipToCountryRegionCode', 'Ship-to Country/Region Code')}
                  {renderText('shipToContact', 'Ship-to Contact')}
                </>,
                <>
                  <FormField label="Location Code" searchQuery={searchQuery}>
                    {editing ? (
                      <DropdownField
                        options={LOCATIONS}
                        value={form.locationCode}
                        onChange={(v) => patch('locationCode', v)}
                        disabled={!editing}
                      />
                    ) : (
                      <div className={readOnlyBox}>{form.locationCode || '\u00a0'}</div>
                    )}
                  </FormField>
                  <FormField label="Responsibility Center" searchQuery={searchQuery}>
                    {editing ? (
                      <DropdownField
                        options={[
                          { value: '', label: '' },
                          { value: 'MAIN', label: 'MAIN' },
                        ]}
                        value={form.responsibilityCenter}
                        onChange={(v) => patch('responsibilityCenter', v)}
                        disabled={!editing}
                      />
                    ) : (
                      <div className={readOnlyBox}>{form.responsibilityCenter || '\u00a0'}</div>
                    )}
                  </FormField>
                  {renderText('checkAvailPeriodCalc', 'Check-Avail. Period Calc.')}
                  <FormField label="Check-Avail. Time Bucket" searchQuery={searchQuery}>
                    {editing ? (
                      <DropdownField
                        options={TIME_BUCKET}
                        value={form.checkAvailTimeBucket}
                        onChange={(v) => patch('checkAvailTimeBucket', v)}
                        disabled={!editing}
                      />
                    ) : (
                      <div className={readOnlyBox}>{form.checkAvailTimeBucket || '\u00a0'}</div>
                    )}
                  </FormField>
                  {renderText('baseCalendarCode', 'Base Calendar Code')}
                </>,
              )}
            </FormSection>
          </div>

          <div id="section-application-settings" className="scroll-mt-28">
            <FormSection title="Application Settings" defaultOpen={false}>
              <p className="px-2 py-4 text-[13px] text-erp-muted">
                Connect to Business Central or your ERP API to load application settings.
              </p>
            </FormSection>
          </div>

          <div id="section-system-settings" className="scroll-mt-28">
            <FormSection title="System Settings" defaultOpen={false}>
              <p className="px-2 py-4 text-[13px] text-erp-muted">
                System settings are not available in this demo workspace.
              </p>
            </FormSection>
          </div>

          <div id="section-currencies" className="scroll-mt-28">
            <FormSection title="Currencies" defaultOpen={false}>
              <p className="px-2 py-4 text-[13px] text-erp-muted">Currency setup opens from the full ERP shell.</p>
            </FormSection>
          </div>

          <div id="section-codes" className="scroll-mt-28">
            <FormSection title="Codes" defaultOpen={false}>
              <p className="px-2 py-4 text-[13px] text-erp-muted">Code lists are managed in the connected ERP.</p>
            </FormSection>
          </div>

          <div id="section-regional-settings" className="scroll-mt-28">
            <FormSection title="Regional Settings" defaultOpen={false}>
              <p className="px-2 py-4 text-[13px] text-erp-muted">Regional formats follow your tenant locale.</p>
            </FormSection>
          </div>

          <div id="section-show-attached" className="scroll-mt-28">
            <FormSection title="Show Attached" defaultOpen={false}>
              <p className="px-2 py-4 text-[13px] text-erp-muted">No attachments in this demo.</p>
            </FormSection>
          </div>
        </div>

        {editing && (
          <div className="mt-4 flex flex-wrap justify-end gap-2 border-t border-erp-hairline pt-4">
            <button
              type="button"
              onClick={handleDiscard}
              className="rounded-sm border border-erp-field bg-white px-5 py-2 font-erp text-sm text-erp-text hover:bg-[#f3f2f1] focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#107c10]"
            >
              Discard
            </button>
            <button
              type="button"
              onClick={handleSave}
              className="rounded-sm border border-[#0078d4] bg-[#0078d4] px-5 py-2 font-erp text-sm font-semibold text-white hover:bg-[#106ebe] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0078d4]"
            >
              Save
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
