// Configuração do Usuário: edita perfil, senha e gerencia conta
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useUser, useUpdateUser } from '../lib/queries';
import { useAuthStore } from '../store/auth';
import { useNavigate } from 'react-router-dom';
import Input from '../components/Input';
import Button from '../components/Button';
import Card from '../components/Card';
import Modal from '../components/Modal';
import { Settings, Lock, LogOut, Trash2, Eye, EyeOff } from 'lucide-react';
import DOMPurify from 'dompurify';
import { useState } from 'react';

const profileSchema = z.object({
  name: z.string().min(1, 'Nome obrigatório'),
  email: z.string().email('Email inválido'),
  password: z.string().optional(),
});

type ProfileForm = z.infer<typeof profileSchema>;

export default function Profile() {
  const { data: user, isLoading } = useUser();
  const updateMutation = useUpdateUser();
  const { logout, user: authUser } = useAuthStore();
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const { register, handleSubmit, formState: { errors }, setError } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: user ? { name: user.name, email: user.email } : {},
  });

  const onSubmit = (data: ProfileForm) => {
    updateMutation.mutate(data, {
      onSuccess: () => alert('Perfil atualizado com sucesso'),
      onError: () => setError('root', { message: 'Erro ao atualizar perfil' }),
    });
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (isLoading) return <div className="p-8 text-center">Carregando perfil...</div>;

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold text-gray-800">Configurações</h1>
        <p className="text-gray-500 mt-2">Gerencie seu perfil e segurança</p>
      </div>

      {/* Profile Section */}
      <Card className="hover:shadow-lg transition">
        <div className="flex items-center gap-4 mb-6 pb-6 border-b">
          <div className="bg-gradient-to-br from-blue-500 to-purple-600 p-4 rounded-full text-white">
            <Settings size={32} />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-gray-800">{user?.name}</h2>
            <p className="text-gray-500">{user?.email}</p>
          </div>
        </div>

        {/* Edit Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <Input
              label="Nome"
              {...register('name')}
              error={errors.name?.message}
            />
            <Input
              label="Email"
              type="email"
              {...register('email')}
              error={errors.email?.message}
            />
          </div>

          <div className="relative">
            <Input
              label="Nova Senha (deixe em branco para não alterar)"
              type={showPassword ? 'text' : 'password'}
              {...register('password')}
              error={errors.password?.message}
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-11 text-gray-400 hover:text-gray-600"
            >
              {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>

          {errors.root && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
              {DOMPurify.sanitize(errors.root.message!)}
            </div>
          )}

          <Button
            type="submit"
            disabled={updateMutation.isPending}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition"
          >
            {updateMutation.isPending ? 'Salvando...' : 'Salvar Alterações'}
          </Button>
        </form>
      </Card>

      {/* Security Section */}
      <Card className="border-l-4 border-l-red-500 hover:shadow-lg transition">
        <div className="flex items-center gap-3 mb-6">
          <div className="bg-red-100 p-3 rounded-full">
            <Lock size={24} className="text-red-600" />
          </div>
          <h3 className="text-xl font-bold text-gray-800">Segurança e Privacidade</h3>
        </div>

        <div className="space-y-3">
          <div className="p-4 bg-gray-50 rounded-lg flex justify-between items-center">
            <div>
              <p className="font-semibold text-gray-800">Sessão Ativa</p>
              <p className="text-sm text-gray-600">Conectado como {authUser?.email}</p>
            </div>
            <Button
              onClick={handleLogout}
              className="bg-red-600 hover:bg-red-700 text-white flex items-center gap-2"
            >
              <LogOut size={16} />
              Sair
            </Button>
          </div>

          <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p className="font-semibold text-yellow-800 mb-2">Excluir Conta</p>
            <p className="text-sm text-yellow-700 mb-3">
              Ao excluir sua conta, todos os seus dados serão permanentemente removidos. Esta ação não pode ser desfeita.
            </p>
            <Button
              onClick={() => setShowDeleteConfirm(true)}
              className="bg-red-600 hover:bg-red-700 text-white flex items-center gap-2"
            >
              <Trash2 size={16} />
              Excluir Conta
            </Button>
          </div>
        </div>
      </Card>

      {/* Delete Confirmation Modal */}
      <Modal isOpen={showDeleteConfirm} onClose={() => setShowDeleteConfirm(false)} title="Excluir Conta">
        <div className="space-y-4">
          <p className="text-gray-700">
            Tem certeza que deseja excluir sua conta? Esta ação é irreversível e todos os seus dados serão perdidos.
          </p>
          <div className="flex gap-2">
            <Button
              onClick={() => setShowDeleteConfirm(false)}
              className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-800"
            >
              Cancelar
            </Button>
            <Button
              onClick={() => setShowDeleteConfirm(false)}
              className="flex-1 bg-red-600 hover:bg-red-700 text-white"
            >
              Excluir Permanentemente
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
