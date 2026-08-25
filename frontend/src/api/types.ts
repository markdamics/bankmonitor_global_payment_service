export type Currency = 'EUR' | 'USD' | 'HUF'

export interface Account {
  id: string
  owner: string
  currency: Currency
  balance: number
  createdAt: string
}
