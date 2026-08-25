import { useEffect, useMemo, useState } from 'react'
import { fetchAccounts, fetchTransfers } from '../api/client'
import type { Account, Transfer } from '../api/types'
import { formatCurrency, formatDateTime } from '../utils/format'
import './TransactionsPage.css'

export default function TransactionsPage() {
  const [transfers, setTransfers] = useState<Transfer[]>([])
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      setIsLoading(true)
      setLoadError(null)
      try {
        const [transfersData, accountsData] = await Promise.all([fetchTransfers(), fetchAccounts()])
        setTransfers(transfersData)
        setAccounts(accountsData)
      } catch (err) {
        setLoadError(err instanceof Error ? err.message : 'Ismeretlen hiba történt')
      } finally {
        setIsLoading(false)
      }
    }
    load()
  }, [])

  const ownerById = useMemo(() => {
    const map = new Map<string, string>()
    for (const account of accounts) {
      map.set(account.id, account.owner)
    }
    return map
  }, [accounts])

  function ownerLabel(accountId: string): string {
    return ownerById.get(accountId) ?? accountId
  }

  return (
    <section className="transactions-page">
      <h1>Tranzakciók</h1>

      {isLoading && <p>Betöltés...</p>}
      {loadError && (
        <p className="form-error" role="alert">
          {loadError}
        </p>
      )}
      {!isLoading && !loadError && transfers.length === 0 && <p>Még nem történt utalás.</p>}

      {!isLoading && !loadError && transfers.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Forrás</th>
              <th>Cél</th>
              <th>Összeg</th>
              <th>Árfolyam</th>
              <th>Státusz</th>
              <th>Létrehozva</th>
            </tr>
          </thead>
          <tbody>
            {transfers.map((transfer) => (
              <tr key={transfer.id}>
                <td>{ownerLabel(transfer.sourceAccountId)}</td>
                <td>{ownerLabel(transfer.targetAccountId)}</td>
                <td>
                  {formatCurrency(transfer.sourceAmount, transfer.sourceCurrency)}
                  {transfer.sourceCurrency !== transfer.targetCurrency && (
                    <> &rarr; {formatCurrency(transfer.targetAmount, transfer.targetCurrency)}</>
                  )}
                </td>
                <td>{transfer.exchangeRate !== null ? transfer.exchangeRate.toFixed(6) : '—'}</td>
                <td>
                  <span className={`status-badge status-${transfer.status.toLowerCase()}`}>
                    {transfer.status === 'COMPLETED' ? 'Sikeres' : 'Sikertelen'}
                  </span>
                </td>
                <td>{formatDateTime(transfer.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
