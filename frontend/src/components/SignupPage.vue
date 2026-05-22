<script setup>
import { ref } from 'vue'

const emit = defineEmits(['back-to-login'])

const username = ref('')
const password = ref('')
const passwordConfirm = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const isSubmitting = ref(false)

async function signup() {
  errorMessage.value = ''
  successMessage.value = ''

  if (!username.value || !password.value || !passwordConfirm.value) {
    errorMessage.value = '아이디와 비밀번호를 모두 입력해주세요.'
    return
  }

  if (password.value !== passwordConfirm.value) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  try {
    isSubmitting.value = true
    const response = await fetch('/api/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        username: username.value,
        password: password.value,
      }),
    })

    if (response.ok) {
      successMessage.value = '회원가입이 완료되었습니다. 로그인해주세요.'
      username.value = ''
      password.value = ''
      passwordConfirm.value = ''
    } else {
      errorMessage.value = '회원가입에 실패했습니다.'
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
  <div class="signup-container">
    <div class="signup-box">
      <h1>AlgoList</h1>
      <p class="signup-subtitle">알고리즘 문제 관리 서비스</p>
      <div class="form-group">
        <input v-model="username" placeholder="아이디" @keyup.enter="signup" />
      </div>
      <div class="form-group">
        <input v-model="password" type="password" placeholder="비밀번호" @keyup.enter="signup" />
      </div>
      <div class="form-group">
        <input
          v-model="passwordConfirm"
          type="password"
          placeholder="비밀번호 확인"
          @keyup.enter="signup"
        />
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>
      <button @click="signup" :disabled="isSubmitting">
        {{ isSubmitting ? '가입 중...' : '회원가입' }}
      </button>
      <button class="login-link" @click="emit('back-to-login')">로그인으로 돌아가기</button>
    </div>
  </div>
</template>

<style scoped>
.signup-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f5f5;
}

.signup-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 360px;
  text-align: center;
}

.signup-box h1 {
  margin-bottom: 4px;
  color: #333;
}

.signup-subtitle {
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

.login-link {
  margin-top: 10px;
  background: white;
  color: #4a90d9;
  border: 1px solid #4a90d9;
}

.login-link:hover {
  background: #f0f6fd;
}
</style>
