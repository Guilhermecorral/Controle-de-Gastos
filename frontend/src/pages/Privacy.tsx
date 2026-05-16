// Página de Política de Privacidade
export default function Privacy() {
  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4">
      <div className="max-w-4xl mx-auto bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold mb-6">Política de Privacidade</h1>

        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-semibold mb-3">1. Informações que Coletamos</h2>
            <p className="text-gray-700">
              Coletamos informações que você nos fornece voluntariamente, como nome, email e informações financeiras pessoais. Também coletamos informações de uso do serviço.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">2. Como Usamos Suas Informações</h2>
            <p className="text-gray-700">
              Usamos suas informações para fornecer, manter e melhorar o serviço. Podemos usar dados para análises, pesquisa e comunicação sobre atualizações.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">3. Segurança de Dados</h2>
            <p className="text-gray-700">
              Implementamos medidas de segurança para proteger suas informações contra acesso, alteração, divulgação ou destruição não autorizada. Senhas são criptografadas.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">4. Cookies</h2>
            <p className="text-gray-700">
              Usamos cookies para melhorar sua experiência. Você pode controlar as configurações de cookies no seu navegador. Alguns recursos podem não funcionar sem cookies.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">5. Compartilhamento de Dados</h2>
            <p className="text-gray-700">
              Não compartilhamos suas informações pessoais com terceiros, exceto quando necessário para fornecer o serviço ou conforme exigido por lei.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">6. Seus Direitos</h2>
            <p className="text-gray-700">
              Você tem o direito de acessar, corrigir ou deletar suas informações pessoais. Entre em contato conosco para exercer esses direitos.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">7. Mudanças na Política</h2>
            <p className="text-gray-700">
              Podemos atualizar esta política periodicamente. Notificaremos você sobre mudanças significativas por email ou no serviço.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">8. Contato</h2>
            <p className="text-gray-700">
              Se tiver perguntas sobre nossa política de privacidade, entre em contato: privacy@controledegastos.com
            </p>
          </div>
        </section>

        <div className="mt-8 pt-6 border-t">
          <p className="text-gray-500 text-sm">Última atualização: Maio de 2026</p>
        </div>
      </div>
    </div>
  );
}
