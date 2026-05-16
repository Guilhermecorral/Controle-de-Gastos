// Página de login: autentica usuário com design moderno e dinâmico
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, Link } from 'react-router-dom';
import { useLogin } from '../lib/queries';
import { useAuthStore } from '../store/auth';
import Input from '../components/Input';
import Button from '../components/Button';
import { Eye, EyeOff, Wallet } from 'lucide-react';
import DOMPurify from 'dompurify';

const loginSchema = z.object({
  email: z.string().email('Email inválido'),
  password: z.string().min(1, 'Senha obrigatória'),
});

type LoginForm = z.infer<typeof loginSchema>;

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [showPassword, setShowPassword] = useState(false);
  const loginMutation = useLogin();

  const { register, handleSubmit, formState: { errors }, setError } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = (data: LoginForm) => {
    loginMutation.mutate(data, {
      onSuccess: (response) => {
        login(response);
        navigate('/dashboard');
      },
      onError: () => {
        setError('root', { message: 'Email ou senha incorretos' });
      },
    });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-600 via-blue-500 to-purple-600 flex items-center justify-center p-4">
      {/* Background Animation */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute w-96 h-96 bg-blue-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse top-0 left-0"></div>
        <div className="absolute w-96 h-96 bg-purple-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse top-0 right-0"></div>
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
            <h1 className="text-3xl font-bold">Controle de Gastos</h1>
            <p className="text-blue-100 mt-2">Gerencie suas finanças com inteligência</p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit(onSubmit)} className="p-8 space-y-6">
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

            {errors.root && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm">
                {DOMPurify.sanitize(errors.root.message!)}
              </div>
            )}

            <Button
              type="submit"
              disabled={loginMutation.isPending}
              className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-semibold py-3 rounded-lg transition transform hover:scale-105"
            >
              {loginMutation.isPending ? 'Entrando...' : 'Entrar'}
            </Button>
          </form>

          {/* Footer */}
          <div className="px-8 pb-8 space-y-4">
            <div className="text-center">
              <p className="text-gray-600">
                Não tem conta?{' '}
                <Link to="/register" className="text-blue-600 hover:text-blue-700 font-semibold">
                  Registre-se
                </Link>
              </p>
            </div>

            <div className="flex gap-2 justify-center text-xs text-gray-400">
              <Link to="/terms" className="hover:text-gray-600">Termos</Link>
              <span>•</span>
              <Link to="/privacy" className="hover:text-gray-600">Privacidade</Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
