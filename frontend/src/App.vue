<script setup>
import { computed, ref, onMounted } from 'vue'
import LoginPage from './components/LoginPage.vue'
import SignupPage from './components/SignupPage.vue'
import UserDetailPage from './components/UserDetailPage.vue'
import UserListPage from './components/UserListPage.vue'
import UserPasswordEditPage from './components/UserPasswordEditPage.vue'
import ListSection from './components/ListSection.vue'
import DetailSection from './components/DetailSection.vue'

const isLoggedIn = ref(false)
const currentUser = ref('')
const currentUserRole = ref('')
const selectedItem = ref(null)
const selectedAdminUsername = ref('') // 어드민이 조회한 유저의 이름
const authPage = ref('login')
const mainPage = ref('problems')
const isUserListPath = ref(window.location.pathname === '/users')
const isAdmin = computed(() => currentUserRole.value === 'ADMIN')

// 페이지 로드 시 로그인 상태 확인
onMounted(async () => {
  try {
    const response = await fetch('/api/me', {
      credentials: 'include',
    })

    if (response.ok) {
      const data = await response.json()
      isLoggedIn.value = true
      currentUser.value = data.username
      currentUserRole.value = data.role || ''
      if (isUserListPath.value && !isAdmin.value) {
        showProblemPage()
      }
    } else {
      isLoggedIn.value = false
      currentUser.value = ''
      currentUserRole.value = ''
    }
  } catch (error) {
    console.error('로그인 상태 확인 실패:', error)
    isLoggedIn.value = false
    currentUser.value = ''
    currentUserRole.value = ''
  }
})

function onLoginSuccess(user) {
  isLoggedIn.value = true
  currentUser.value = user.username
  currentUserRole.value = user.role || ''
  authPage.value = 'login'
  mainPage.value = 'problems'
  if (isUserListPath.value && !isAdmin.value) {
    showProblemPage()
  }
}

async function logout() {
  try {
    await fetch('/api/logout', {
      method: 'POST',
      credentials: 'include',
    })
  } catch (error) {
    console.error('로그아웃 실패:', error)
  }
  isLoggedIn.value = false
  currentUser.value = ''
  currentUserRole.value = ''
  selectedItem.value = null
  selectedAdminUsername.value = ''
  authPage.value = 'login'
  mainPage.value = 'problems'
}

function onSelectItem(item) {
  selectedItem.value = item
}

function showSignupPage() {
  authPage.value = 'signup'
}

function showLoginPage() {
  authPage.value = 'login'
}

function showProblemPage() {
  mainPage.value = 'problems'
  selectedAdminUsername.value = ''
  if (isUserListPath.value) {
    window.history.pushState({}, '', '/')
    isUserListPath.value = false
  }
}

function showUserDetailPage() {
  mainPage.value = 'user-detail'
}

function showUserListPage() {
  if (!isAdmin.value) return

  selectedAdminUsername.value = ''
  window.history.pushState({}, '', '/users')
  isUserListPath.value = true
}

function showAdminUserDetailPage(username) {
  selectedAdminUsername.value = username
}

function showUserPasswordEditPage() {
  mainPage.value = 'user-password-edit'
}
</script>

<template>
  <SignupPage v-if="!isLoggedIn && authPage === 'signup'" @back-to-login="showLoginPage" />
  <LoginPage
    v-else-if="!isLoggedIn"
    @login-success="onLoginSuccess"
    @signup-click="showSignupPage"
  />
  <div v-else class="outer-container">
    <div class="app-container">
      <div class="top-bar">
        <button v-if="isAdmin" class="user-link" @click="showUserListPage">유저 목록 확인</button>
        <button class="user-link" @click="showUserDetailPage">
          {{ currentUser }}님 환영합니다
        </button>
        <button @click="logout" class="logout-btn">로그아웃</button>
      </div>
      <UserDetailPage
        v-if="isUserListPath && isAdmin && selectedAdminUsername"
        :username="selectedAdminUsername"
        api-base-path="/api/admin/users"
        :show-actions="false"
        @back="showUserListPage"
      />
      <UserListPage
        v-else-if="isUserListPath && isAdmin"
        @back="showProblemPage"
        @select-user="showAdminUserDetailPage"
      />
      <UserDetailPage
        v-else-if="mainPage === 'user-detail'"
        :username="currentUser"
        @back="showProblemPage"
        @edit-password="showUserPasswordEditPage"
        @delete-success="logout"
      />
      <UserPasswordEditPage
        v-else-if="mainPage === 'user-password-edit'"
        :username="currentUser"
        @back="showUserDetailPage"
      />
      <div v-else class="main-content">
        <DetailSection :selected-item="selectedItem" :username="currentUser" />
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

.user-link {
  padding: 4px 0;
  background: none;
  color: #666;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.user-link:hover {
  color: #1a56db;
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
