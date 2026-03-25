<script setup>
import { ref, onMounted } from 'vue'
import LoginPage from './components/LoginPage.vue'
import ListSection from './components/ListSection.vue'
import DetailSection from './components/DetailSection.vue'

const isLoggedIn = ref(false)
const currentUser = ref('')
const selectedItem = ref(null)

// 페이지 로드 시 로그인 상태 확인
onMounted(async () => {
  try {
    const response = await fetch('/api/me', {
      credentials: 'include'
    })
    if (response.ok) {
      const data = await response.json()
      isLoggedIn.value = true
      currentUser.value = data.username
    }
  } catch (error) {
    console.error('로그인 상태 확인 실패:', error)
  }
})

function onLoginSuccess(username) {
  isLoggedIn.value = true
  currentUser.value = username
}

async function logout() {
  try {
    await fetch('/api/logout', {
      method: 'POST',
      credentials: 'include'
    })
  } catch (error) {
    console.error('로그아웃 실패:', error)
  }
  isLoggedIn.value = false
  currentUser.value = ''
  selectedItem.value = null
}

function onSelectItem(item) {
  selectedItem.value = item
}
</script>

<template>
  <LoginPage v-if="!isLoggedIn" @login-success="onLoginSuccess" />
  <div v-else class="outer-container">
    <div class="app-container">
      <div class="top-bar">
        <span>{{ currentUser }}님 환영합니다</span>
        <button @click="logout" class="logout-btn">로그아웃</button>
      </div>
      <div class="main-content">
        <DetailSection :selected-item="selectedItem" :username="username" />
        <ListSection @select-item="onSelectItem" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.outer-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #e9ecef;
}

.app-container {
  display: flex;
  flex-direction: column;
  width: 90vw;
  height: 85vh;
  max-width: 1500px;
  max-height: 1000px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.top-bar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  color: #666;
}

.logout-btn {
  padding: 4px 12px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
}

</style>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  line-height: 1.4;
}
</style>
