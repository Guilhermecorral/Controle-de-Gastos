import { useEffect, useMemo, useState } from 'react';
import { Analytics } from '@vercel/analytics/react';
import { useLocation } from 'react-router-dom';
import {
  cookiePreferencesChangedEvent,
  CookiePreferences,
  readCookiePreferences,
} from '../lib/cookiePreferences';

declare global {
  interface Window {
    dataLayer?: unknown[];
    gtag?: (...args: unknown[]) => void;
  }
}

const googleAnalyticsMeasurementId = import.meta.env.VITE_GA_MEASUREMENT_ID?.trim() || '';

export default function AnalyticsBridge() {
  const location = useLocation();
  const [preferences, setPreferences] = useState<CookiePreferences | null>(() => readCookiePreferences());

  useEffect(() => {
    const syncPreferences = () => {
      setPreferences(readCookiePreferences());
    };

    syncPreferences();
    window.addEventListener('storage', syncPreferences);
    window.addEventListener(cookiePreferencesChangedEvent, syncPreferences as EventListener);

    return () => {
      window.removeEventListener('storage', syncPreferences);
      window.removeEventListener(cookiePreferencesChangedEvent, syncPreferences as EventListener);
    };
  }, []);

  const analyticsEnabled = useMemo(() => preferences?.analytics === true, [preferences]);

  useEffect(() => {
    if (!analyticsEnabled || !googleAnalyticsMeasurementId) {
      return;
    }

    if (!document.querySelector(`script[data-ga-id="${googleAnalyticsMeasurementId}"]`)) {
      const script = document.createElement('script');
      script.async = true;
      script.src = `https://www.googletagmanager.com/gtag/js?id=${googleAnalyticsMeasurementId}`;
      script.dataset.gaId = googleAnalyticsMeasurementId;
      document.head.appendChild(script);
    }

    window.dataLayer = window.dataLayer || [];
    window.gtag =
      window.gtag ||
      function gtag(...args: unknown[]) {
        window.dataLayer?.push(args);
      };

    window.gtag('js', new Date());
    window.gtag('config', googleAnalyticsMeasurementId, {
      anonymize_ip: true,
      page_path: `${location.pathname}${location.search}${location.hash}`,
    });
  }, [analyticsEnabled, location.hash, location.pathname, location.search]);

  if (!analyticsEnabled) {
    return null;
  }

  return <Analytics />;
}
