import { render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TransactionsPage from '../TransactionsPage'
import { fetchAccounts, fetchTransfers } from '../../api/client'
import type { Account, Transfer } from '../../api/types'

vi.mock('../../api/client', () => ({
  fetchAccounts: vi.fn(),
  fetchTransfers: vi.fn(),
}))

const mockedFetchAccounts = vi.mocked(fetchAccounts)
const mockedFetchTransfers = vi.mocked(fetchTransfers)

const alice: Account = { id: 'acc-alice', owner: 'Alice', currency: 'EUR', balance: 100, createdAt: '2026-01-01T00:00:00Z' }
const bob: Account = { id: 'acc-bob', owner: 'Bob', currency: 'EUR', balance: 20, createdAt: '2026-01-01T00:00:00Z' }
const carol: Account = { id: 'acc-carol', owner: 'Carol', currency: 'USD', balance: 5, createdAt: '2026-01-01T00:00:00Z' }

function transfer(overrides: Partial<Transfer> = {}): Transfer {
  return {
    id: 'transfer-1',
    sourceAccountId: alice.id,
    targetAccountId: bob.id,
    sourceCurrency: 'EUR',
    targetCurrency: 'EUR',
    sourceAmount: 10,
    targetAmount: 10,
    exchangeRate: null,
    status: 'COMPLETED',
    idempotencyKey: 'key-1',
    createdAt: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('TransactionsPage', () => {
  beforeEach(() => {
    mockedFetchAccounts.mockReset()
    mockedFetchTransfers.mockReset()
    mockedFetchAccounts.mockResolvedValue([alice, bob, carol])
  })

  it('shows an empty-state message when there are no transfers', async () => {
    mockedFetchTransfers.mockResolvedValue([])

    render(<TransactionsPage />)

    expect(await screen.findByText('Még nem történt utalás.')).toBeInTheDocument()
  })

  it('resolves account ids to owner names', async () => {
    mockedFetchTransfers.mockResolvedValue([transfer()])

    render(<TransactionsPage />)

    const row = (await screen.findByText('Alice')).closest('tr')!
    expect(within(row).getByText('Bob')).toBeInTheDocument()
  })

  it('shows the conversion arrow and rate for a cross-currency transfer', async () => {
    mockedFetchTransfers.mockResolvedValue([transfer({
      targetAccountId: carol.id,
      targetCurrency: 'USD',
      targetAmount: 10.8,
      exchangeRate: 1.08,
    })])

    render(<TransactionsPage />)

    const row = (await screen.findByText('Alice')).closest('tr')!
    expect(within(row).getByText(/→/)).toBeInTheDocument()
    expect(within(row).getByText('1.080000')).toBeInTheDocument()
  })

  it('shows a dash instead of a rate for a same-currency transfer', async () => {
    mockedFetchTransfers.mockResolvedValue([transfer()])

    render(<TransactionsPage />)

    const row = (await screen.findByText('Alice')).closest('tr')!
    expect(within(row).getByText('—')).toBeInTheDocument()
  })

  it('labels COMPLETED and FAILED statuses in Hungarian', async () => {
    mockedFetchTransfers.mockResolvedValue([
      transfer({ id: 't-1', status: 'COMPLETED' }),
      transfer({ id: 't-2', status: 'FAILED' }),
    ])

    render(<TransactionsPage />)

    expect(await screen.findByText('Sikeres')).toBeInTheDocument()
    expect(screen.getByText('Sikertelen')).toBeInTheDocument()
  })

  it('shows the error message when loading fails', async () => {
    mockedFetchTransfers.mockRejectedValue(new Error('Backend unavailable'))

    render(<TransactionsPage />)

    expect(await screen.findByText('Backend unavailable')).toBeInTheDocument()
  })
})
