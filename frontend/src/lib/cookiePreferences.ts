export type CookiePreferences = {
  necessary: true;
  analytics: boolean;
  preferences: boolean;
};

export const cookiePreferencesStorageKey = 'cg-cookie-preferences';
export const cookiePreferencesChangedEvent = 'cg-cookie-preferences-changed';

export function readCookiePreferences(): CookiePreferences | null {
  const rawValue = localStorage.getItem(cookiePreferencesStorageKey);

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as CookiePreferences;
  } catch {
    return null;
  }
}

export function saveCookiePreferences(preferences: CookiePreferences) {
  localStorage.setItem(cookiePreferencesStorageKey, JSON.stringify(preferences));
  window.dispatchEvent(new CustomEvent(cookiePreferencesChangedEvent, { detail: preferences }));
}
