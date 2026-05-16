// Componente para selects, com opções dinâmicas
import { forwardRef } from 'react';

interface SelectProps {
  label?: string;
  options: { value: string; label: string }[];
  error?: string;
  value?: string;
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void;
  className?: string;
}

const Select = forwardRef<HTMLSelectElement, SelectProps>(({ label, options, error, value, onChange, className = '', ...props }, ref) => {
  return (
    <div className="mb-4">
      {label && <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>}
      <select
        ref={ref}
        value={value}
        onChange={onChange}
        className={`w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 min-h-[44px] ${
          error ? 'border-red-500 focus:ring-red-500' : 'border-gray-300 focus:ring-blue-500'
        } ${className}`}
        {...props}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error && <p className="text-red-500 text-sm mt-1">{error}</p>}
    </div>
  );
});

Select.displayName = 'Select';

export default Select;
