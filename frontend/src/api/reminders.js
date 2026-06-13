import { request } from './http'

export async function fetchToday() {
  const response = await request('/api/reminders/today')
  return response.json()
}
