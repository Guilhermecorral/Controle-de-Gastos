// Cookie consent banner, informa ao usuário sobre uso de cookies
import { useState, useEffect } from 'react';
import Button from './Button';

export default function CookieConsent() {
  const [accepted, setAccepted] = useState(!!localStorage.getItem('cookieConsent'));

  useEffect(() => {
    if (localStorage.getItem('cookieConsent')) {
      setAccepted(true);
    }
  }, []);

  if (accepted) return null;

  const handleAccept = () => {
    localStorage.setItem('cookieConsent', 'true');
    setAccepted(true);
  };

  return (
    <div className="fixed bottom-0 left-0 right-0 bg-gray-900 text-white p-4 shadow-lg z-40">
      <div className="max-w-6xl mx-auto flex items-center justify-between flex-col md:flex-row gap-4">
        <div className="flex-1">
          <p className="text-sm">
            Usamos cookies para melhorar sua experiência. Ao continuar navegando, você concorda com nossa política de cookies.{' '}
            <a href="/privacy" className="text-blue-400 hover:underline">
              Saiba mais
            </a>
          </p>
        </div>
        <div className="flex gap-2">
          <Button onClick={handleAccept} className="bg-blue-600 hover:bg-blue-700 text-white py-2 px-4">
            Aceitar
          </Button>
        </div>
      </div>
    </div>
  );
}
