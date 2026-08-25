import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { createAccount, fetchAccounts } from '../api/client'
import type { Account, Currency } from '../api/types'
import './AccountsPage.css'

const CURRENCIES: Currency[] = ['EUR', 'USD', 'HUF']

function formatBalance(balance: number, currency: Currency): string {
  return new Intl.NumberFormat('hu-HU', { style: 'currency', currency }).format(balance)
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString('hu-HU')
}

export default function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [owner, setOwner] = useState('')
  const [currency, setCurrency] = useState<Currency>('EUR')
  const [initialBalance, setInitialBalance] = useState('0')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)

  async function loadAccounts() {
    setIsLoading(true)
    setLoadError(null)
    try {
      setAccounts(await fetchAccounts())
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : 'Ismeretlen hiba történt')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    loadAccounts()
  }, [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitError(null)

    const trimmedOwner = owner.trim()
    const parsedBalance = Number(initialBalance)

    if (!trimmedOwner) {
      setSubmitError('A tulajdonos neve kötelező')
      return
    }
    if (!Number.isFinite(parsedBalance) || parsedBalance < 0) {
      setSubmitError('A kezdő egyenlegnek nem-negatív számnak kell lennie')
      return
    }

    setIsSubmitting(true)
    try {
      await createAccount({ owner: trimmedOwner, currency, initialBalance: parsedBalance })
      setOwner('')
      setInitialBalance('0')
      await loadAccounts()
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : 'Ismeretlen hiba történt')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="accounts-page">
      <h1>Számlák</h1>

      <form className="accounts-form" onSubmit={handleSubmit}>
        <h2>Új számla</h2>

        <div className="form-row">
          <label htmlFor="owner">Tulajdonos</label>
          <input
            id="owner"
            value={owner}
            onChange={(event) => setOwner(event.target.value)}
            placeholder="pl. Kovács János"
            disabled={isSubmitting}
          />
        </div>

        <div className="form-row">
          <label htmlFor="currency">Devizanem</label>
          <select
            id="currency"
            value={currency}
            onChange={(event) => setCurrency(event.target.value as Currency)}
            disabled={isSubmitting}
          >
            {CURRENCIES.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="initialBalance">Kezdő egyenleg</label>
          <input
            id="initialBalance"
            type="number"
            min="0"
            step="0.01"
            value={initialBalance}
            onChange={(event) => setInitialBalance(event.target.value)}
            disabled={isSubmitting}
          />
        </div>

        {submitError && (
          <p className="form-error" role="alert">
            {submitError}
          </p>
        )}

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Létrehozás...' : 'Számla létrehozása'}
        </button>
      </form>

      <div className="accounts-list">
        <h2>Meglévő számlák</h2>

        {isLoading && <p>Betöltés...</p>}
        {loadError && (
          <p className="form-error" role="alert">
            {loadError}
          </p>
        )}
        {!isLoading && !loadError && accounts.length === 0 && <p>Még nincs számla.</p>}

        {!isLoading && !loadError && accounts.length > 0 && (
          <table>
            <thead>
              <tr>
                <th>Tulajdonos</th>
                <th>Devizanem</th>
                <th>Egyenleg</th>
                <th>Létrehozva</th>
              </tr>
            </thead>
            <tbody>
              {accounts.map((account) => (
                <tr key={account.id}>
                  <td>{account.owner}</td>
                  <td>{account.currency}</td>
                  <td>{formatBalance(account.balance, account.currency)}</td>
                  <td>{formatDate(account.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  )
}
