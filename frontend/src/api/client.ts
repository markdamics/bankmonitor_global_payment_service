import type { Account, Currency } from './types'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export interface CreateAccountRequest {
  owner: string
  currency: Currency
  initialBalance: number
}

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json()
    if (typeof body?.detail === 'string') {
      return body.detail
    }
  } catch {
    // response had no JSON body; fall through to the generic message
  }
  return `A kérés sikertelen volt (HTTP ${response.status})`
}

export async function fetchAccounts(): Promise<Account[]> {
  const response = await fetch(`${API_BASE_URL}/api/accounts`)
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
  return response.json()
}

export async function createAccount(request: CreateAccountRequest): Promise<Account> {
  const response = await fetch(`${API_BASE_URL}/api/accounts`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response))
  }
  return response.json()
}
