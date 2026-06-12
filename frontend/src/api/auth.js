/**
 * 인증/세션 API.
 * `import * as authApi from '@/api/auth'` 형태로 사용한다.
 */
import { request, ApiError } from './http'

/** 현재 로그인한 사용자 조회. 미로그인이면 null */
export async function fetchCurrentUser() {
  try {
    const response = await request('/api/me')
    return response.json()
  } catch (error) {
    if (error instanceof ApiError) return null // 401 등 — 로그인 안 된 상태
    throw error // 네트워크 오류는 호출부에서 처리
  }
}

/** 로그아웃 */
export async function logout() {
  await request('/api/logout', { method: 'POST' })
}
