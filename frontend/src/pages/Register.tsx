// Página de registro: cria novo usuário com design moderno
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useRegister } from '../lib/queries';
import { useAuthStore } from '../store/auth';
import Input from '../components/Input';
import Button from '../components/Button';
import { Eye, EyeOff, Wallet, CheckCircle } from 'lucide-react';
import { useState } from 'react';
import DOMPurify from 'dompurify';

const registerSchema = z.object({
  name: z.string().min(1, 'Nome obrigatório'),
  email: z.string().email('Email inválido'),
  password: z.string().min(6, 'Senha deve ter no mínimo 6 caracteres'),
});

type RegisterForm = z.infer<typeof registerSchema>;

export default function Register() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [showPassword, setShowPassword] = useState(false);
  const registerMutation = useRegister();

  const { register, handleSubmit, formState: { errors }, setError } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = (data: RegisterForm) => {
    registerMutation.mutate(data, {
      onSuccess: (response) => {
        login(response);
        navigate('/dashboard');
      },
      onError: (error: any) => {
        if (error.response?.data?.message) {
          setError('root', { message: error.response.data.message });
        } else {
          setError('root', { message: 'Erro ao registrar. Tente novamente.' });
        }
      },
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-600 via-blue-500 to-purple-600 flex items-center justify-center p-4">
      {/* Background Animation */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute w-96 h-96 bg-blue-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse bottom-0 right-0"></div>
        <div className="absolute w-96 h-96 bg-purple-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse bottom-20 left-20"></div>
      </div>

      <div className="relative w-full max-w-md">
        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-purple-600 p-8 text-white text-center">
            <div className="flex justify-center mb-4">
              <div className="bg-white bg-opacity-20 p-3 rounded-full">
                <Wallet size={32} />
              </div>
            </div>
            <h1 className="text-3xl font-bold">Criar Conta</h1>
            <p className="text-blue-100 mt-2">Comece a gerenciar suas finanças</p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit(onSubmit)} className="p-8 space-y-6">
            <Input
              label="Nome Completo"
              type="text"
              placeholder="Seu Nome"
              {...register('name')}
              error={errors.name?.message}
            />

            <Input
              label="Email"
              type="email"
              placeholder="seu@email.com"
              {...register('email')}
              error={errors.email?.message}
            />

            <div className="relative">
              <Input
                label="Senha"
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
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

            {/* Requirements */}
            <div className="bg-blue-50 p-4 rounded-lg">
              <p className="text-xs text-blue-700 font-semibold mb-2">Requisitos de Senha:</p>
              <ul className="space-y-1 text-xs text-blue-600">
                <li className="flex items-center gap-2">
                  <CheckCircle size={14} className="text-green-600" />
                  Mínimo 6 caracteres
                </li>
              </ul>
            </div>

            {errors.root && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {DOMPurify.sanitize(errors.root.message!)}
              </div>
            )}

            <Button
              type="submit"
              disabled={registerMutation.isPending}
              className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-semibold py-3 rounded-lg transition transform hover:scale-105"
            >
              {registerMutation.isPending ? 'Criando Conta...' : 'Criar Conta'}
            </Button>
          </form>

          {/* Footer */}
          <div className="px-8 pb-8 space-y-4 border-t border-gray-200">
            <div className="text-center">
              <p className="text-gray-600">
                Já tem conta?{' '}
                <Link to="/login" className="text-blue-600 hover:text-blue-700 font-semibold">
                  Entrar
                </Link>
              </p>
            </div>

            <div className="text-center text-xs text-gray-400">
              <p>Ao criar uma conta, você concorda com nossos</p>
              <div className="flex gap-2 justify-center mt-1">
                <Link to="/terms" className="hover:text-gray-600">Termos de Uso</Link>
                <span>•</span>
                <Link to="/privacy" className="hover:text-gray-600">Privacidade</Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
