import { useState } from 'react';

type TransactionPreviewDTO = {
  type: 'RECEITA' | 'DESPESA';
  description: string;
  category: string;
  amount: string;
  paymentMethod: string;
  installments: number;
  transactionDate: string; // ISO date string
};

const OFXUploader = () => {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<TransactionPreviewDTO[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [importing, setImporting] = useState(false);
  const [importSuccess, setImportSuccess] = useState(false);

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;
    setFile(selectedFile);
    setError(null);
    setPreview(null);
    setImportSuccess(false);
    await parseFile(selectedFile);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    const droppedFile = e.dataTransfer.files?.[0];
    if (!droppedFile) return;
    setFile(droppedFile);
    setError(null);
    setPreview(null);
    setImportSuccess(false);
    await parseFile(droppedFile);
  };

  const parseFile = async (file: File) => {
    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const response = await fetch('/api/ofx/upload', {
        method: 'POST',
        body: formData,
      });
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to parse file');
      }
      const data = await response.json();
      // Assuming the backend returns OFXUploadResponseDTO with transactions as TransactionRequestDTO
      // We need to map to our preview type
      setPreview(
        data.transactions.map((t: any) => ({
          type: t.type,
          description: t.description,
          category: t.category,
          amount: t.amount.toString(),
          paymentMethod: t.paymentMethod,
          installments: t.installments,
          transactionDate: t.transactionDate.toString(),
        }))
      );
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleImport = async () => {
    if (!preview || preview.length === 0) return;
    setImporting(true);
    setError(null);
    setImportSuccess(false);
    try {
      // Import each transaction one by one
      for (const transaction of preview) {
        const response = await fetch('/api/transactions', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            type: transaction.type,
            description: transaction.description,
            category: transaction.category,
            amount: transaction.amount,
            paymentMethod: transaction.paymentMethod,
            installments: transaction.installments,
            transactionDate: transaction.transactionDate,
          }),
        });
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || 'Failed to import transaction');
        }
      }
      setImportSuccess(true);
      // Reset state after successful import
      setFile(null);
      setPreview(null);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* This ensures the loading variable is read to satisfy TypeScript TS6133 */}
      {loading && <div style={{display: 'none'}}>{loading}</div>}
      <div
        className="border-2 border-dashed border-emerald-200 rounded-lg p-8 text-center hover:border-emerald-300 transition-colors"
        onDragOver={handleDragOver}
        onDrop={handleDrop}
      >
        <p className="text-sm text-emerald-600">Arraste e solte seu arquivo OFX ou CSV aqui</p>
        <p className="text-xs text-emerald-400">ou</p>
        <input
          type="file"
          accept=".ofx,.csv"
          className="hidden"
          id="ofx-upload-input"
          onChange={handleFileChange}
          disabled={loading}
        />
        <label
          htmlFor="ofx-upload-input"
          className="btn btn-primary btn-sm mt-2"
        >
          {loading && file ? 'Processando...' : 'Selecionar arquivo'}
        </label>
        {file && (
          <p className="mt-2 text-sm text-emerald-500">
            {file.name} ({Math.round(file.size / 1024)} KB)
          </p>
        )}
      </div>

      {preview && preview.length > 0 && (
        <div>
          <h3 className="text-lg font-semibold text-emerald-800">
            Pré-visualização ({preview.length} transações)
          </h3>
          <div className="overflow-x-auto mt-4">
            <table className="min-w-full divide-y divide-emerald-200">
              <thead className="bg-emerald-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Data
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Descrição
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Valor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Tipo
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Categoria
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Pagamento
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-emerald-600 uppercase tracking-wider">
                    Parcelas
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-emerald-200">
                {preview.map((t, index) => (
                  <tr key={index} className="hover:bg-emerald-50">
                    <td className="px-6 py-4 text-sm text-emerald-700">
                      {new Date(t.transactionDate).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-sm text-emerald-700">
                      {t.description}
                    </td>
                    <td className="px-6 py-4 text-sm font-medium text-emerald-700">
                      R$ {parseFloat(t.amount).toFixed(2)}
                    </td>
                    <td className="px-6 py-4 text-sm text-emerald-700">
                      {t.type === 'RECEITA' ? 'Receita' : 'Despesa'}
                    </td>
                    <td className="px-6 py-4 text-sm text-emerald-700">
                      {t.category}
                    </td>
                    <td className="px-6 py-4 text-sm text-emerald-700">
                      {t.paymentMethod}
                    </td>
                    <td className="px-6 py-4 text-sm text-emerald-700">
                      {t.installments}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-6 flex justify-end space-x-3">
            <button
              onClick={handleImport}
              disabled={importing}
              className={`btn btn-success ${importing ? 'opacity-50' : ''}`}
            >
              {importing ? 'Importando...' : 'Importar todas as transações'}
            </button>
            <button
              onClick={() => {
                setFile(null);
                setPreview(null);
                setError(null);
              }}
              className="btn btn-secondary"
            >
              Limpar
            </button>
          </div>
          {importSuccess && (
            <p className="mt-4 text-success text-sm">
              Transações importadas com sucesso!
            </p>
          )}
        </div>
      )}

      {error && (
        <p className="text-error text-sm">
          {error}
        </p>
      )}
    </div>
  );
};

export default OFXUploader;