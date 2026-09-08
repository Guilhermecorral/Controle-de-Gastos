import { ChangeEvent, useState } from 'react'
import { AlertTriangle, FileUp, LoaderCircle, X } from 'lucide-react'
import type { InvestmentImportConfirmItem, InvestmentImportPreviewItem } from '../../../types'
import { getApiErrorMessage } from '../../../lib/httpErrors'
import { useConfirmInvestmentImportMutation, usePreviewInvestmentImportMutation } from '../../../lib/queries'

type ReviewRow = InvestmentImportPreviewItem & { selected: boolean }

type InvestmentImportDialogProps = {
  open: boolean
  onClose: () => void
  onFinished: (message: string) => void
}

const MAX_FILE_BYTES = 5 * 1024 * 1024

export default function InvestmentImportDialog({ open, onClose, onFinished }: InvestmentImportDialogProps) {
  const previewMutation = usePreviewInvestmentImportMutation()
  const confirmMutation = useConfirmInvestmentImportMutation()
  const [batchId, setBatchId] = useState<number | null>(null)
  const [filename, setFilename] = useState('')
  const [rows, setRows] = useState<ReviewRow[]>([])
  const [warnings, setWarnings] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  const reset = () => {
    setBatchId(null)
    setFilename('')
    setRows([])
    setWarnings([])
    setError(null)
  }

  const close = () => {
    if (previewMutation.isPending || confirmMutation.isPending) return
    reset()
    onClose()
  }

  const loadFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return
    if (file.size > MAX_FILE_BYTES) {
      setError('O arquivo pode ter no máximo 5 MB.')
      return
    }
    setError(null)
    previewMutation.mutate(file, {
      onSuccess: (preview) => {
        setBatchId(preview.batchId)
        setFilename(preview.filename)
        setWarnings(preview.warnings)
        setRows(preview.items.map((item) => ({ ...item, selected: item.selectedByDefault })))
      },
      onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível analisar o arquivo.')),
    })
  }

  const update = (id: number, patch: Partial<ReviewRow>) => {
    setRows((current) => current.map((row) => row.id === id ? { ...row, ...patch } : row))
  }

  const confirm = () => {
    if (batchId == null) return
    const selected = rows.filter((row) => row.selected)
    if (!selected.length) {
      setError('Selecione ao menos uma operação para salvar.')
      return
    }
    setError(null)
    confirmMutation.mutate({
      batchId,
      items: rows.map(toConfirmItem),
    }, {
      onSuccess: (response) => {
        onFinished(response.message)
        reset()
        onClose()
      },
      onError: (reason) => setError(getApiErrorMessage(reason, 'Não foi possível salvar as operações revisadas.')),
    })
  }

  return (
    <div className="fixed inset-0 z-[70] flex items-end justify-center bg-slate-950/55 p-0 backdrop-blur-sm sm:items-center sm:p-6" role="dialog" aria-modal="true" aria-label="Importar investimentos">
      <section className="max-h-[96vh] w-full max-w-6xl overflow-hidden rounded-t-[30px] bg-white shadow-2xl sm:rounded-[30px]">
        <header className="flex items-start justify-between border-b border-slate-100 px-6 py-5 sm:px-8">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[.18em] text-emerald-600">Importação assistida</p>
            <h3 className="mt-1 text-xl font-semibold text-slate-950">Revise seus investimentos antes de salvar</h3>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">O arquivo entra primeiro em uma área segura de revisão. Nada altera a carteira ou o financeiro até a confirmação.</p>
          </div>
          <button className="rounded-full bg-slate-100 p-2 text-slate-500 transition hover:bg-slate-200" type="button" onClick={close} aria-label="Fechar importação"><X size={20} /></button>
        </header>

        <div className="max-h-[calc(96vh-112px)] overflow-y-auto p-6 sm:p-8">
          {!batchId && <label className="flex cursor-pointer flex-col items-center justify-center rounded-[24px] border-2 border-dashed border-emerald-200 bg-emerald-50/50 px-6 py-10 text-center transition hover:border-emerald-400 hover:bg-emerald-50">
            {previewMutation.isPending ? <LoaderCircle className="animate-spin text-emerald-600" size={28} /> : <FileUp className="text-emerald-600" size={28} />}
            <span className="mt-4 font-semibold text-slate-900">{previewMutation.isPending ? 'Lendo e organizando o arquivo...' : 'Selecionar CSV, Excel, OFX ou PDF SINACOR'}</span>
            <span className="mt-2 text-sm text-slate-500">Colunas sugeridas: Data, Ticker, Operação, Quantidade, Preço, Corretagem, Taxa B3 e IRRF.</span>
            <span className="mt-1 text-xs text-slate-400">PDF precisa ser a nota original com texto selecionável. OFX bancário comum continua no importador de extrato. Máximo de 5 MB.</span>
            <input className="sr-only" type="file" accept=".csv,.tsv,.xls,.xlsx,.ofx,.pdf" onChange={loadFile} disabled={previewMutation.isPending} />
          </label>}

          {batchId && <>
            <div className="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-slate-50 p-4">
              <div><p className="font-semibold text-slate-900">{filename}</p><p className="mt-1 text-sm text-slate-500">{rows.length} operação(ões) encontradas. Ajuste somente o que for necessário.</p></div>
              <button className="rounded-full border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-emerald-300 hover:text-emerald-700" type="button" onClick={reset}>Escolher outro arquivo</button>
            </div>
            {warnings.length > 0 && <Notice title="Pontos para revisar" messages={warnings} />}
            {rows.some((row) => row.possibleDuplicate) && <Notice title="Possíveis duplicidades" messages={["As linhas destacadas parecem coincidir com uma operação já existente. Elas não são bloqueadas: confirme somente se o lançamento ainda não estiver na carteira."]} />}
            {rows.length > 0 && <div className="overflow-x-auto rounded-[22px] border border-slate-200"><table className="min-w-[1120px] w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500"><tr><th className="p-3"><input aria-label="Selecionar todas" type="checkbox" checked={rows.every((row) => row.selected)} onChange={(event) => setRows((current) => current.map((row) => ({ ...row, selected: event.target.checked })))} /></th><th className="p-3">Operação</th><th className="p-3">Ativo</th><th className="p-3">Data</th><th className="p-3">Quantidade</th><th className="p-3">Preço</th><th className="p-3">Custos</th><th className="p-3">IRRF</th><th className="p-3">Aviso</th></tr></thead><tbody>{rows.map((row) => <tr key={row.id} className={row.possibleDuplicate ? 'border-t border-amber-100 bg-amber-50/40' : 'border-t border-slate-100'}><td className="p-3 align-top"><input aria-label={`Selecionar ${row.symbol}`} type="checkbox" checked={row.selected} onChange={(event) => update(row.id, { selected: event.target.checked })} /></td><td className="p-3 align-top"><select className="rounded-lg border border-slate-200 bg-white px-2 py-1.5" value={row.movementType} onChange={(event) => update(row.id, { movementType: event.target.value as ReviewRow['movementType'] })}><option value="COMPRA">Compra</option><option value="VENDA">Venda</option></select></td><td className="p-3 align-top"><input className="w-24 rounded-lg border border-slate-200 px-2 py-1.5 font-semibold uppercase" value={row.symbol} onChange={(event) => update(row.id, { symbol: event.target.value.toUpperCase(), name: event.target.value.toUpperCase() || row.name })} /><select className="mt-2 w-24 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-xs" value={row.assetType} onChange={(event) => update(row.id, { assetType: event.target.value as ReviewRow['assetType'] })}><option value="ACAO">Ação</option><option value="FII">FII</option><option value="FIAGRO">Fiagro</option><option value="CRIPTO">Cripto</option></select></td><td className="p-3 align-top"><input className="rounded-lg border border-slate-200 px-2 py-1.5" type="date" value={row.eventDate} onChange={(event) => update(row.id, { eventDate: event.target.value })} /></td><td className="p-3 align-top"><NumberInput value={row.quantity} onChange={(quantity) => update(row.id, { quantity })} /></td><td className="p-3 align-top"><NumberInput value={row.unitPrice} onChange={(unitPrice) => update(row.id, { unitPrice })} /></td><td className="p-3 align-top"><NumberInput value={row.costs.brokerageFee + row.costs.b3Fee + row.costs.otherCosts} onChange={(otherCosts) => update(row.id, { costs: { ...row.costs, brokerageFee: 0, b3Fee: 0, otherCosts } })} /></td><td className="p-3 align-top"><NumberInput value={row.costs.withheldTax} onChange={(withheldTax) => update(row.id, { costs: { ...row.costs, withheldTax } })} /></td><td className="p-3 align-top text-xs leading-5 text-slate-500">{row.warning || 'Pronto para revisar'}</td></tr>)}</tbody></table></div>}
            {rows.length === 0 && <p className="rounded-2xl bg-slate-50 p-5 text-sm leading-6 text-slate-600">Nenhuma operação foi identificada automaticamente. Confira se o arquivo possui colunas de ticker, data, tipo, quantidade e preço.</p>}
          </>}

          {error && <p className="mt-5 rounded-2xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{error}</p>}
          {batchId && <footer className="mt-6 flex flex-wrap items-center justify-between gap-4"><p className="text-sm text-slate-500">{rows.filter((row) => row.selected).length} linha(s) serão enviadas para a carteira.</p><button className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60" type="button" disabled={confirmMutation.isPending || previewMutation.isPending || !rows.length} onClick={confirm}>{confirmMutation.isPending ? 'Salvando operações...' : 'Salvar importação revisada'}</button></footer>}
        </div>
      </section>
    </div>
  )
}

function NumberInput({ value, onChange }: { value: number; onChange: (value: number) => void }) {
  return <input className="w-24 rounded-lg border border-slate-200 px-2 py-1.5" inputMode="decimal" value={Number.isFinite(value) ? value : 0} onChange={(event) => onChange(Number(event.target.value.replace(',', '.')) || 0)} />
}

function Notice({ title, messages }: { title: string; messages: string[] }) {
  return <div className="mb-4 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900"><AlertTriangle className="mt-0.5 shrink-0" size={18} /><div><p className="font-semibold">{title}</p>{messages.map((message) => <p key={message} className="mt-1 leading-6">{message}</p>)}</div></div>
}

function toConfirmItem(row: ReviewRow): InvestmentImportConfirmItem {
  const { sourceRow: _sourceRow, selectedByDefault: _selectedByDefault, possibleDuplicate: _possibleDuplicate, warning: _warning, ...item } = row
  return item
}
