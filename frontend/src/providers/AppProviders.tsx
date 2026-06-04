import { ReactNode } from 'react';
import { BrowserRouter } from 'react-router-dom';
import CookieConsent from '../components/CookieConsent';

export default function AppProviders({ children }: { children: ReactNode }) {
  return (
    <BrowserRouter>
      {children}
      <CookieConsent />
    </BrowserRouter>
  );
}
