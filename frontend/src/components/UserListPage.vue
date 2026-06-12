<script setup>
import { computed, onMounted, ref } from 'vue'
import { NETWORK_ERROR_MESSAGE, readErrorMessage } from '../utils/apiError'

const emit = defineEmits(['back', 'select-user'])

const users = ref([])
const errorMessage = ref('')
const isLoading = ref(false)
const hasLoaded = ref(false)
const filters = ref({
  // 페이징 요청을 위한 검색 조건
  page: 1,
  size: 10,
  status: 'ALL',
  searchType: 'username',
  keyword: '',
})
const pageInfo = ref({
  // 요청 후 응답을 받을 객체
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

function userKey(user) {
  return user.id ?? user.userId ?? user.username
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function buildUserListUrl() {
  const params = new URLSearchParams()
  params.set('page', filters.value.page)
  params.set('size', filters.value.size)
  params.set('status', filters.value.status)

  if (filters.value.keyword.trim()) {
    params.set('searchType', filters.value.searchType)
    params.set('keyword', filters.value.keyword.trim())
  }

  return `/api/admin/users?${params.toString()}`
}

async function loadUsers() {
  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await fetch(buildUserListUrl(), {
      credentials: 'include',
    })

    if (response.ok) {
      const data = await response.json()
      users.value = data.users || []
      pageInfo.value = {
        page: data.page || filters.value.page,
        size: data.size || filters.value.size,
        totalCount: data.totalCount || 0,
        totalPages: data.totalPages || 0,
      }
    } else {
      errorMessage.value = await readErrorMessage(response, '회원 목록을 불러오지 못했습니다.')
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = NETWORK_ERROR_MESSAGE
  } finally {
    isLoading.value = false
    hasLoaded.value = true
  }
}

function searchUsers() {
  filters.value.page = 1
  loadUsers()
}

function resetFilters() {
  filters.value = {
    page: 1,
    size: 10,
    status: 'ALL',
    searchType: 'username',
    keyword: '',
  }
  loadUsers()
}

function changePage(page) {
  if (page < 1 || page > pageInfo.value.totalPages || page === pageInfo.value.page) return

  filters.value.page = page
  loadUsers()
}

onMounted(loadUsers)
</script>

<template>
  <div class="user-list-page">
    <div class="user-list-panel">
      <div class="user-list-header">
        <div>
          <h2>전체 회원 조회</h2>
          <p>가입된 회원 목록을 확인합니다.</p>
        </div>
        <button class="back-button" @click="emit('back')">돌아가기</button>
      </div>

      <p v-if="isLoading && !hasLoaded" class="guide-message">회원 목록을 불러오는 중입니다.</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <form class="filter-bar" @submit.prevent="searchUsers">
        <label>
          <span>상태</span>
          <select v-model="filters.status" :disabled="isLoading" @change="searchUsers">
            <option value="ALL">전체</option>
            <option value="ACTIVE">활성</option>
            <option value="SUSPENDED">정지</option>
            <option value="DELETED">삭제</option>
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
              <th>ID</th>
              <th>아이디</th>
              <th>닉네임</th>
              <th>권한</th>
              <th>생성일</th>
              <th>삭제일</th>
              <th>계정 상태</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="userKey(user)">
              <td>{{ user.id ?? user.userId ?? '-' }}</td>
              <td>
                <button class="username-button" @click="emit('select-user', user.username)">
                  {{ user.username }}
                </button>
              </td>
              <td>{{ user.nickname || '-' }}</td>
              <td>{{ user.role || 'USER' }}</td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>{{ formatDate(user.deletedAt) }}</td>
              <td>{{ user.accountStatus || 'ACTIVE' }}</td>
            </tr>
            <tr v-if="!isLoading && users.length === 0">
              <td colspan="7" class="empty-message">조회된 회원이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pagination">
        <span class="page-summary">총 {{ pageInfo.totalCount }}명</span>
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
.user-list-page {
  flex: 1;
  padding: 28px 36px;
  overflow: auto;
  background: white;
}

.user-list-panel {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 0;
  box-shadow: none;
}

.user-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.user-list-header h2 {
  margin-bottom: 4px;
  color: #333;
}

.user-list-header p {
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
.reset-button {
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

.reset-button {
  background: white;
  color: #777;
  border: 1px solid #ddd;
}

.reset-button:hover {
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
  min-width: 860px;
  border-collapse: collapse;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  text-align: left;
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

.empty-message {
  text-align: center;
  color: #999;
}

.username-button {
  padding: 0;
  background: none;
  color: #4a90d9;
  border: none;
  font-size: 14px;
  cursor: pointer;
}

.username-button:hover {
  color: #1a56db;
  text-decoration: underline;
}

.back-button {
  padding: 8px 14px;
  background: white;
  color: #4a90d9;
  border: 1px solid #4a90d9;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.back-button:hover {
  background: #f0f6fd;
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
  .user-list-page {
    padding: 18px;
  }

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
