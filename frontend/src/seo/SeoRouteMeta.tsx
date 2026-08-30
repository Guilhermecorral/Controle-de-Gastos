import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const canonicalOrigin = 'https://www.farolfinanceiro.online';
const defaultDescription =
  'Organize receitas, despesas, compras planejadas e extratos em um ambiente financeiro claro, seguro e fácil de acompanhar.';

const routeTitles: Record<string, string> = {
  '/': 'Farol Financeiro | Controle financeiro com clareza',
  '/login': 'Entrar | Farol Financeiro',
  '/cadastro': 'Criar conta | Farol Financeiro',
  '/esqueci-a-senha': 'Recuperar senha | Farol Financeiro',
  '/redefinir-senha': 'Redefinir senha | Farol Financeiro',
};

export default function SeoRouteMeta() {
  const location = useLocation();

  useEffect(() => {
    const isLandingPage = location.pathname === '/';
    const title = routeTitles[location.pathname] || 'Área segura | Farol Financeiro';
    const canonicalUrl = `${canonicalOrigin}${isLandingPage ? '/' : location.pathname}`;
    const robots = isLandingPage
      ? 'index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1'
      : 'noindex, nofollow';

    document.documentElement.lang = 'pt-BR';
    document.title = title;
    setMeta('name', 'description', defaultDescription);
    setMeta('name', 'robots', robots);
    setMeta('property', 'og:title', title);
    setMeta('property', 'og:description', defaultDescription);
    setMeta('property', 'og:url', canonicalUrl);
    setMeta('name', 'twitter:title', title);
    setMeta('name', 'twitter:description', defaultDescription);
    setCanonical(canonicalUrl);
  }, [location.pathname]);

  return null;
}

function setMeta(attribute: 'name' | 'property', key: string, content: string) {
  let element = document.querySelector<HTMLMetaElement>(`meta[${attribute}="${key}"]`);
  if (!element) {
    element = document.createElement('meta');
    element.setAttribute(attribute, key);
    document.head.appendChild(element);
  }
  element.content = content;
}

function setCanonical(href: string) {
  let canonical = document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
  if (!canonical) {
    canonical = document.createElement('link');
    canonical.rel = 'canonical';
    document.head.appendChild(canonical);
  }
  canonical.href = href;
}
