/**
 * 관리자(admin) 도메인 API.
 * `import * as adminApi from '@/api/admin'` 형태로 사용한다.
 * 모든 요청은 /api/admin/** 로 가며, 백엔드 시큐리티에서 ROLE_ADMIN 만 허용한다.
 */
import { postJson, request } from './http'

/** 응답 body 가 JSON 이면 파싱, 비어 있으면 빈 객체를 돌려준다. */
async function readJsonSafely(response) {
  try {
    return await response.json()
  } catch {
    return {}
  }
}

/** 코드포스 문제 수집 배치 실행 트리거 */
export async function runCodeforcesIngest() {
  const response = await postJson('/api/admin/batch/codeforces', {})
  return readJsonSafely(response)
}

/** 코드포스 문제 수집 배치 중지(graceful stop → STOPPED, 다음 실행 시 이어서 재개) */
export async function stopCodeforcesIngest() {
  const response = await postJson('/api/admin/batch/codeforces/stop', {})
  return readJsonSafely(response)
}

/** GitHub(백준) 문제 수집 배치 실행 트리거 */
export async function runGitHubIngest() {
  const response = await postJson('/api/admin/batch/github', {})
  return readJsonSafely(response)
}

/** GitHub(백준) 문제 수집 배치 중지 */
export async function stopGitHubIngest() {
  const response = await postJson('/api/admin/batch/github/stop', {})
  return readJsonSafely(response)
}

/** 관리자 회원 상세: 특정 회원의 등록 문제 목록 조회 */
export async function fetchUserProblems(username) {
  const response = await request(`/api/admin/users/${encodeURIComponent(username)}/problems`)
  return response.json()
}

/** 관리자 회원 상세: 특정 회원의 문제별 풀이 목록 조회(code 미포함) */
export async function fetchUserProblemSolutions(username, userProblemId) {
  const response = await request(
    `/api/admin/users/${encodeURIComponent(username)}/problems/${userProblemId}/solutions`,
  )
  return response.json()
}

/** 관리자 회원 상세: 특정 회원의 풀이 상세 조회(code 포함) */
export async function fetchUserSolution(username, userProblemId, solutionId) {
  const response = await request(
    `/api/admin/users/${encodeURIComponent(username)}/problems/${userProblemId}/solutions/${solutionId}`,
  )
  return response.json()
}
