<script setup>
import { onMounted, ref } from 'vue'

const emit = defineEmits(['back'])

const users = ref([])
const errorMessage = ref('')
const isLoading = ref(false)

function userKey(user) {
  return user.id ?? user.userId ?? user.username
}

async function loadUsers() {
  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await fetch('/api/admin/users', {
      credentials: 'include',
    })

    if (response.ok) {
      users.value = await response.json()
    } else {
      errorMessage.value = '회원 목록을 불러오지 못했습니다.'
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isLoading.value = false
  }
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

      <p v-if="isLoading" class="guide-message">회원 목록을 불러오는 중입니다.</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>아이디</th>
              <th>권한</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="userKey(user)">
              <td>{{ user.id ?? user.userId ?? '-' }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.role || 'USER' }}</td>
            </tr>
            <tr v-if="!isLoading && users.length === 0">
              <td colspan="3" class="empty-message">조회된 회원이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.user-list-page {
  flex: 1;
  padding: 32px;
  overflow: auto;
  background: #f5f5f5;
}

.user-list-panel {
  max-width: 900px;
  margin: 0 auto;
  background: white;
  padding: 28px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
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

.table-wrap {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

table {
  width: 100%;
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
</style>
