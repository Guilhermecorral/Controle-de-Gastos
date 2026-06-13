import { ReactNode } from 'react';
import { BrowserRouter } from 'react-router-dom';
import AnalyticsBridge from '../analytics/AnalyticsBridge';
import CookieConsent from '../components/CookieConsent';

export default function AppProviders({ children }: { children: ReactNode }) {
  return (
    <BrowserRouter>
      <AnalyticsBridge />
      {children}
      <CookieConsent />
    </BrowserRouter>
  );
}
