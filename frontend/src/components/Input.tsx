// Componente reutilizável para inputs, com validação visual
import { forwardRef } from 'react';

interface InputProps {
  label?: string;
  type?: string;
  placeholder?: string;
  error?: string;
  value?: string | number;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  step?: string;
  className?: string;
}

const Input = forwardRef<HTMLInputElement, InputProps>(({ label, type = 'text', placeholder, error, value, onChange, step, className = '', ...props }, ref) => {
  return (
    <div className="mb-4">
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <input
        ref={ref}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        step={step}
        className={`w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 min-h-[44px] ${
          error ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
        } ${className}`}
        {...props}
      />
      {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
