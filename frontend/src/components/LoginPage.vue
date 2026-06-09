<script setup>
import { ref } from 'vue'

const emit = defineEmits(['login-success', 'signup-click'])
const props = defineProps({
  successMessage: {
    type: String,
    default: '',
  },
})

const username = ref('')
const password = ref('')
const errorMessage = ref('')
const suspensionInfo = ref(null)

async function login() {
  errorMessage.value = ''
  suspensionInfo.value = null

  if (!username.value || !password.value) {
    errorMessage.value = '아이디와 비밀번호를 입력해주세요.'
    return
  }

  try {
    const formData = new URLSearchParams()
    formData.append('username', username.value)
    formData.append('password', password.value)

    const response = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      credentials: 'include',
      body: formData,
    })

    if (response.ok) {
      const data = await response.json()
      emit('login-success', data)
    } else if (response.status === 403) {
      try {
        const data = await response.json()
        suspensionInfo.value = {
          message: data.message || '정지된 계정입니다.',
          suspendedUntilDate: data.suspendedUntilDate || '-',
          reason: data.reason || '등록된 정지 사유가 없습니다.',
        }
      } catch (error) {
        console.log(error)
        errorMessage.value = '정지된 계정입니다.'
      }
    } else {
      errorMessage.value = '아이디 또는 비밀번호가 틀렸습니다.'
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <h1>AlgoList</h1>
      <p class="login-subtitle">알고리즘 문제 관리 서비스</p>
      <p v-if="props.successMessage" class="success">{{ props.successMessage }}</p>
      <div class="form-group">
        <input v-model="username" placeholder="아이디" @keyup.enter="login" />
      </div>
      <div class="form-group">
        <input v-model="password" type="password" placeholder="비밀번호" @keyup.enter="login" />
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <div v-if="suspensionInfo" class="suspension-message">
        <strong>{{ suspensionInfo.message }}</strong>
        <p>정지 종료일: {{ suspensionInfo.suspendedUntilDate }}</p>
        <p>정지 사유: {{ suspensionInfo.reason }}</p>
      </div>
      <button @click="login">로그인</button>
      <button class="signup-link" @click="emit('signup-click')">회원가입</button>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f5f5;
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 360px;
  text-align: center;
}

.login-box h1 {
  margin-bottom: 4px;
  color: #333;
}

.login-subtitle {
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

.suspension-message {
  padding: 12px;
  margin-bottom: 12px;
  background: #fff5f5;
  color: #9f2a21;
  border: 1px solid #f3c2bc;
  border-radius: 6px;
  text-align: left;
  font-size: 13px;
}

.suspension-message strong {
  display: block;
  margin-bottom: 6px;
}

.suspension-message p {
  margin: 2px 0;
  word-break: break-word;
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

.signup-link {
  margin-top: 10px;
  background: white;
  color: #4a90d9;
  border: 1px solid #4a90d9;
}

.signup-link:hover {
  background: #f0f6fd;
}
</style>
