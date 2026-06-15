export const NETWORK_ERROR_MESSAGE = '서버에 연결할 수 없습니다.'

export async function readErrorMessage(response, fallbackMessage) {
  try {
    const data = await response.clone().json()
    return data.message || fallbackMessage
  } catch {
    return fallbackMessage
  }
}
