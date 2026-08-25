import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { createTransfer, fetchAccounts } from '../api/client'
import type { Account, Transfer } from '../api/types'
import { formatCurrency } from '../utils/format'
import './TransferPage.css'

function accountLabel(account: Account): string {
  return `${account.owner} (${account.currency}) — ${formatCurrency(account.balance, account.currency)}`
}

export default function TransferPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoadingAccounts, setIsLoadingAccounts] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [sourceAccountId, setSourceAccountId] = useState('')
  const [targetAccountId, setTargetAccountId] = useState('')
  const [amount, setAmount] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [result, setResult] = useState<Transfer | null>(null)

  async function loadAccounts() {
    setIsLoadingAccounts(true)
    setLoadError(null)
    try {
      setAccounts(await fetchAccounts())
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : 'Ismeretlen hiba történt')
    } finally {
      setIsLoadingAccounts(false)
    }
  }

  useEffect(() => {
    loadAccounts()
  }, [])

  const targetOptions = useMemo(
    () => accounts.filter((account) => account.id !== sourceAccountId),
    [accounts, sourceAccountId],
  )

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)
    setResult(null)

    const parsedAmount = Number(amount)

    if (!sourceAccountId || !targetAccountId) {
      setSubmitError('Válassz forrás- és célszámlát')
      return
    }
    if (sourceAccountId === targetAccountId) {
      setSubmitError('A forrás- és célszámla nem lehet ugyanaz')
      return
    }
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setSubmitError('Az összegnek pozitív számnak kell lennie')
      return
    }

    setIsSubmitting(true)
    try {
      const transfer = await createTransfer({
        sourceAccountId,
        targetAccountId,
        amount: parsedAmount,
        idempotencyKey: crypto.randomUUID(),
      })
      setResult(transfer)
      setAmount('')
      await loadAccounts()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : 'Ismeretlen hiba történt')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="transfer-page">
      <h1>Utalás</h1>

      {loadError && (
        <p className="form-error" role="alert">
          {loadError}
        </p>
      )}

      <form className="form-card" onSubmit={handleSubmit}>
        <div className="form-row">
          <label htmlFor="sourceAccount">Forrás számla</label>
          <select
            id="sourceAccount"
            value={sourceAccountId}
            onChange={(event) => setSourceAccountId(event.target.value)}
            disabled={isSubmitting || isLoadingAccounts}
          >
            <option value="">Válassz számlát...</option>
            {accounts.map((account) => (
              <option key={account.id} value={account.id}>
                {accountLabel(account)}
              </option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="targetAccount">Cél számla</label>
          <select
            id="targetAccount"
            value={targetAccountId}
            onChange={(event) => setTargetAccountId(event.target.value)}
            disabled={isSubmitting || isLoadingAccounts}
          >
            <option value="">Válassz számlát...</option>
            {targetOptions.map((account) => (
              <option key={account.id} value={account.id}>
                {accountLabel(account)}
              </option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="amount">Összeg</label>
          <input
            id="amount"
            type="number"
            min="0.01"
            step="0.01"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
            disabled={isSubmitting}
          />
        </div>

        {submitError && (
          <p className="form-error" role="alert">
            {submitError}
          </p>
        )}

        <button type="submit" disabled={isSubmitting || isLoadingAccounts}>
          {isSubmitting ? 'Utalás folyamatban...' : 'Utalás indítása'}
        </button>
      </form>

      {result && (
        <div className={`transfer-result transfer-result-${result.status.toLowerCase()}`}>
          {result.status === 'COMPLETED' ? (
            <p>
              Sikeres utalás: {formatCurrency(result.sourceAmount, result.sourceCurrency)}
              {result.sourceCurrency !== result.targetCurrency && (
                <> &rarr; {formatCurrency(result.targetAmount, result.targetCurrency)}</>
              )}
              {result.exchangeRate !== null && ` (árfolyam: ${result.exchangeRate.toFixed(6)})`}
            </p>
          ) : (
            <p>Sikertelen utalás: fedezethiány a forrásszámlán.</p>
          )}
        </div>
      )}
    </section>
  )
}
