// Análise Mensal: compara mês selecionado com anteriores e acumulado
import { useState } from 'react';
import { useMonthlyAnalysis } from '../lib/queries';
import Card from '../components/Card';
import Select from '../components/Select';
import { ArrowUp, ArrowDown, Minus } from 'lucide-react';

export default function MonthlyAnalysis() {
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;
  const [year, setYear] = useState(currentYear);
  const [month, setMonth] = useState(currentMonth);
  const { data, isLoading, error } = useMonthlyAnalysis(year, month);

  const yearOptions = Array.from({ length: 10 }, (_, i) => ({
    value: (currentYear - i).toString(),
    label: (currentYear - i).toString(),
  }));

  const monthOptions = Array.from({ length: 12 }, (_, i) => ({
    value: (i + 1).toString(),
    label: new Date(0, i).toLocaleString('pt-BR', { month: 'long' }),
  }));

  const getTrendIcon = (trend: string) => {
    switch (trend) {
      case 'MELHOR':
        return <ArrowUp size={20} className="text-green-600" />;
      case 'PIOR':
        return <ArrowDown size={20} className="text-red-600" />;
      default:
        return <Minus size={20} className="text-gray-600" />;
    }
  };

  if (isLoading) return <div className="p-8 text-center">Carregando análise...</div>;
  if (error) return <div className="p-8 text-center text-red-500">Erro ao carregar análise</div>;

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold text-gray-800">Análise Mensal</h1>
        <p className="text-gray-500 mt-2">Analyze seus gastos mês a mês com comparativos</p>
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
      </div>

      {data && (
        <>
          {/* Main Summary */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-gradient-to-br from-green-50 to-green-100 border border-green-200 p-6 rounded-lg">
              <p className="text-green-600 text-sm font-semibold uppercase">Receitas</p>
              <p className="text-3xl font-bold text-green-700 mt-2">R$ {data.totalReceitas.toFixed(2)}</p>
            </div>
            <div className="bg-gradient-to-br from-red-50 to-red-100 border border-red-200 p-6 rounded-lg">
              <p className="text-red-600 text-sm font-semibold uppercase">Despesas</p>
              <p className="text-3xl font-bold text-red-700 mt-2">R$ {data.totalDespesas.toFixed(2)}</p>
            </div>
            <div className={`bg-gradient-to-br ${data.saldo >= 0 ? 'from-blue-50 to-blue-100' : 'from-yellow-50 to-yellow-100'} border ${data.saldo >= 0 ? 'border-blue-200' : 'border-yellow-200'} p-6 rounded-lg`}>
              <p className={`text-sm font-semibold uppercase ${data.saldo >= 0 ? 'text-blue-600' : 'text-yellow-600'}`}>Saldo</p>
              <p className={`text-3xl font-bold mt-2 ${data.saldo >= 0 ? 'text-blue-700' : 'text-yellow-700'}`}>R$ {data.saldo.toFixed(2)}</p>
            </div>
          </div>

          {/* Maior Gasto */}
          <Card title="Maior Gasto do Mês" className="hover:shadow-lg transition">
            <div className="bg-gray-50 p-6 rounded-lg">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-2xl font-bold text-gray-800">{data.maiorGasto.description}</p>
                  <p className="text-gray-500 text-sm mt-1">{data.maiorGasto.category}</p>
                </div>
                <p className="text-red-600 font-bold text-xl">R$ {data.maiorGasto.amount.toFixed(2)}</p>
              </div>
              <p className="text-gray-600 text-sm">Data: {new Date(data.maiorGasto.transactionDate).toLocaleDateString('pt-BR')}</p>
            </div>
          </Card>

          {/* Comparativos */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* vs Mês Anterior */}
            <Card title="vs Mês Anterior" className="hover:shadow-lg transition">
              <div className="space-y-4">
                <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-700 font-medium">Receitas</span>
                    {getTrendIcon(data.comparativoMesAnterior.tendenciaReceitas)}
                  </div>
                  <span className={data.comparativoMesAnterior.diferencaReceitas >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoMesAnterior.diferencaReceitas >= 0 ? '+' : ''}R$ {data.comparativoMesAnterior.diferencaReceitas.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-700 font-medium">Despesas</span>
                    {getTrendIcon(data.comparativoMesAnterior.tendenciaDespesas)}
                  </div>
                  <span className={data.comparativoMesAnterior.diferencaDespesas >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoMesAnterior.diferencaDespesas >= 0 ? '+' : ''}R$ {data.comparativoMesAnterior.diferencaDespesas.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center p-3 bg-blue-50 rounded font-semibold">
                  <div className="flex items-center gap-2">
                    <span className="text-blue-700">Saldo</span>
                    {getTrendIcon(data.comparativoMesAnterior.tendenciaSaldo)}
                  </div>
                  <span className={data.comparativoMesAnterior.diferencaSaldo >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoMesAnterior.diferencaSaldo >= 0 ? '+' : ''}R$ {data.comparativoMesAnterior.diferencaSaldo.toFixed(2)}
                  </span>
                </div>
              </div>
            </Card>

            {/* vs Mesmo Mês Ano Anterior */}
            <Card title="vs Mesmo Mês (Ano Anterior)" className="hover:shadow-lg transition">
              <div className="space-y-4">
                <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-700 font-medium">Receitas</span>
                    {getTrendIcon(data.comparativoMesmoMesAnoAnterior.tendenciaReceitas)}
                  </div>
                  <span className={data.comparativoMesmoMesAnoAnterior.diferencaReceitas >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoMesmoMesAnoAnterior.diferencaReceitas >= 0 ? '+' : ''}R$ {data.comparativoMesmoMesAnoAnterior.diferencaReceitas.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center p-3 bg-gray-50 rounded">
                  <div className="flex items-center gap-2">
                    <span className="text-gray-700 font-medium">Despesas</span>
                    {getTrendIcon(data.comparativoMesmoMesAnoAnterior.tendenciaDespesas)}
                  </div>
                  <span className={data.comparativoMesmoMesAnoAnterior.diferencaDespesas >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoMesmoMesAnoAnterior.diferencaDespesas >= 0 ? '+' : ''}R$ {data.comparativoMesmoMesAnoAnterior.diferencaDespesas.toFixed(2)}
                  </span>
                </div>
                <div className="flex justify-between items-center p-3 bg-blue-50 rounded font-semibold">
                  <div className="flex items-center gap-2">
                    <span className="text-blue-700">Saldo</span>
                    {getTrendIcon(data.comparativoMesmoMesAnoAnterior.tendenciaSaldo)}
                  </div>
                  <span className={data.comparativoMesmoMesAnoAnterior.diferencaSaldo >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoMesmoMesAnoAnterior.diferencaSaldo >= 0 ? '+' : ''}R$ {data.comparativoMesmoMesAnoAnterior.diferencaSaldo.toFixed(2)}
                  </span>
                </div>
              </div>
            </Card>
          </div>

          {/* Acumulado Anual */}
          <Card title="Acumulado Anual" className="hover:shadow-lg transition">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="p-4 bg-blue-50 rounded-lg">
                <p className="text-blue-600 font-semibold mb-3">Ano Atual ({year})</p>
                <p className="text-gray-700 mb-2">Receitas: <span className="font-bold text-green-600">R$ {data.acumuladoAnoAtual.totalReceitas.toFixed(2)}</span></p>
                <p className="text-gray-700 mb-2">Despesas: <span className="font-bold text-red-600">R$ {data.acumuladoAnoAtual.totalDespesas.toFixed(2)}</span></p>
                <p className="text-gray-700">Saldo: <span className="font-bold text-blue-600">R$ {data.acumuladoAnoAtual.saldo.toFixed(2)}</span></p>
              </div>
              <div className="p-4 bg-green-50 rounded-lg">
                <p className="text-green-600 font-semibold mb-3">Comparativo ({year - 1})</p>
                <p className="text-gray-700 flex justify-between items-center mb-2">
                  Variação de Receitas
                  {getTrendIcon(data.comparativoAcumuladoAnoAnterior.tendenciaReceitas)}
                  <span className={data.comparativoAcumuladoAnoAnterior.diferencaReceitas >= 0 ? 'text-green-600' : 'text-red-600'}>
                    {data.comparativoAcumuladoAnoAnterior.diferencaReceitas >= 0 ? '+' : ''}R$ {data.comparativoAcumuladoAnoAnterior.diferencaReceitas.toFixed(2)}
                  </span>
                </p>
                <p className="text-gray-700 flex justify-between items-center">
                  Variação de Despesas
                  {getTrendIcon(data.comparativoAcumuladoAnoAnterior.tendenciaDespesas)}
                  <span className={data.comparativoAcumuladoAnoAnterior.diferencaDespesas >= 0 ? 'text-red-600' : 'text-green-600'}>
                    {data.comparativoAcumuladoAnoAnterior.diferencaDespesas >= 0 ? '+' : ''}R$ {data.comparativoAcumuladoAnoAnterior.diferencaDespesas.toFixed(2)}
                  </span>
                </p>
              </div>
            </div>
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
                    <div className="w-full bg-gray-200 rounded-full h-3">
                      <div
                        className="bg-gradient-to-r from-blue-500 to-purple-600 h-3 rounded-full"
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
          </Card>
        </>
      )}
    </div>
  );
}
