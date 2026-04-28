export function jwtDecode(token: string): Record<string, unknown> | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  } catch {
    return null;
  }
}

export function hasRole(roles: string[], role: string): boolean {
  return roles.some((r) => r === role || r === `ROLE_${role}`);
}
