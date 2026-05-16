// Criar Nova Transação: formulário para adicionar receita ou despesa com design funcional
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate } from 'react-router-dom';
import { useCreateTransaction } from '../lib/queries';
import Input from '../components/Input';
import Select from '../components/Select';
import Button from '../components/Button';
import { Plus, ArrowUpCircle, ArrowDownCircle } from 'lucide-react';
import DOMPurify from 'dompurify';
import { useState } from 'react';

const transactionSchema = z.object({
  type: z.enum(['RECEITA', 'DESPESA']),
  description: z.string().min(1, 'Descrição obrigatória'),
  category: z.string().min(1, 'Categoria obrigatória'),
  amount: z.string().regex(/^\d+(\.\d{1,2})?$/, 'Valor inválido'),
  paymentMethod: z.string().min(1, 'Método de pagamento obrigatório'),
  transactionDate: z.string().min(1, 'Data obrigatória'),
});

type TransactionForm = z.infer<typeof transactionSchema>;

export default function CreateTransaction() {
  const navigate = useNavigate();
  const createMutation = useCreateTransaction();
  const [transactionType, setTransactionType] = useState<'RECEITA' | 'DESPESA'>('DESPESA');

  const { register, handleSubmit, formState: { errors }, setError, watch } = useForm<TransactionForm>({
    resolver: zodResolver(transactionSchema),
    defaultValues: { type: transactionType, transactionDate: new Date().toISOString().split('T')[0] },
  });

  const selectedType = watch('type') || transactionType;

  const onSubmit = (data: TransactionForm) => {
    createMutation.mutate({
      ...data,
      amount: parseFloat(data.amount),
    }, {
      onSuccess: () => navigate('/transactions'),
      onError: () => setError('root', { message: 'Erro ao criar transação' }),
    });
  };

  const categoryOptions = {
    RECEITA: [
      { value: 'OUTROS', label: 'Salário' },
      { value: 'LAZER', label: 'Freelance' },
      { value: 'EDUCACAO', label: 'Investimento' },
      { value: 'OUTROS', label: 'Outros' },
    ],
    DESPESA: [
      { value: 'ALIMENTACAO', label: 'Alimentação' },
      { value: 'TRANSPORTE', label: 'Transporte' },
      { value: 'MORADIA', label: 'Moradia' },
      { value: 'SAUDE', label: 'Saúde' },
      { value: 'LAZER', label: 'Lazer' },
      { value: 'EDUCACAO', label: 'Educação' },
      { value: 'COMPRAS', label: 'Compras' },
      { value: 'OUTROS', label: 'Outros' },
    ],
  };

  const paymentOptions = [
    { value: 'PIX', label: 'PIX' },
    { value: 'CARTAO_DEBITO', label: 'Cartão de Débito' },
    { value: 'CARTAO_CREDITO_AVISTA', label: 'Cartão de Crédito à Vista' },
    { value: 'CARTAO_CREDITO_PARCELADO', label: 'Cartão de Crédito Parcelado' },
    { value: 'DINHEIRO', label: 'Dinheiro' },
  ];

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold text-gray-800">Nova Transação</h1>
        <p className="text-gray-500 mt-2">Registre uma nova receita ou despesa</p>
      </div>

      {/* Type Selection */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <button
          onClick={() => setTransactionType('RECEITA')}
          className={`p-6 rounded-lg border-2 transition transform hover:scale-105 ${
            selectedType === 'RECEITA'
              ? 'border-green-500 bg-green-50'
              : 'border-gray-200 bg-white hover:border-green-200'
          }`}
        >
          <div className="flex items-center justify-center mb-2">
            <div className={`p-3 rounded-full ${selectedType === 'RECEITA' ? 'bg-green-200' : 'bg-gray-100'}`}>
              <ArrowUpCircle size={32} className={selectedType === 'RECEITA' ? 'text-green-600' : 'text-gray-600'} />
            </div>
          </div>
          <p className={`font-semibold text-lg ${selectedType === 'RECEITA' ? 'text-green-600' : 'text-gray-600'}`}>
            Receita
          </p>
          <p className="text-sm text-gray-500 mt-1">Dinheiro que entra</p>
        </button>

        <button
          onClick={() => setTransactionType('DESPESA')}
          className={`p-6 rounded-lg border-2 transition transform hover:scale-105 ${
            selectedType === 'DESPESA'
              ? 'border-red-500 bg-red-50'
              : 'border-gray-200 bg-white hover:border-red-200'
          }`}
        >
          <div className="flex items-center justify-center mb-2">
            <div className={`p-3 rounded-full ${selectedType === 'DESPESA' ? 'bg-red-200' : 'bg-gray-100'}`}>
              <ArrowDownCircle size={32} className={selectedType === 'DESPESA' ? 'text-red-600' : 'text-gray-600'} />
            </div>
          </div>
          <p className={`font-semibold text-lg ${selectedType === 'DESPESA' ? 'text-red-600' : 'text-gray-600'}`}>
            Despesa
          </p>
          <p className="text-sm text-gray-500 mt-1">Dinheiro que sai</p>
        </button>
      </div>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} className="bg-white p-8 rounded-lg shadow-md max-w-2xl mx-auto space-y-6 w-full">
        <input type="hidden" {...register('type')} value={selectedType} />

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Input
            label="Descrição"
            placeholder="Ex: Almoço no trabalho"
            {...register('description')}
            error={errors.description?.message}
          />
          <Select
            label="Categoria"
            options={categoryOptions[selectedType]}
            {...register('category')}
            error={errors.category?.message}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Input
            label="Valor (R$)"
            type="number"
            step="0.01"
            placeholder="0.00"
            {...register('amount')}
            error={errors.amount?.message}
          />
          <Select
            label="Método de Pagamento"
            options={paymentOptions}
            {...register('paymentMethod')}
            error={errors.paymentMethod?.message}
          />
        </div>

        <Input
          label="Data da Transação"
          type="date"
          {...register('transactionDate')}
          error={errors.transactionDate?.message}
        />

        {errors.root && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
            {DOMPurify.sanitize(errors.root.message!)}
          </div>
        )}

        <div className="flex gap-4 pt-4">
          <Button
            type="submit"
            disabled={createMutation.isPending}
            className={`flex-1 flex items-center justify-center gap-2 text-white font-semibold py-3 rounded-lg transition ${
              selectedType === 'RECEITA'
                ? 'bg-green-600 hover:bg-green-700'
                : 'bg-red-600 hover:bg-red-700'
            }`}
          >
            <Plus size={20} />
            {createMutation.isPending ? 'Criando...' : 'Criar Transação'}
          </Button>
          <Button
            type="button"
            onClick={() => navigate('/transactions')}
            className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold py-3 rounded-lg transition"
          >
            Cancelar
          </Button>
        </div>
      </form>
    </div>
  );
}
