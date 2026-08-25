import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TransferPage from '../TransferPage'
import { createTransfer, fetchAccounts } from '../../api/client'
import type { Account, Transfer } from '../../api/types'

vi.mock('../../api/client', () => ({
  fetchAccounts: vi.fn(),
  createTransfer: vi.fn(),
}))

const mockedFetchAccounts = vi.mocked(fetchAccounts)
const mockedCreateTransfer = vi.mocked(createTransfer)

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

async function selectAccounts(sourceLabelPattern: RegExp, targetLabelPattern: RegExp) {
  const user = userEvent.setup()
  const sourceSelect = screen.getByLabelText('Forrás számla')
  await user.selectOptions(sourceSelect, within(sourceSelect).getByRole('option', { name: sourceLabelPattern }))
  const targetSelect = screen.getByLabelText('Cél számla')
  await user.selectOptions(targetSelect, within(targetSelect).getByRole('option', { name: targetLabelPattern }))
  return user
}

describe('TransferPage', () => {
  beforeEach(() => {
    mockedFetchAccounts.mockReset()
    mockedCreateTransfer.mockReset()
    mockedFetchAccounts.mockResolvedValue([alice, bob, carol])
  })

  it('populates both account selects once accounts are loaded', async () => {
    render(<TransferPage />)

    expect(await within(screen.getByLabelText('Forrás számla')).findByText(/Alice/)).toBeInTheDocument()
    expect(within(screen.getByLabelText('Cél számla')).getByText(/Bob/)).toBeInTheDocument()
  })

  it('excludes the selected source account from the target options', async () => {
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    const user = userEvent.setup()
    const sourceSelect = screen.getByLabelText('Forrás számla')

    await user.selectOptions(sourceSelect, within(sourceSelect).getByRole('option', { name: /Alice/ }))

    expect(within(screen.getByLabelText('Cél számla')).queryByText(/Alice/)).not.toBeInTheDocument()
  })

  it('rejects submission when no accounts are selected', async () => {
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Utalás indítása' }))

    expect(await screen.findByText('Válassz forrás- és célszámlát')).toBeInTheDocument()
    expect(mockedCreateTransfer).not.toHaveBeenCalled()
  })

  it('shows a success message for a same-currency transfer', async () => {
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    mockedCreateTransfer.mockResolvedValue(transfer())
    const user = await selectAccounts(/Alice/, /Bob/)

    await user.type(screen.getByLabelText('Összeg'), '10')
    await user.click(screen.getByRole('button', { name: 'Utalás indítása' }))

    expect(await screen.findByText(/Sikeres utalás/)).toBeInTheDocument()
    expect(mockedCreateTransfer).toHaveBeenCalledWith(
        expect.objectContaining({ sourceAccountId: alice.id, targetAccountId: bob.id, amount: 10 }),
    )
  })

  it('shows the conversion arrow and rate for a cross-currency transfer', async () => {
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    mockedCreateTransfer.mockResolvedValue(transfer({
      targetAccountId: carol.id,
      targetCurrency: 'USD',
      targetAmount: 10.8,
      exchangeRate: 1.08,
    }))
    const user = await selectAccounts(/Alice/, /Carol/)

    await user.type(screen.getByLabelText('Összeg'), '10')
    await user.click(screen.getByRole('button', { name: 'Utalás indítása' }))

    const message = await screen.findByText(/Sikeres utalás/)
    expect(message.textContent).toContain('1.080000')
  })

  it('shows a failure message when the transfer fails due to insufficient funds', async () => {
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    mockedCreateTransfer.mockResolvedValue(transfer({ status: 'FAILED' }))
    const user = await selectAccounts(/Alice/, /Bob/)

    await user.type(screen.getByLabelText('Összeg'), '999')
    await user.click(screen.getByRole('button', { name: 'Utalás indítása' }))

    expect(await screen.findByText('Sikertelen utalás: fedezethiány a forrásszámlán.')).toBeInTheDocument()
  })

  it('shows the error message when the request itself fails', async () => {
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    mockedCreateTransfer.mockRejectedValue(new Error('Service unavailable'))
    const user = await selectAccounts(/Alice/, /Bob/)

    await user.type(screen.getByLabelText('Összeg'), '10')
    await user.click(screen.getByRole('button', { name: 'Utalás indítása' }))

    expect(await screen.findByText('Service unavailable')).toBeInTheDocument()
  })

  it('refreshes account balances after a successful transfer', async () => {
    mockedFetchAccounts.mockResolvedValueOnce([alice, bob, carol])
        .mockResolvedValueOnce([{ ...alice, balance: 90 }, { ...bob, balance: 30 }, carol])
    render(<TransferPage />)
    await screen.findAllByText(/Alice/)
    mockedCreateTransfer.mockResolvedValue(transfer())
    const user = await selectAccounts(/Alice/, /Bob/)

    await user.type(screen.getByLabelText('Összeg'), '10')
    await user.click(screen.getByRole('button', { name: 'Utalás indítása' }))

    await waitFor(() => expect(mockedFetchAccounts).toHaveBeenCalledTimes(2))
  })
})
