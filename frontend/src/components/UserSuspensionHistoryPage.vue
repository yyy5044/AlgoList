<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchWithCsrf } from '../api/http'
import { NETWORK_ERROR_MESSAGE, readErrorMessage } from '../utils/apiError'

const emit = defineEmits(['back'])

const suspensions = ref([])
const errorMessage = ref('')
const isLoading = ref(false)
const hasLoaded = ref(false)
const filters = ref({
  page: 1,
  size: 10,
  status: 'ALL',
  searchType: 'username',
  keyword: '',
})
const pageInfo = ref({
  page: 1,
  size: 10,
  totalCount: 0,
  totalPages: 0,
})

const visiblePages = computed(() => {
  const totalPages = pageInfo.value.totalPages
  const currentPage = pageInfo.value.page
  const startPage = Math.max(currentPage - 2, 1)
  const endPage = Math.min(startPage + 4, totalPages)
  const pages = []

  for (let page = startPage; page <= endPage; page++) {
    pages.push(page)
  }

  return pages
})

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function suspensionKey(suspension) {
  return suspension.suspensionId
}

function statusLabel(status) {
  return status === 'ACTIVE' ? '활성 정지' : '해제됨'
}

function buildSuspensionHistoryUrl() {
  const params = new URLSearchParams()
  params.set('page', filters.value.page)
  params.set('size', filters.value.size)
  params.set('status', filters.value.status)

  if (filters.value.keyword.trim()) {
    params.set('searchType', filters.value.searchType)
    params.set('keyword', filters.value.keyword.trim())
  }

  return `/api/admin/user-suspensions?${params.toString()}`
}

async function loadSuspensions() {
  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await fetchWithCsrf(buildSuspensionHistoryUrl(), {
      credentials: 'include',
    })

    if (response.ok) {
      const data = await response.json()
      suspensions.value = data.suspensions || []
      pageInfo.value = {
        page: data.page || filters.value.page,
        size: data.size || filters.value.size,
        totalCount: data.totalCount || 0,
        totalPages: data.totalPages || 0,
      }
    } else {
      errorMessage.value = await readErrorMessage(response, '정지 이력을 불러오지 못했습니다.')
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = NETWORK_ERROR_MESSAGE
  } finally {
    isLoading.value = false
    hasLoaded.value = true
  }
}

function searchSuspensions() {
  filters.value.page = 1
  loadSuspensions()
}

function resetFilters() {
  filters.value = {
    page: 1,
    size: 10,
    status: 'ALL',
    searchType: 'username',
    keyword: '',
  }
  loadSuspensions()
}

function changePage(page) {
  if (page < 1 || page > pageInfo.value.totalPages || page === pageInfo.value.page) return

  filters.value.page = page
  loadSuspensions()
}

onMounted(loadSuspensions)
</script>

<template>
  <div class="suspension-history-page">
    <div class="suspension-history-panel">
      <div class="suspension-history-header">
        <div>
          <h2>정지 이력 조회</h2>
          <p>회원 정지와 해제 기록을 확인합니다.</p>
        </div>
        <button class="back-button" @click="emit('back')">회원 목록</button>
      </div>

      <p v-if="isLoading && !hasLoaded" class="guide-message">정지 이력을 불러오는 중입니다.</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <form class="filter-bar" @submit.prevent="searchSuspensions">
        <label>
          <span>상태</span>
          <select v-model="filters.status" :disabled="isLoading" @change="searchSuspensions">
            <option value="ALL">전체</option>
            <option value="ACTIVE">활성 정지</option>
            <option value="RELEASED">해제됨</option>
          </select>
        </label>

        <label>
          <span>검색 조건</span>
          <select v-model="filters.searchType" :disabled="isLoading">
            <option value="username">아이디</option>
            <option value="nickname">닉네임</option>
          </select>
        </label>

        <label class="keyword-field">
          <span>검색어</span>
          <input
            v-model="filters.keyword"
            placeholder="검색어를 입력하세요"
            :disabled="isLoading"
          />
        </label>

        <button type="submit" class="search-button" :disabled="isLoading">검색</button>
        <button type="button" class="reset-button" :disabled="isLoading" @click="resetFilters">
          초기화
        </button>
      </form>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>대상 유저</th>
              <th>상태</th>
              <th>정지 사유</th>
              <th>정지일</th>
              <th>정지 종료일</th>
              <th>정지 처리자</th>
              <th>해제일</th>
              <th>해제 처리자</th>
              <th>해제 사유</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="suspension in suspensions" :key="suspensionKey(suspension)">
              <td>
                <div class="user-cell">
                  <strong>{{ suspension.username }}</strong>
                  <span>{{ suspension.nickname || '-' }} · {{ suspension.accountStatus || 'ACTIVE' }}</span>
                </div>
              </td>
              <td>
                <span
                  class="status-badge"
                  :class="{ active: suspension.suspensionStatus === 'ACTIVE' }"
                >
                  {{ statusLabel(suspension.suspensionStatus) }}
                </span>
              </td>
              <td class="reason-cell">{{ suspension.reason || '-' }}</td>
              <td>{{ formatDate(suspension.suspendedAt) }}</td>
              <td>{{ formatDate(suspension.suspendedUntil) }}</td>
              <td>{{ suspension.suspendedByUsername || '-' }}</td>
              <td>{{ formatDate(suspension.releasedAt) }}</td>
              <td>{{ suspension.releasedByUsername || '-' }}</td>
              <td class="reason-cell">{{ suspension.releaseReason || '-' }}</td>
            </tr>
            <tr v-if="!isLoading && suspensions.length === 0">
              <td colspan="9" class="empty-message">조회된 정지 이력이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-summary">총 {{ pageInfo.totalCount }}건</span>
        <div class="page-buttons">
          <button
            type="button"
            class="page-button"
            :disabled="isLoading || pageInfo.page <= 1"
            @click="changePage(pageInfo.page - 1)"
          >
            이전
          </button>
          <button
            v-for="page in visiblePages"
            :key="page"
            type="button"
            class="page-button"
            :class="{ active: page === pageInfo.page }"
            :disabled="isLoading"
            @click="changePage(page)"
          >
            {{ page }}
          </button>
          <button
            type="button"
            class="page-button"
            :disabled="isLoading || pageInfo.page >= pageInfo.totalPages"
            @click="changePage(pageInfo.page + 1)"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.suspension-history-page {
  flex: 1;
  padding: 28px 36px;
  overflow: auto;
  background: white;
}

.suspension-history-panel {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
}

.suspension-history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.suspension-history-header h2 {
  margin-bottom: 4px;
  color: #333;
}

.suspension-history-header p {
  color: #999;
  font-size: 14px;
}

.guide-message {
  color: #999;
  font-size: 13px;
  margin-bottom: 12px;
}

.error {
  color: #e74c3c;
  font-size: 13px;
  margin-bottom: 12px;
}

.filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 16px;
}

.filter-bar label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-bar span {
  color: #777;
  font-size: 13px;
}

.filter-bar select,
.filter-bar input {
  height: 38px;
  padding: 0 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #333;
  font-size: 14px;
  background: white;
}

.filter-bar select:disabled,
.filter-bar input:disabled {
  color: #999;
  background: #f8f9fa;
  cursor: default;
}

.keyword-field {
  flex: 1;
}

.search-button,
.reset-button,
.back-button {
  height: 38px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.search-button {
  background: #4a90d9;
  color: white;
  border: 1px solid #4a90d9;
}

.search-button:hover {
  background: #3a7bc8;
}

.reset-button,
.back-button {
  background: white;
  color: #777;
  border: 1px solid #ddd;
}

.reset-button:hover,
.back-button:hover {
  background: #f8f9fa;
}

.search-button:disabled,
.reset-button:disabled {
  color: #bbb;
  background: #f8f9fa;
  border-color: #eee;
  cursor: default;
}

.table-wrap {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow-x: auto;
  overflow-y: hidden;
}

table {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
  vertical-align: top;
  font-size: 14px;
}

th {
  background: #f8f9fa;
  color: #666;
  font-weight: 600;
}

td {
  color: #333;
}

tbody tr:last-child td {
  border-bottom: none;
}

.user-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-cell strong {
  color: #333;
}

.user-cell span {
  color: #999;
  font-size: 12px;
}

.status-badge {
  display: inline-block;
  min-width: 64px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f1f3f5;
  color: #777;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.active {
  background: #fff3cd;
  color: #946200;
}

.reason-cell {
  max-width: 220px;
  white-space: normal;
  word-break: break-word;
}

.empty-message {
  text-align: center;
  color: #999;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.page-summary {
  color: #777;
  font-size: 13px;
}

.page-buttons {
  display: flex;
  gap: 6px;
}

.page-button {
  min-width: 36px;
  height: 34px;
  padding: 0 10px;
  background: white;
  color: #4a90d9;
  border: 1px solid #d8e7f6;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.page-button:hover {
  background: #f0f6fd;
}

.page-button.active {
  background: #4a90d9;
  color: white;
  border-color: #4a90d9;
}

.page-button:disabled {
  color: #bbb;
  background: #f8f9fa;
  border-color: #eee;
  cursor: default;
}

@media (max-width: 720px) {
  .suspension-history-page {
    padding: 18px;
  }

  .suspension-history-header,
  .filter-bar,
  .pagination {
    align-items: stretch;
    flex-direction: column;
  }

  .page-buttons {
    flex-wrap: wrap;
  }
}
</style>
