// Lista de Desejos: gerencia itens da wishlist com progresso visual e dinâmico
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useWishlist, useCreateWishlistItem, useUpdateWishlistItem, useDeleteWishlistItem } from '../lib/queries';
import Button from '../components/Button';
import Input from '../components/Input';
import Modal from '../components/Modal';
import { Plus, Edit, Trash2, Target } from 'lucide-react';
import { WishlistItem } from '../types';

export default function Wishlist() {
  const { data: wishlist, isLoading, error } = useWishlist();
  const createMutation = useCreateWishlistItem();
  const updateMutation = useUpdateWishlistItem();
  const deleteMutation = useDeleteWishlistItem();
  const [showCreate, setShowCreate] = useState(false);
  const [editingItem, setEditingItem] = useState<WishlistItem | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<number | null>(null);

  const handleCreate = (data: Omit<WishlistItem, 'id'>) => {
    createMutation.mutate(data, {
      onSuccess: () => setShowCreate(false),
    });
  };

  const handleUpdate = (data: Partial<WishlistItem>) => {
    if (editingItem) {
      updateMutation.mutate({ id: editingItem.id, data }, {
        onSuccess: () => setEditingItem(null),
      });
    }
  };

  const handleDelete = (id: number) => {
    deleteMutation.mutate(id, {
      onSuccess: () => setShowDeleteConfirm(null),
    });
  };

  if (isLoading) return <div className="p-8 text-center">Carregando desejos...</div>;
  if (error) return <div className="p-8 text-center text-red-500">Erro ao carregar wishlist</div>;

  const totalSaved = wishlist?.reduce((sum, item) => sum + item.currentAmount, 0) || 0;
  const totalTarget = wishlist?.reduce((sum, item) => sum + item.targetAmount, 0) || 0;

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-4xl font-bold text-gray-800">Lista de Desejos</h1>
          <p className="text-gray-500 mt-2">Organize seus objetivos financeiros</p>
        </div>
        <Button onClick={() => setShowCreate(true)} className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2">
          <Plus size={20} />
          Novo Desejo
        </Button>
      </div>

      {/* Summary */}
      {wishlist && wishlist.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-gradient-to-br from-blue-50 to-blue-100 border border-blue-200 p-6 rounded-lg">
            <p className="text-blue-600 text-sm font-semibold uppercase">Total de Desejos</p>
            <p className="text-3xl font-bold text-blue-700 mt-2">{wishlist.length}</p>
          </div>
          <div className="bg-gradient-to-br from-green-50 to-green-100 border border-green-200 p-6 rounded-lg">
            <p className="text-green-600 text-sm font-semibold uppercase">Já Poupado</p>
            <p className="text-3xl font-bold text-green-700 mt-2">R$ {totalSaved.toFixed(2)}</p>
          </div>
          <div className="bg-gradient-to-br from-purple-50 to-purple-100 border border-purple-200 p-6 rounded-lg">
            <p className="text-purple-600 text-sm font-semibold uppercase">Meta Total</p>
            <p className="text-3xl font-bold text-purple-700 mt-2">R$ {totalTarget.toFixed(2)}</p>
          </div>
        </div>
      )}

      {/* Wishlist Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {wishlist && wishlist.length > 0 ? (
          wishlist.map((item) => {
            const percentage = Math.min((item.currentAmount / item.targetAmount) * 100, 100);
            const remaining = item.targetAmount - item.currentAmount;
            const isComplete = percentage >= 100;

            return (
              <div key={item.id} className={`bg-white rounded-lg shadow-md hover:shadow-lg transition overflow-hidden border-l-4 ${isComplete ? 'border-l-green-500' : 'border-l-blue-500'}`}>
                {/* Header */}
                <div className={`${isComplete ? 'bg-green-50' : 'bg-blue-50'} p-4 border-b`}>
                  <div className="flex justify-between items-start mb-2">
                    <div>
                      <h3 className="text-lg font-bold text-gray-800">{item.name}</h3>
                      {item.description && <p className="text-sm text-gray-600">{item.description}</p>}
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => setEditingItem(item)}
                        className="p-2 hover:bg-blue-200 rounded transition"
                      >
                        <Edit size={16} className="text-blue-600" />
                      </button>
                      <button
                        onClick={() => setShowDeleteConfirm(item.id)}
                        className="p-2 hover:bg-red-200 rounded transition"
                      >
                        <Trash2 size={16} className="text-red-600" />
                      </button>
                    </div>
                  </div>
                </div>

                {/* Progress */}
                <div className="p-4 space-y-4">
                  {/* Amount Display */}
                  <div className="bg-gray-50 p-3 rounded">
                    <div className="flex justify-between mb-1">
                      <span className="text-sm font-medium text-gray-700">Progresso</span>
                      <span className="text-sm font-bold text-gray-800">{percentage.toFixed(0)}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-3">
                      <div
                        className={`h-3 rounded-full transition-all duration-500 ${isComplete ? 'bg-green-500' : 'bg-blue-500'}`}
                        style={{ width: `${percentage}%` }}
                      ></div>
                    </div>
                  </div>

                  {/* Amount Info */}
                  <div className="grid grid-cols-2 gap-3">
                    <div className="bg-green-50 p-3 rounded text-center">
                      <p className="text-xs text-green-600 font-semibold">Poupado</p>
                      <p className="text-lg font-bold text-green-700 mt-1">R$ {item.currentAmount.toFixed(2)}</p>
                    </div>
                    <div className="bg-blue-50 p-3 rounded text-center">
                      <p className="text-xs text-blue-600 font-semibold">Meta</p>
                      <p className="text-lg font-bold text-blue-700 mt-1">R$ {item.targetAmount.toFixed(2)}</p>
                    </div>
                  </div>

                  {/* Remaining */}
                  <div className="text-center">
                    {isComplete ? (
                      <div className="bg-green-100 p-2 rounded">
                        <p className="text-green-700 font-semibold text-sm">✓ Meta atingida!</p>
                      </div>
                    ) : (
                      <p className="text-gray-600 text-sm">
                        Faltam <span className="font-bold text-red-600">R$ {remaining.toFixed(2)}</span>
                      </p>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        ) : (
          <div className="col-span-full text-center py-12">
            <div className="flex justify-center mb-4">
              <div className="bg-blue-100 p-4 rounded-full">
                <Target size={40} className="text-blue-600" />
              </div>
            </div>
            <p className="text-gray-500 text-lg">Nenhum desejo ainda</p>
            <p className="text-gray-400 text-sm mt-1">Crie seu primeiro desejo para começar a poupar!</p>
          </div>
        )}
      </div>

      {/* Create Modal */}
      <Modal isOpen={showCreate} onClose={() => setShowCreate(false)} title="Novo Desejo">
        <WishlistForm onSubmit={handleCreate} />
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={!!editingItem} onClose={() => setEditingItem(null)} title="Editar Desejo">
        {editingItem && <WishlistForm onSubmit={handleUpdate} defaultValues={editingItem} />}
      </Modal>

      {/* Delete Modal */}
      <Modal isOpen={!!showDeleteConfirm} onClose={() => setShowDeleteConfirm(null)} title="Confirmar Exclusão">
        <p>Tem certeza que deseja excluir este desejo?</p>
        <div className="flex gap-2 mt-4">
          <Button onClick={() => handleDelete(showDeleteConfirm!)} variant="danger" disabled={deleteMutation.isPending}>
            {deleteMutation.isPending ? 'Excluindo...' : 'Excluir'}
          </Button>
          <Button onClick={() => setShowDeleteConfirm(null)} variant="secondary">Cancelar</Button>
        </div>
      </Modal>
    </div>
  );
}

function WishlistForm({ onSubmit, defaultValues }: { onSubmit: (data: any) => void; defaultValues?: WishlistItem }) {
  const { register, handleSubmit } = useForm({
    defaultValues,
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <Input label="Nome do Desejo" placeholder="Ex: Notebook Gamer" {...register('name', { required: true })} />
      <Input label="Descrição" placeholder="Ex: Para estudos e trabalho" {...register('description')} />
      <Input label="Meta (R$)" type="number" step="0.01" placeholder="0.00" {...register('targetAmount', { required: true, valueAsNumber: true })} />
      <Input label="Já Poupado (R$)" type="number" step="0.01" placeholder="0.00" {...register('currentAmount', { required: true, valueAsNumber: true })} />
      <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white">
        Salvar Desejo
      </Button>
    </form>
  );
}
