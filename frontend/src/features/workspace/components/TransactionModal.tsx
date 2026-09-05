import { Dispatch, SetStateAction, useDeferredValue, useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { Category, InvestmentAssetSearchResponse, InvestmentAssetType, InvestmentPositionResponse, InvestmentTradeRequest, PaymentMethod } from '../../../types';
import { categoryLabels, paymentMethodLabels } from '../../../lib/mockFinance';
import { useInvestmentAssetSearchQuery, useInvestmentPortfolioQuery, useRecordInvestmentTradeMutation } from '../../../lib/queries';
import { getApiErrorMessage } from '../../../lib/httpErrors';
import { Field, SelectField } from '../../shared/ui';
import { TransactionDraft } from '../types';
import OFXUploader from '../../ofx-upload/components/OFXUploader';

type TransactionModalProps = {
  isOpen: boolean;
  draft: TransactionDraft;
  receiptFile: File | null;
  suggestion: Category | null;
  onDraftChange: Dispatch<SetStateAction<TransactionDraft>>;
  onReceiptFileChange: (file: File | null) => void;
  onDescriptionChange: (value: string) => void;
  onCategoryTouched: (value: boolean) => void;
  onSubmit: () => void;
  onTransactionsImported: (importedTransactions: number) => void;
  onInvestmentRecorded: (operation: string) => void;
  onClose: () => void;
};

export default function TransactionModal({
  isOpen,
  draft,
  receiptFile,
  suggestion,
  onDraftChange,
  onReceiptFileChange,
  onDescriptionChange,
  onCategoryTouched,
  onSubmit,
  onTransactionsImported,
  onInvestmentRecorded,
  onClose,
}: TransactionModalProps) {
  const [mode, setMode] = useState<'single' | 'batch'>('single');

  useEffect(() => {
    if (!isOpen) {
      setMode('single');
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  const isInstallmentPayment = draft.paymentMethod === 'CARTAO_CREDITO_PARCELADO';

  return (
    <div className="fixed inset-0 z-40 overflow-y-auto bg-slate-950/45 p-4">
      <div className="mx-auto flex min-h-full items-center justify-center">
        <div
          className={`max-h-[calc(100vh-2rem)] w-full overflow-y-auto rounded-[32px] bg-white p-6 shadow-[0_28px_80px_rgba(15,23,42,0.22)] transition-all ${
            mode === 'batch' ? 'max-w-6xl' : 'max-w-2xl'
          }`}
        >
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-emerald-600">
              {mode === 'single' ? 'Lançamento rápido' : 'Importação em lote'}
            </p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-900">
              {mode === 'single' ? (draft.category === 'INVESTIMENTO' ? 'Novo investimento' : draft.type === 'RECEITA' ? 'Nova receita' : 'Nova despesa') : 'Importar extrato bancário'}
            </h3>
            <p className="mt-2 text-sm leading-7 text-slate-600">
              {mode === 'single'
                ? 'Registre uma movimentação com sugestão automática de categoria, parcelamento e anexo fiscal.'
                : 'Envie OFX, CSV, TSV ou Excel, revise cada sugestão e confirme apenas as movimentações que deseja importar.'}
            </p>
          </div>
          <button
            className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
            onClick={onClose}
            type="button"
          >
            Fechar
          </button>
        </div>

        <div className="mt-6 grid grid-cols-2 gap-2 rounded-[20px] bg-slate-100 p-1.5">
          <ModeButton active={mode === 'single'} label="Lançamento único" onClick={() => setMode('single')} />
          <ModeButton active={mode === 'batch'} label="Importar planilha/OFX" onClick={() => setMode('batch')} />
        </div>

        {mode === 'batch' ? (
          <div className="mt-6">
            <OFXUploader compact onImported={onTransactionsImported} />
          </div>
        ) : draft.category === 'INVESTIMENTO' ? (
          <InvestmentOperationForm
            onCancel={() => onDraftChange((current) => ({ ...current, type: 'DESPESA', category: 'OUTROS' }))}
            onRecorded={onInvestmentRecorded}
          />
        ) : (
          <>

        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <SelectField
            label="Tipo"
            options={[
              { value: 'DESPESA', label: 'Despesa' },
              { value: 'RECEITA', label: 'Receita' },
              { value: 'INVESTIMENTO', label: 'Investimento' },
            ]}
            value={draft.type}
            onChange={(value) => {
              onCategoryTouched(true);
              onDraftChange((currentValue) => ({ ...currentValue,
                type: value === 'INVESTIMENTO' ? 'DESPESA' : value as 'RECEITA' | 'DESPESA',
                category: value === 'INVESTIMENTO' ? 'INVESTIMENTO' : 'OUTROS' }));
            }}
          />
          <Field label="Data">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              type="date"
              value={draft.transactionDate}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, transactionDate: event.target.value }))}
            />
          </Field>
          <Field label="Descrição">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="Ex.: consulta médica, lanche, aluguel"
              value={draft.description}
              onChange={(event) => onDescriptionChange(event.target.value)}
            />
          </Field>
          <Field label="Valor">
            <input
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
              placeholder="0,00"
              type="number"
              value={draft.amount}
              onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, amount: event.target.value }))}
            />
          </Field>
          <SelectField
            label="Categoria"
            options={Object.entries(categoryLabels).filter(([value]) => value !== 'INVESTIMENTO').map(([value, label]) => ({ value, label }))}
            value={draft.category}
            onChange={(value) => {
              onCategoryTouched(true);
              onDraftChange((currentValue) => ({ ...currentValue, category: value as Category }));
            }}
          />
          <SelectField
            label="Pagamento"
            options={Object.entries(paymentMethodLabels).map(([value, label]) => ({ value, label }))}
            value={draft.paymentMethod}
            onChange={(value) =>
              onDraftChange((currentValue) => ({
                ...currentValue,
                paymentMethod: value as PaymentMethod,
                installments: value === 'CARTAO_CREDITO_PARCELADO' ? Math.max(currentValue.installments, 2) : 1,
              }))
            }
          />
          {isInstallmentPayment && (
            <Field label="Parcelas">
              <input
                className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 outline-none transition focus:border-emerald-400 focus:bg-white"
                min={2}
                type="number"
                value={draft.installments}
                onChange={(event) =>
                  onDraftChange((currentValue) => ({
                    ...currentValue,
                    installments: Math.max(2, Number(event.target.value) || 2),
                  }))
                }
              />
            </Field>
          )}
        </div>

        <Field label="Observação">
          <textarea
            className="min-h-[110px] w-full rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 outline-none transition focus:border-emerald-400 focus:bg-white"
            placeholder="Notas rápidas para lembrar do contexto."
            value={draft.notes}
            onChange={(event) => onDraftChange((currentValue) => ({ ...currentValue, notes: event.target.value }))}
          />
        </Field>

        <label className="mt-5 flex min-h-[140px] cursor-pointer flex-col items-center justify-center rounded-[24px] border border-dashed border-emerald-200 bg-emerald-50/50 p-5 text-center">
          <span className="text-sm font-semibold uppercase tracking-[0.14em] text-emerald-700">Nota fiscal opcional</span>
          <span className="mt-3 text-sm leading-7 text-slate-600">
            Se existir comprovante agora, você já pode anexar aqui. No parcelado, um upload cobre o grupo inteiro.
          </span>
          <span className="mt-5 rounded-full border border-emerald-200 bg-white px-4 py-2 text-sm font-semibold text-emerald-700">
            {receiptFile ? receiptFile.name : 'Escolher arquivo'}
          </span>
          <input
            accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
            className="hidden"
            type="file"
            onChange={(event) => onReceiptFileChange(event.target.files?.[0] ?? null)}
          />
        </label>

        <div className="mt-5 flex flex-col gap-4 rounded-[24px] bg-slate-50 p-4 md:flex-row md:items-center md:justify-between">
          <div className="text-sm leading-7 text-slate-600">
            <p>
              Sugestão automática:{' '}
              <span className="font-semibold text-emerald-700">{suggestion ? categoryLabels[suggestion] : 'sem sugestão suficiente'}</span>
            </p>
            <p>Se a sugestão errar, o usuário continua livre para trocar manualmente.</p>
          </div>
          <button
            className="rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition hover:bg-emerald-600"
            onClick={onSubmit}
            type="button"
          >
            Salvar lançamento
          </button>
        </div>
          </>
        )}
      </div>
    </div>
    </div>
  );
}

type TradableAssetType = Exclude<InvestmentAssetType, 'RENDA_FIXA'>;

function InvestmentOperationForm({ onCancel, onRecorded }: { onCancel: () => void; onRecorded: (operation: string) => void }) {
  const [operation, setOperation] = useState<'COMPRA' | 'VENDA'>('COMPRA');
  const [assetType, setAssetType] = useState<TradableAssetType>('ACAO');
  const [query, setQuery] = useState('');
  const deferredQuery = useDeferredValue(query);
  const [selected, setSelected] = useState<InvestmentAssetSearchResponse | null>(null);
  const [positionId, setPositionId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [unitPrice, setUnitPrice] = useState(0);
  const [eventDate, setEventDate] = useState(new Date().toISOString().slice(0, 10));
  const [exchangeRate, setExchangeRate] = useState(0);
  const [error, setError] = useState('');
  const portfolio = useInvestmentPortfolioQuery();
  const trade = useRecordInvestmentTradeMutation();
  const search = useInvestmentAssetSearchQuery(deferredQuery, assetType, operation === 'COMPRA' && !selected);
  const sellable = (portfolio.data?.positions ?? []).filter((position) => position.assetType !== 'RENDA_FIXA' && (position.quantity ?? 0) > 0);
  const total = Math.max(0, quantity * unitPrice);

  const reset = () => {
    setSelected(null);
    setPositionId(null);
    setQuery('');
    setUnitPrice(0);
    setExchangeRate(0);
    setError('');
  };
  const chooseAsset = (asset: InvestmentAssetSearchResponse, id: number | null = null, price?: number | null) => {
    setSelected(asset);
    setPositionId(id);
    setUnitPrice(price ?? asset.currentPrice ?? 0);
    setError('');
  };
  const choosePosition = (position: InvestmentPositionResponse) => chooseAsset({
    assetType: position.assetType as TradableAssetType,
    symbol: position.symbol ?? '',
    externalId: position.externalId ?? '',
    name: position.name,
    market: (position.market ?? 'BR') as 'BR' | 'US' | 'GLOBAL',
    exchange: position.exchange ?? '',
    currency: position.currency ?? position.quote.currency,
    currentPrice: position.quote.price,
    source: position.quote.source,
  }, position.id, position.quote.price ?? position.averagePrice);
  const submit = () => {
    if (!selected || quantity <= 0 || unitPrice <= 0) {
      setError('Selecione um ativo e informe quantidade e preço maiores que zero.');
      return;
    }
    if (selected.currency !== 'BRL' && exchangeRate <= 0) {
      setError('Informe o câmbio usado nesta operação para registrar o valor em reais.');
      return;
    }
    const payload: InvestmentTradeRequest = {
      requestId: crypto.randomUUID(),
      positionId,
      movementType: operation,
      assetType: selected.assetType as TradableAssetType,
      symbol: selected.symbol,
      externalId: selected.externalId,
      name: selected.name,
      market: selected.market,
      exchange: selected.exchange,
      currency: selected.currency,
      quantity,
      unitPrice,
      fees: 0,
      eventDate,
      exchangeRate: selected.currency === 'BRL' ? undefined : exchangeRate,
      costs: { brokerageFee: 0, b3Fee: 0, otherCosts: 0, withheldTax: 0 },
    };
    trade.mutate(payload, {
      onSuccess: () => {
        const label = `${operation === 'COMPRA' ? 'Compra' : 'Venda'} de ${selected.symbol}`;
        reset();
        onRecorded(label);
      },
      onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível registrar a operação.')),
    });
  };

  return <div className="mt-6 space-y-5">
    <div className="flex flex-wrap items-start justify-between gap-3">
      <div>
        <p className="text-sm font-semibold text-slate-900">Operação na carteira</p>
        <p className="mt-1 max-w-xl text-sm leading-6 text-slate-500">Escolha o ativo e registre a operação uma única vez. O Farol atualiza a carteira, as transações e o painel financeiro juntos.</p>
      </div>
      <button type="button" className="text-sm font-semibold text-slate-600 hover:text-slate-900" onClick={onCancel}>Voltar para lançamento comum</button>
    </div>
    <div className="grid grid-cols-2 gap-2 rounded-2xl bg-slate-100 p-1.5">
      {(['COMPRA', 'VENDA'] as const).map((type) => <button key={type} type="button" className={`rounded-xl px-4 py-3 text-sm font-semibold ${operation === type ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`} onClick={() => { setOperation(type); reset(); }}>{type === 'COMPRA' ? 'Comprar ativo' : 'Vender ativo'}</button>)}
    </div>
    {!selected && operation === 'COMPRA' && <>
      <div className="flex flex-wrap gap-2">{(['ACAO', 'FII', 'CRIPTO'] as TradableAssetType[]).map((type) => <button key={type} type="button" className={`rounded-full border px-4 py-2 text-sm font-semibold ${assetType === type ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 text-slate-600'}`} onClick={() => { setAssetType(type); setQuery(''); }}>{type === 'ACAO' ? 'Ações' : type === 'FII' ? 'FIIs' : 'Cripto'}</button>)}</div>
      <label className="block text-sm font-medium text-slate-700">Ativo que deseja comprar<div className="relative mt-1"><Search size={18} className="absolute left-4 top-3.5 text-slate-400" /><input autoFocus className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 pl-11 pr-4 outline-none focus:border-emerald-400 focus:bg-white" value={query} onChange={(event) => setQuery(event.target.value)} placeholder={assetType === 'ACAO' ? 'Ex.: BBAS3, PETR4, Itaú...' : 'Digite ticker ou nome'} /></div></label>
      <div className="space-y-2">
        {search.isFetching && <p className="py-3 text-center text-sm text-slate-500">Buscando ativos verificados...</p>}
        {!search.isFetching && deferredQuery.length >= 2 && search.data?.length === 0 && <p className="rounded-2xl border border-dashed border-slate-200 p-4 text-center text-sm text-slate-500">Nenhum ativo verificado foi encontrado.</p>}
        {search.data?.map((asset) => <button key={`${asset.market}-${asset.externalId}`} type="button" className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left hover:border-emerald-200 hover:bg-emerald-50/40" onClick={() => chooseAsset(asset)}><span><b className="text-slate-950">{asset.symbol}</b><span className="ml-2 text-sm text-slate-500">{asset.name}</span></span><span className="text-sm font-semibold text-slate-700">{asset.currentPrice == null ? 'Cotação indisponível' : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: asset.currency || 'BRL' }).format(asset.currentPrice)}</span></button>)}
      </div>
    </>}
    {!selected && operation === 'VENDA' && <div className="space-y-2"><p className="text-sm font-medium text-slate-700">Escolha uma posição da sua carteira</p>{sellable.length === 0 ? <p className="rounded-2xl border border-dashed border-slate-200 p-4 text-sm text-slate-500">Ainda não há ativos disponíveis para venda.</p> : sellable.map((position) => <button key={position.id} type="button" className="flex w-full items-center justify-between rounded-2xl border border-slate-100 bg-slate-50 p-4 text-left hover:border-rose-200" onClick={() => choosePosition(position)}><span><b className="text-slate-950">{position.symbol || position.name}</b><span className="ml-2 text-sm text-slate-500">Disponível: {position.quantity}</span></span><span className="text-sm font-semibold text-slate-700">{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: position.currency ?? 'BRL' }).format(position.quote.price ?? position.averagePrice ?? 0)}</span></button>)}</div>}
    {selected && <><div className="flex items-center justify-between rounded-2xl border border-emerald-100 bg-emerald-50 p-4"><div><p className="text-xs font-semibold uppercase tracking-[.14em] text-emerald-700">Ativo selecionado</p><p className="mt-1 font-semibold text-slate-950">{selected.symbol} · {selected.name}</p></div><button type="button" className="text-sm font-semibold text-slate-600" onClick={reset}>Trocar</button></div><div className="grid gap-4 sm:grid-cols-3"><label className="text-sm font-medium text-slate-700">Quantidade<input className="mt-1 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4" type="number" min="0.00000001" step="0.00000001" value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} /></label><label className="text-sm font-medium text-slate-700">Preço unitário ({selected.currency})<input className="mt-1 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4" type="number" min="0.000001" step="0.000001" value={unitPrice} onChange={(event) => setUnitPrice(Number(event.target.value))} /></label><label className="text-sm font-medium text-slate-700">Data<input className="mt-1 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4" type="date" value={eventDate} onChange={(event) => setEventDate(event.target.value)} /></label></div>{selected.currency !== 'BRL' && <label className="block text-sm font-medium text-slate-700">Câmbio da operação (R$ por {selected.currency})<input className="mt-1 h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4" type="number" min="0.000001" step="0.000001" value={exchangeRate} onChange={(event) => setExchangeRate(Number(event.target.value))} /></label>}<div className="flex items-center justify-between rounded-2xl bg-slate-100 px-4 py-3"><span className="text-sm text-slate-600">Total da {operation === 'COMPRA' ? 'compra' : 'venda'}</span><strong className="text-slate-950">{new Intl.NumberFormat('pt-BR', { style: 'currency', currency: selected.currency || 'BRL' }).format(total)}</strong></div></>}
    {error && <p className="rounded-2xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
    <button type="button" onClick={submit} disabled={!selected || trade.isPending} className="w-full rounded-full bg-emerald-500 px-5 py-3 text-sm font-semibold text-white disabled:bg-slate-300">{trade.isPending ? 'Registrando...' : operation === 'COMPRA' ? 'Comprar e atualizar carteira' : 'Vender e atualizar carteira'}</button>
  </div>;
}

function ModeButton({ active, label, onClick }: { active: boolean; label: string; onClick: () => void }) {
  return (
    <button
      className={`rounded-2xl px-4 py-3 text-sm font-semibold transition ${
        active ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
      }`}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}
