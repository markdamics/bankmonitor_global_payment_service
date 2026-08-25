export type Currency = 'EUR' | 'USD' | 'HUF'

export interface Account {
  id: string
  owner: string
  currency: Currency
  balance: number
  createdAt: string
}

export type TransferStatus = 'COMPLETED' | 'FAILED'

export interface Transfer {
  id: string
  sourceAccountId: string
  targetAccountId: string
  sourceCurrency: Currency
  targetCurrency: Currency
  sourceAmount: number
  targetAmount: number
  exchangeRate: number | null
  status: TransferStatus
  idempotencyKey: string
  createdAt: string
}
