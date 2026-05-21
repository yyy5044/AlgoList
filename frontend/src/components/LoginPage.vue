<script setup>
import { ref } from 'vue'

const emit = defineEmits(['login-success'])

const username = ref('')
const password = ref('')
const errorMessage = ref('')

async function login() {
  if (!username.value || !password.value) {
    errorMessage.value = '아이디와 비밀번호를 입력해주세요.'
    return
  }

  try {
    const response = await fetch('/api/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({
        username: username.value,
        password: password.value
      })
    })

    if (response.ok) {
      const data = await response.json()
      emit('login-success', data.username)
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
      <div class="form-group">
        <input v-model="username" placeholder="아이디" @keyup.enter="login" />
      </div>
      <div class="form-group">
        <input v-model="password" type="password" placeholder="비밀번호" @keyup.enter="login" />
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <button @click="login">로그인</button>
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
</style>
