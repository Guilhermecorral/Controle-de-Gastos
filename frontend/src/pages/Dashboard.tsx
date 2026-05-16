// Dashboard: exibe visão geral das finanças com filtros por ano/mês
import { useState } from 'react';
import { useDashboard } from '../lib/queries';
import Card from '../components/Card';
import Button from '../components/Button';
import Select from '../components/Select';
import { TrendingUp, TrendingDown, DollarSign, ChevronRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const navigate = useNavigate();
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;
  const [year, setYear] = useState(currentYear);
  const [month, setMonth] = useState(currentMonth);
  const { data, isLoading, error, refetch } = useDashboard(year, month);

  const yearOptions = Array.from({ length: 10 }, (_, i) => ({
    value: (currentYear - i).toString(),
    label: (currentYear - i).toString(),
  }));

  const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    value: (i + 1).toString(),
    label: new Date(0, i).toLocaleString('pt-BR', { month: 'long' }),
  }));

  if (isLoading) return <div className="p-8">Carregando...</div>;
  if (error) return <div className="p-8 text-red-500">Erro ao carregar dashboard</div>;

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-4xl font-bold text-gray-800">Dashboard</h1>
          <p className="text-gray-500 mt-2">Resumo das suas finanças</p>
        </div>
        <Button onClick={() => navigate('/transactions/new')} className="bg-blue-600 hover:bg-blue-700 text-white">
          + Nova Transação
        </Button>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow-md flex gap-4 items-end">
        <Select
          label="Ano"
          options={yearOptions}
          value={year.toString()}
          onChange={(e) => setYear(+e.target.value)}
        />
        <Select
          label="Mês"
          options={monthOptions}
          value={month.toString()}
          onChange={(e) => setMonth(+e.target.value)}
        />
        <Button onClick={() => refetch()} className="bg-gray-200 hover:bg-gray-300">
          Atualizar
        </Button>
      </div>

      {data && (
        <>
          {/* Main Stats */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* Receitas */}
            <div className="bg-gradient-to-br from-green-50 to-green-100 border border-green-200 p-6 rounded-lg shadow-md hover:shadow-lg transition">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-green-600 text-sm font-semibold uppercase">Receitas do Mês</p>
                  <p className="text-3xl font-bold text-green-700 mt-2">R$ {data.receitasMesAtual.toFixed(2)}</p>
                </div>
                <div className="bg-green-200 p-3 rounded-full">
                  <TrendingUp className="text-green-600" size={24} />
                </div>
              </div>
              <p className="text-green-600 text-sm">+ Acumulado: R$ {data.totalReceitasAcumuladas.toFixed(2)}</p>
            </div>

            {/* Despesas */}
            <div className="bg-gradient-to-br from-red-50 to-red-100 border border-red-200 p-6 rounded-lg shadow-md hover:shadow-lg transition">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-red-600 text-sm font-semibold uppercase">Despesas do Mês</p>
                  <p className="text-3xl font-bold text-red-700 mt-2">R$ {data.despesasMesAtual.toFixed(2)}</p>
                </div>
                <div className="bg-red-200 p-3 rounded-full">
                  <TrendingDown className="text-red-600" size={24} />
                </div>
              </div>
              <p className="text-red-600 text-sm">+ Acumulado: R$ {data.totalDespesasAcumuladas.toFixed(2)}</p>
            </div>

            {/* Saldo */}
            <div className={`bg-gradient-to-br ${data.resultadoMesAtual >= 0 ? 'from-blue-50 to-blue-100' : 'from-yellow-50 to-yellow-100'} border ${data.resultadoMesAtual >= 0 ? 'border-blue-200' : 'border-yellow-200'} p-6 rounded-lg shadow-md hover:shadow-lg transition`}>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className={`text-sm font-semibold uppercase ${data.resultadoMesAtual >= 0 ? 'text-blue-600' : 'text-yellow-600'}`}>Saldo do Mês</p>
                  <p className={`text-3xl font-bold mt-2 ${data.resultadoMesAtual >= 0 ? 'text-blue-700' : 'text-yellow-700'}`}>
                    R$ {data.resultadoMesAtual.toFixed(2)}
                  </p>
                </div>
                <div className={`${data.resultadoMesAtual >= 0 ? 'bg-blue-200' : 'bg-yellow-200'} p-3 rounded-full`}>
                  <DollarSign className={data.resultadoMesAtual >= 0 ? 'text-blue-600' : 'text-yellow-600'} size={24} />
                </div>
              </div>
              <p className={`text-sm ${data.resultadoMesAtual >= 0 ? 'text-blue-600' : 'text-yellow-600'}`}>Acumulado: R$ {data.saldoAcumulado.toFixed(2)}</p>
            </div>
          </div>

          {/* Bottom Stats */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Últimas Transações */}
            <Card title="Últimas Transações" className="hover:shadow-lg transition">
              <div className="space-y-3">
                {data.ultimasTransacoes.length > 0 ? (
                  data.ultimasTransacoes.slice(0, 5).map((t) => (
                    <div key={t.id} className="flex justify-between items-center p-3 bg-gray-50 rounded hover:bg-gray-100 transition">
                      <div>
                        <p className="font-medium text-gray-800">{t.description}</p>
                        <p className="text-sm text-gray-500">{new Date(t.transactionDate).toLocaleDateString('pt-BR')}</p>
                      </div>
                      <p className={`font-bold ${t.type === 'RECEITA' ? 'text-green-600' : 'text-red-600'}`}>
                        {t.type === 'RECEITA' ? '+' : '-'} R$ {t.amount.toFixed(2)}
                      </p>
                    </div>
                  ))
                ) : (
                  <p className="text-gray-500 text-center py-4">Nenhuma transação</p>
                )}
              </div>
              <button
                onClick={() => navigate('/transactions')}
                className="mt-4 w-full flex items-center justify-center gap-2 text-blue-600 hover:text-blue-700 font-semibold"
              >
                Ver Todas <ChevronRight size={16} />
              </button>
            </Card>

            {/* Gastos por Categoria */}
            <Card title="Gastos por Categoria" className="hover:shadow-lg transition">
              <div className="space-y-3">
                {data.gastosPorCategoria.length > 0 ? (
                  data.gastosPorCategoria.map((cat) => (
                    <div key={cat.category}>
                      <div className="flex justify-between mb-2">
                        <span className="font-medium text-gray-800">{cat.category}</span>
                        <span className="text-gray-600">R$ {cat.totalAmount.toFixed(2)}</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-2">
                        <div
                          className="bg-blue-600 h-2 rounded-full"
                          style={{
                            width: `${Math.min(
                              (cat.totalAmount / Math.max(...data.gastosPorCategoria.map((c) => c.totalAmount), 1)) * 100,
                              100
                            )}%`,
                          }}
                        ></div>
                      </div>
                    </div>
                  ))
                ) : (
                  <p className="text-gray-500 text-center py-4">Sem gastos categorizados</p>
                )}
              </div>
              <button
                onClick={() => navigate('/analysis')}
                className="mt-4 w-full flex items-center justify-center gap-2 text-blue-600 hover:text-blue-700 font-semibold"
              >
                Análise Completa <ChevronRight size={16} />
              </button>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
