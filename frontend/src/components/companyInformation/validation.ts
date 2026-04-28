/** Lightweight validators for ERP-style forms */

export function validateEmail(v: string): string | null {
  if (!v.trim()) return null;
  const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v.trim());
  return ok ? null : 'Enter a valid email address';
}

/** Basic IBAN check: length, country letters, alphanumeric */
export function validateIban(v: string): string | null {
  const s = v.replace(/\s/g, '').toUpperCase();
  if (!s) return null;
  if (s.length < 15 || s.length > 34) return 'IBAN length looks invalid';
  if (!/^[A-Z]{2}[0-9]{2}[A-Z0-9]+$/.test(s)) return 'IBAN format is invalid';
  return null;
}

/** Allows digits, spaces, +, -, parentheses */
export function validatePhone(v: string): string | null {
  if (!v.trim()) return null;
  const digits = v.replace(/\D/g, '');
  if (digits.length < 6) return 'Phone number is too short';
  return null;
}
