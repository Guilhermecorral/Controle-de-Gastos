// Lista de Transações: exibe, filtra, edita e deleta transações com interface dinâmica
import { useState } from 'react';
import { useTransactions, useDeleteTransaction, useUpdateTransaction } from '../lib/queries';
import Card from '../components/Card';
import Button from '../components/Button';
import Select from '../components/Select';
import Modal from '../components/Modal';
import Input from '../components/Input';
import { Edit, Trash2, Plus, Filter } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Transaction } from '../types';

export default function Transactions() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<{ type?: string; category?: string; startDate?: string; endDate?: string; page?: number }>({});
  const { data, isLoading, error } = useTransactions(filters);
  const deleteMutation = useDeleteTransaction();
  const updateMutation = useUpdateTransaction();
  const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<number | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  const typeOptions = [
    { value: '', label: 'Todos os Tipos' },
    { value: 'RECEITA', label: 'Receitas' },
    { value: 'DESPESA', label: 'Despesas' },
  ];

  const categoryOptions = [
    { value: '', label: 'Todas as Categorias' },
    { value: 'ALIMENTACAO', label: 'Alimentação' },
    { value: 'TRANSPORTE', label: 'Transporte' },
    { value: 'MORADIA', label: 'Moradia' },
    { value: 'SAUDE', label: 'Saúde' },
    { value: 'LAZER', label: 'Lazer' },
    { value: 'EDUCACAO', label: 'Educação' },
    { value: 'COMPRAS', label: 'Compras' },
    { value: 'OUTROS', label: 'Outros' },
  ];

  const handleEdit = (transaction: Transaction) => {
    setEditingTransaction(transaction);
  };

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id, {
      onSuccess: () => setShowDeleteConfirm(null),
    });
  };

  const handleUpdate = (data: Partial<Transaction>) => {
    if (editingTransaction) {
      updateMutation.mutate({ id: editingTransaction.id, data }, {
        onSuccess: () => setEditingTransaction(null),
      });
    }
  };

  if (isLoading) return <div className="p-8 text-center">Carregando transações...</div>;
  if (error) return <div className="p-8 text-center text-red-500">Erro ao carregar transações</div>;

  const hasActiveFilters = Object.values(filters).some(v => v);

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-4xl font-bold text-gray-800">Transações</h1>
          <p className="text-gray-500 mt-2">Histórico completo de entradas e saídas</p>
        </div>
        <Button onClick={() => navigate('/transactions/new')} className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2">
          <Plus size={20} />
          Nova Transação
        </Button>
      </div>

      {/* Filters */}
      <div className="bg-white p-4 rounded-lg shadow-md">
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="flex items-center gap-2 text-blue-600 hover:text-blue-700 font-semibold"
        >
          <Filter size={20} />
          {showFilters ? 'Ocultar Filtros' : 'Mostrar Filtros'}
          {hasActiveFilters && <span className="bg-blue-600 text-white text-xs px-2 py-1 rounded-full">Ativo</span>}
        </button>

        {showFilters && (
          <div className="mt-4 grid grid-cols-1 md:grid-cols-4 gap-4">
            <Select
              label="Tipo"
              options={typeOptions}
              value={filters.type || ''}
              onChange={(e) => setFilters({ ...filters, type: e.target.value })}
            />
            <Select
              label="Categoria"
              options={categoryOptions}
              value={filters.category || ''}
              onChange={(e) => setFilters({ ...filters, category: e.target.value })}
            />
            <Input
              label="Data Início"
              type="date"
              value={filters.startDate || ''}
              onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
            />
            <Input
              label="Data Fim"
              type="date"
              value={filters.endDate || ''}
              onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
            />
          </div>
        )}
      </div>

      {/* Transactions List */}
      <Card className="hover:shadow-lg transition">
        {data?.content && data.content.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b-2 border-gray-200">
                  <th className="text-left p-4 font-semibold text-gray-700">Data</th>
                  <th className="text-left p-4 font-semibold text-gray-700">Descrição</th>
                  <th className="text-left p-4 font-semibold text-gray-700">Tipo</th>
                  <th className="text-left p-4 font-semibold text-gray-700">Categoria</th>
                  <th className="text-right p-4 font-semibold text-gray-700">Valor</th>
                  <th className="text-center p-4 font-semibold text-gray-700">Ações</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((t) => (
                  <tr key={t.id} className="border-b hover:bg-gray-50 transition">
                    <td className="p-4 text-sm text-gray-600">{new Date(t.transactionDate).toLocaleDateString('pt-BR')}</td>
                    <td className="p-4 font-medium text-gray-800">{t.description}</td>
                    <td className="p-4">
                      <span className={`px-3 py-1 rounded-full text-sm font-semibold ${t.type === 'RECEITA' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {t.type}
                      </span>
                    </td>
                    <td className="p-4 text-gray-600">{t.category}</td>
                    <td className={`p-4 text-right font-bold ${t.type === 'RECEITA' ? 'text-green-600' : 'text-red-600'}`}>
                      {t.type === 'RECEITA' ? '+' : '-'} R$ {t.amount.toFixed(2)}
                    </td>
                    <td className="p-4 text-center">
                      <div className="flex gap-2 justify-center">
                        <button
                          onClick={() => handleEdit(t)}
                          className="p-2 hover:bg-blue-100 rounded transition text-blue-600"
                        >
                          <Edit size={16} />
                        </button>
                        <button
                          onClick={() => setShowDeleteConfirm(t.id)}
                          className="p-2 hover:bg-red-100 rounded transition text-red-600"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="text-center py-12">
            <p className="text-gray-500 text-lg">Nenhuma transação encontrada</p>
            <Button
              onClick={() => navigate('/transactions/new')}
              className="mt-4 bg-blue-600 hover:bg-blue-700 text-white"
            >
              Criar primeira transação
            </Button>
          </div>
        )}
      </Card>

      {/* Edit Modal */}
      <Modal isOpen={!!editingTransaction} onClose={() => setEditingTransaction(null)} title="Editar Transação">
        {editingTransaction && (
          <form onSubmit={(e) => { e.preventDefault(); const formData = new FormData(e.currentTarget); handleUpdate({ description: Array.from(formData.entries()).find(([k]) => k === 'description')?.[1] as string }); }} className="space-y-4">
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">Descrição</label>
              <input
                name="description"
                defaultValue={editingTransaction.description}
                className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
            </div>
            <Button type="submit" disabled={updateMutation.isPending} className="w-full bg-blue-600 hover:bg-blue-700 text-white">
              {updateMutation.isPending ? 'Salvando...' : 'Salvar'}
            </Button>
          </form>
        )}
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal isOpen={!!showDeleteConfirm} onClose={() => setShowDeleteConfirm(null)} title="Confirmar Exclusão">
        <p className="text-gray-700 mb-4">Tem certeza que deseja excluir esta transação? Esta ação não pode ser desfeita.</p>
        <div className="flex gap-2">
          <Button
            onClick={() => handleDelete(showDeleteConfirm!)}
            variant="danger"
            disabled={deleteMutation.isPending}
            className="flex-1"
          >
            {deleteMutation.isPending ? 'Excluindo...' : 'Excluir'}
          </Button>
          <Button onClick={() => setShowDeleteConfirm(null)} variant="secondary" className="flex-1">
            Cancelar
          </Button>
        </div>
      </Modal>
    </div>
  );
}

