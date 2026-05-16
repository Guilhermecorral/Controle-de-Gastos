// Página de Termo de Uso
export default function Terms() {
  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4">
      <div className="max-w-4xl mx-auto bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold mb-6">Termos de Uso</h1>

        <section className="space-y-6">
          <div>
            <h2 className="text-2xl font-semibold mb-3">1. Aceitação dos Termos</h2>
            <p className="text-gray-700">
              Ao acessar e usar o Controle de Gastos, você concorda em cumprir com estes termos e condições. Se você não concordar com qualquer parte destes termos, não deverá usar o serviço.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">2. Uso Licenciado</h2>
            <p className="text-gray-700">
              É concedida a você uma licença limitada, não exclusiva e revogável para usar este serviço. Você concorda em não reproduzir, transmitir, distribuir ou vender qualquer conteúdo sem nossa permissão.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">3. Responsabilidade do Usuário</h2>
            <p className="text-gray-700">
              Você é responsável por manter a confidencialidade de suas credenciais de login. Você concorda em aceitar responsabilidade por todas as atividades que ocorrem sob sua conta.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">4. Limitação de Responsabilidade</h2>
            <p className="text-gray-700">
              Em nenhuma circunstância, a Controle de Gastos será responsável por danos indiretos, incidentais, especiais ou consequentes, incluindo perda de dados.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">5. Modificação dos Termos</h2>
            <p className="text-gray-700">
              Reservamos o direito de modificar estes termos a qualquer momento. Continuando a usar o serviço após as modificações, você concorda com os termos revisados.
            </p>
          </div>

          <div>
            <h2 className="text-2xl font-semibold mb-3">6. Contato</h2>
            <p className="text-gray-700">
              Se você tiver dúvidas sobre estes termos, por favor entre em contato conosco em support@controledegastos.com
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
