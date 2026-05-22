<script setup>
import { ref } from 'vue'

const props = defineProps({
  username: String,
})

const emit = defineEmits(['back'])

const password = ref('')
const passwordConfirm = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const isSubmitting = ref(false)

async function updatePassword() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!password.value || !passwordConfirm.value) {
    errorMessage.value = '새 비밀번호를 모두 입력해주세요.'
    return
  }

  if (password.value !== passwordConfirm.value) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  try {
    isSubmitting.value = true
    const response = await fetch(`/api/users/${encodeURIComponent(props.username)}/password`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        password: password.value,
      }),
    })

    if (response.ok) {
      successMessage.value = '비밀번호가 수정되었습니다.'
      password.value = ''
      passwordConfirm.value = ''
    } else {
      errorMessage.value = '비밀번호 수정에 실패했습니다.'
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="edit-page">
    <div class="edit-panel">
      <h2>비밀번호 수정</h2>
      <p class="edit-subtitle">{{ username }}님의 비밀번호를 변경합니다.</p>

      <div class="form-group">
        <input
          v-model="password"
          type="password"
          placeholder="새 비밀번호"
          @keyup.enter="updatePassword"
        />
      </div>
      <div class="form-group">
        <input
          v-model="passwordConfirm"
          type="password"
          placeholder="새 비밀번호 확인"
          @keyup.enter="updatePassword"
        />
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

      <button @click="updatePassword" :disabled="isSubmitting">
        {{ isSubmitting ? '수정 중...' : '수정 완료' }}
      </button>
      <button class="back-button" @click="emit('back')">뒤로가기</button>
    </div>
  </div>
</template>

<style scoped>
.edit-page {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  background: #f5f5f5;
}

.edit-panel {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 360px;
  text-align: center;
}

.edit-panel h2 {
  margin-bottom: 4px;
  color: #333;
}

.edit-subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 12px;
}

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
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
