import type { Currency } from '../api/types'

export function formatCurrency(amount: number, currency: Currency): string {
  return new Intl.NumberFormat('hu-HU', { style: 'currency', currency }).format(amount)
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('hu-HU')
}
