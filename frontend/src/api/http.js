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

let onUnauthorized = null
let csrfToken = null
let csrfHeaderName = 'X-XSRF-TOKEN'
let csrfTokenPromise = null
const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

export function setOnUnauthorized(callback) {
  onUnauthorized = callback
}

// 로그인/로그아웃처럼 Spring Security가 CSRF 토큰을 새로 만드는 시점에 캐시를 비운다.
export function clearCsrfToken() {
  csrfToken = null
  csrfHeaderName = 'X-XSRF-TOKEN'
}

function getRequestMethod(options) {
  return (options.method || 'GET').toUpperCase()
}

function isUnsafeMethod(method) {
  return !SAFE_METHODS.has(method)
}

// Spring Security SPA CSRF 설정이 내려준 XSRF-TOKEN 쿠키 값을 읽는다.
function readCookie(name) {
  if (typeof document === 'undefined' || !document.cookie) {
    return ''
  }

  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(`${name}=`))

  if (!cookie) {
    return ''
  }

  try {
    return decodeURIComponent(cookie.slice(name.length + 1))
  } catch {
    return cookie.slice(name.length + 1)
  }
}

// 상태 변경 요청 전에 /api/csrf를 호출해 토큰 쿠키를 발급받고, 이후 요청에서는 메모리 캐시를 재사용한다.
async function loadCsrfToken() {
  if (csrfToken) {
    return csrfToken
  }

  if (!csrfTokenPromise) {
    csrfTokenPromise = fetch('/api/csrf', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) {
          throw new ApiError(response.status)
        }

        const data = await response.json()
        csrfHeaderName = data.headerName || csrfHeaderName
        // csrf.spa()에서는 헤더에 쿠키의 원본 토큰 값을 보내야 검증을 통과한다.
        csrfToken = readCookie(CSRF_COOKIE_NAME) || data.token
        return csrfToken
      })
      .finally(() => {
        csrfTokenPromise = null
      })
  }

  return csrfTokenPromise
}

// 기존 헤더를 보존하면서 CSRF 검증용 헤더만 추가한다.
function withCsrfHeader(headers, token) {
  const mergedHeaders = new Headers(headers || {})
  if (token && !mergedHeaders.has(csrfHeaderName)) {
    mergedHeaders.set(csrfHeaderName, token)
  }
  return mergedHeaders
}

// POST/PUT/PATCH/DELETE 같은 unsafe method 요청에만 CSRF 토큰을 자동으로 붙인다.
export async function fetchWithCsrf(url, options = {}) {
  const method = getRequestMethod(options)
  const requestOptions = { ...options, method, credentials: 'include' }

  if (isUnsafeMethod(method)) {
    const token = await loadCsrfToken()
    requestOptions.headers = withCsrfHeader(options.headers, token)
  }

  const response = await fetch(url, requestOptions)
  if (url === '/api/login' || url === '/api/logout' || response.status === 403) {
    clearCsrfToken()
  }
  return response
}

export async function request(url, options = {}) {
  const response = await fetchWithCsrf(url, options)
  if (!response.ok) {
    if (response.status === 401 && onUnauthorized) {
      onUnauthorized()
    }
    throw new ApiError(response.status)
  }
  return response
}

export function postJson(url, body) {
  return request(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}
