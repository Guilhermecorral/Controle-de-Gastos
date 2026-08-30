import { ReactNode } from 'react';
import { BrowserRouter } from 'react-router-dom';
import AnalyticsBridge from '../analytics/AnalyticsBridge';
import CookieConsent from '../components/CookieConsent';
import SeoRouteMeta from '../seo/SeoRouteMeta';

export default function AppProviders({ children }: { children: ReactNode }) {
  return (
    <BrowserRouter>
      <SeoRouteMeta />
      <AnalyticsBridge />
      {children}
      <CookieConsent />
    </BrowserRouter>
  );
}
