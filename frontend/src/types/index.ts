export type Currency = 'EUR' | 'USD' | 'HUF'

export interface Account {
  id: string
  owner: string
  currency: Currency
  balance: number
}

export interface Transfer {
  id: string
  sourceAccountId: string
  targetAccountId: string
  amount: number
  currency: Currency
  status: 'COMPLETED' | 'FAILED'
  createdAt: string
}
