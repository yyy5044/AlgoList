<script setup>
import { onMounted, ref } from 'vue'

const props = defineProps({
  username: String,
})

const emit = defineEmits(['back', 'edit-password', 'delete-success'])

const user = ref({
  username: props.username,
  role: '',
})
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const isDeleting = ref(false)

async function loadUser() {
  if (!props.username) return

  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await fetch(`/api/users/${encodeURIComponent(props.username)}`, {
      credentials: 'include',
    })

    if (response.ok) {
      user.value = await response.json()
    } else {
      errorMessage.value = '회원 정보를 불러오지 못했습니다.'
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isLoading.value = false
  }
}

onMounted(loadUser)

async function deleteUser() {
  const confirmed = window.confirm('정말 탈퇴하시겠습니까?')
  if (!confirmed) return

  try {
    isDeleting.value = true
    errorMessage.value = ''
    successMessage.value = ''
    const response = await fetch(`/api/users/${encodeURIComponent(props.username)}`, {
      method: 'DELETE',
      credentials: 'include',
    })

    if (response.ok) {
      successMessage.value = '회원 탈퇴가 완료되었습니다.'
      emit('delete-success')
    } else {
      errorMessage.value = '회원 탈퇴에 실패했습니다.'
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isDeleting.value = false
  }
}
</script>

<template>
  <div class="user-page">
    <div class="user-panel">
      <h2>회원 상세</h2>
      <p class="user-subtitle">내 계정 정보를 확인할 수 있습니다.</p>

      <p v-if="isLoading" class="guide-message">회원 정보를 불러오는 중입니다.</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

      <div class="info-list">
        <div class="info-row">
          <span class="info-label">아이디</span>
          <span class="info-value">{{ user.username }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">권한</span>
          <span class="info-value">{{ user.role || 'USER' }}</span>
        </div>
      </div>

      <button @click="emit('edit-password')">수정</button>
      <button class="delete-button" @click="deleteUser" :disabled="isDeleting">
        {{ isDeleting ? '탈퇴 중...' : '회원 탈퇴' }}
      </button>
      <button class="back-button" @click="emit('back')">뒤로가기</button>
    </div>
  </div>
</template>

<style scoped>
.user-page {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  background: #f5f5f5;
}

.user-panel {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 360px;
  text-align: center;
}

.user-panel h2 {
  margin-bottom: 4px;
  color: #333;
}

.user-subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 24px;
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

.success {
  color: #2e7d32;
  font-size: 13px;
  margin-bottom: 12px;
}

.info-list {
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: #888;
}

.info-value {
  color: #333;
  font-weight: 600;
}

button {
  width: 100%;
  padding: 10px;
  background: #4a90d9;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
}

button:hover {
  background: #3a7bc8;
}

button:disabled {
  background: #9bbfe5;
  cursor: default;
}

.delete-button {
  margin-top: 10px;
  background: #e74c3c;
}

.delete-button:hover {
  background: #d43f30;
}

.back-button {
  margin-top: 10px;
  background: white;
  color: #4a90d9;
  border: 1px solid #4a90d9;
}

.back-button:hover {
  background: #f0f6fd;
}
</style>
