import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AccountsPage from '../AccountsPage'
import { createAccount, fetchAccounts } from '../../api/client'
import type { Account } from '../../api/types'

vi.mock('../../api/client', () => ({
  fetchAccounts: vi.fn(),
  createAccount: vi.fn(),
}))

const mockedFetchAccounts = vi.mocked(fetchAccounts)
const mockedCreateAccount = vi.mocked(createAccount)

function account(overrides: Partial<Account> = {}): Account {
  return {
    id: 'acc-1',
    owner: 'Alice',
    currency: 'EUR',
    balance: 100,
    createdAt: '2026-01-01T10:00:00Z',
    ...overrides,
  }
}

describe('AccountsPage', () => {
  beforeEach(() => {
    mockedFetchAccounts.mockReset()
    mockedCreateAccount.mockReset()
  })

  it('renders the fetched accounts in the table', async () => {
    mockedFetchAccounts.mockResolvedValue([account({ owner: 'Alice' }), account({ id: 'acc-2', owner: 'Bob' })]);

    render(<AccountsPage />)

    expect(await screen.findByText('Alice')).toBeInTheDocument()
    expect(screen.getByText('Bob')).toBeInTheDocument()
  })

  it('shows an empty-state message when there are no accounts', async () => {
    mockedFetchAccounts.mockResolvedValue([])

    render(<AccountsPage />)

    expect(await screen.findByText('Még nincs számla.')).toBeInTheDocument()
  })

  it('shows the error message when loading accounts fails', async () => {
    mockedFetchAccounts.mockRejectedValue(new Error('Backend unavailable'))

    render(<AccountsPage />)

    expect(await screen.findByText('Backend unavailable')).toBeInTheDocument()
  })

  it('rejects submission when the owner name is blank', async () => {
    mockedFetchAccounts.mockResolvedValue([])
    const user = userEvent.setup()

    render(<AccountsPage />)
    await screen.findByText('Még nincs számla.')

    await user.click(screen.getByRole('button', { name: 'Számla létrehozása' }))

    expect(await screen.findByText('A tulajdonos neve kötelező')).toBeInTheDocument()
    expect(mockedCreateAccount).not.toHaveBeenCalled()
  })

  it('submits the form and refreshes the list on success', async () => {
    mockedFetchAccounts
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([account({ owner: 'Charlie', currency: 'USD', balance: 50 })])
    mockedCreateAccount.mockResolvedValue(account({ owner: 'Charlie', currency: 'USD', balance: 50 }))
    const user = userEvent.setup()

    render(<AccountsPage />)
    await screen.findByText('Még nincs számla.')

    await user.type(screen.getByLabelText('Tulajdonos'), 'Charlie')
    await user.selectOptions(screen.getByLabelText('Devizanem'), 'USD')
    await user.clear(screen.getByLabelText('Kezdő egyenleg'))
    await user.type(screen.getByLabelText('Kezdő egyenleg'), '50')
    await user.click(screen.getByRole('button', { name: 'Számla létrehozása' }))

    await waitFor(() => {
      expect(mockedCreateAccount).toHaveBeenCalledWith({ owner: 'Charlie', currency: 'USD', initialBalance: 50 })
    })
    expect(await screen.findByText('Charlie')).toBeInTheDocument()
    expect(screen.getByLabelText('Tulajdonos')).toHaveValue('')
  })

  it('shows the submit error message when account creation fails', async () => {
    mockedFetchAccounts.mockResolvedValue([])
    mockedCreateAccount.mockRejectedValue(new Error('Duplicate account'))
    const user = userEvent.setup()

    render(<AccountsPage />)
    await screen.findByText('Még nincs számla.')

    await user.type(screen.getByLabelText('Tulajdonos'), 'Dave')
    await user.click(screen.getByRole('button', { name: 'Számla létrehozása' }))

    expect(await screen.findByText('Duplicate account')).toBeInTheDocument()
  })
})
