/**
 * problem 도메인 API.
 * `import * as problemApi from '@/api/problems'` 형태로 사용한다.
 */
import { request, postJson } from './http'

/** 내 문제 전체 조회 */
export async function fetchMine() {
  const response = await request('/api/problems')
  return response.json()
}

/** 문제 검색 (DB 우선, 없으면 GitHub 실시간 수집) */
export async function search(query) {
  const response = await request(`/api/problems/search?query=${encodeURIComponent(query)}`)
  return response.json()
}

/** 내 문제에 추가. 성공 시 생성된 UserProblem을 반환 */
export async function add(problemId) {
  const response = await postJson('/api/problems', { problemId })
  return response.json()
}

/** 내 문제에서 삭제 */
export async function remove(problemId) {
  await request(`/api/problems/${problemId}`, { method: 'DELETE' })
}

/** 사이트별 문제 목록 한 페이지 조회 */
export async function browse(site, page, size) {
  const params = new URLSearchParams({ page, size })
  const response = await request(`/api/problems/browse/${site}?${params}`)
  return response.json()
}

/** 문제 본문 조회 (목록 응답에는 본문이 빠져 있어 모달에서 따로 가져온다) */
export async function fetchDescription(problemId) {
  const response = await request(`/api/problems/detail/${problemId}`)
  return response.text()
}
