/**
 * fetch 공통 래퍼.
 * - 세션 쿠키(credentials)를 항상 포함한다.
 * - 실패 응답(4xx/5xx)은 상태 코드를 담은 ApiError로 던져서
 *   호출부가 상태 코드로 분기할 수 있게 한다.
 */
export class ApiError extends Error {
  constructor(status) {
    super(`요청 실패 (${status})`)
    this.status = status
  }
}

export async function request(url, options = {}) {
  const response = await fetch(url, { credentials: 'include', ...options })
  if (!response.ok) throw new ApiError(response.status)
  return response
}

export function postJson(url, body) {
  return request(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}
