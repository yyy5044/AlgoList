<script setup>
import { computed, ref, onMounted } from 'vue'
import { setOnUnauthorized } from '@/api/http'
import * as authApi from '@/api/auth'
import LoginPage from './components/LoginPage.vue'
import SignupPage from './components/SignupPage.vue'
import UserDetailPage from './components/UserDetailPage.vue'
import UserListPage from './components/UserListPage.vue'
import UserSuspensionHistoryPage from './components/UserSuspensionHistoryPage.vue'
import BatchTriggerPage from './components/BatchTriggerPage.vue'
import UserProfileEditPage from './components/UserPasswordEditPage.vue'
import ListSection from './components/ListSection.vue'
import DetailSection from './components/DetailSection.vue'
import BrowsePage from './components/BrowsePage.vue'

const isLoggedIn = ref(false)
const currentUser = ref('')
const currentUserRole = ref('')
const selectedItem = ref(null)
const selectedAdminUsername = ref('') // 어드민이 조회한 유저의 이름
const authPage = ref('login')
const mainPage = ref('problems')
// 상단 탭: 'main' = 메인 페이지(문제 둘러보기), 'my' = 내 문제, 'users' = 어드민 유저 조회
const activeTab = ref('main')
const isUserListPath = ref(window.location.pathname === '/users')
const isUserSuspensionHistoryPath = ref(window.location.pathname === '/users/suspensions')
const isBatchTriggerPath = ref(false) // 어드민 '배치 트리거' 뷰 표시 여부
const loginSuccessMessage = ref('')
const listSectionRef = ref(null) // 둘러보기에서 문제 추가 시 내 문제 목록 갱신용
const isAdmin = computed(() => currentUserRole.value === 'ADMIN')

// 로그아웃·세션 만료 시 화면 상태를 초기값으로 되돌린다
function resetSessionState() {
  isLoggedIn.value = false
  currentUser.value = ''
  currentUserRole.value = ''
  selectedItem.value = null
  selectedAdminUsername.value = ''
  authPage.value = 'login'
  mainPage.value = 'problems'
  activeTab.value = 'main'
  loginSuccessMessage.value = ''
}

setOnUnauthorized(() => resetSessionState())

function hasAdminUserPath() {
  return isUserListPath.value || isUserSuspensionHistoryPath.value
}

function leaveAdminUserPath() {
  if (!hasAdminUserPath()) return

  window.history.pushState({}, '', '/')
  isUserListPath.value = false
  isUserSuspensionHistoryPath.value = false
}

// 페이지 로드 시 로그인 상태 확인
onMounted(async () => {
  try {
    const user = await authApi.fetchCurrentUser()
    if (!user) {
      resetSessionState()
      return
    }
    isLoggedIn.value = true
    currentUser.value = user.username
    currentUserRole.value = user.role || ''
    if (hasAdminUserPath() && isAdmin.value) {
      activeTab.value = 'users'
    } else if (hasAdminUserPath()) {
      showProblemPage()
    }
  } catch (error) {
    console.error('로그인 상태 확인 실패:', error)
    resetSessionState()
  }
})

function onLoginSuccess(user) {
  isLoggedIn.value = true
  currentUser.value = user.username
  currentUserRole.value = user.role || ''
  authPage.value = 'login'
  mainPage.value = 'problems'
  activeTab.value = 'main'
  loginSuccessMessage.value = ''
  if (hasAdminUserPath() && isAdmin.value) {
    activeTab.value = 'users'
  } else if (hasAdminUserPath()) {
    showProblemPage()
  }
}

function onSignupSuccess(message) {
  isLoggedIn.value = false
  authPage.value = 'login'
  loginSuccessMessage.value = message
}

async function logout() {
  try {
    await authApi.logout()
  } catch (error) {
    console.error('로그아웃 실패:', error)
  }
  resetSessionState()
}

function onSelectItem(item) {
  selectedItem.value = item
  mainPage.value = 'problems'
  activeTab.value = 'my'
  selectedAdminUsername.value = ''
  isBatchTriggerPath.value = false
  leaveAdminUserPath()
}

function showSignupPage() {
  authPage.value = 'signup'
  loginSuccessMessage.value = ''
}

function showLoginPage() {
  authPage.value = 'login'
}

function showProblemPage() {
  mainPage.value = 'problems'
  activeTab.value = 'main'
  selectedAdminUsername.value = ''
  isBatchTriggerPath.value = false
  leaveAdminUserPath()
}

function showMyProblemPage() {
  activeTab.value = 'my'
  mainPage.value = 'problems'
  selectedAdminUsername.value = ''
  isBatchTriggerPath.value = false
  leaveAdminUserPath()
}

function showUserDetailPage() {
  mainPage.value = 'user-detail'
  activeTab.value = ''
  selectedAdminUsername.value = ''
  isBatchTriggerPath.value = false
  leaveAdminUserPath()
}

function showUserListPage() {
  if (!isAdmin.value) return

  activeTab.value = 'users'
  selectedAdminUsername.value = ''
  isBatchTriggerPath.value = false
  window.history.pushState({}, '', '/users')
  isUserListPath.value = true
  isUserSuspensionHistoryPath.value = false
}

function showUserSuspensionHistoryPage() {
  if (!isAdmin.value) return

  activeTab.value = 'users'
  selectedAdminUsername.value = ''
  isBatchTriggerPath.value = false
  window.history.pushState({}, '', '/users/suspensions')
  isUserListPath.value = false
  isUserSuspensionHistoryPath.value = true
}

function showBatchTriggerPage() {
  if (!isAdmin.value) return

  activeTab.value = 'my'
  selectedAdminUsername.value = ''
  isUserListPath.value = false
  isUserSuspensionHistoryPath.value = false
  isBatchTriggerPath.value = true
  if (window.location.pathname === '/users' || window.location.pathname === '/users/suspensions') {
    window.history.pushState({}, '', '/')
  }
}

function showAdminUserDetailPage(username) {
  selectedAdminUsername.value = username
}

function showUserProfileEditPage() {
  mainPage.value = 'user-password-edit'
}

// 둘러보기에서 문제를 추가하면 내 문제 목록을 갱신
function onProblemAdded() {
  listSectionRef.value?.refresh()
}

function onUserProblemUpdated(userProblem) {
  if (!userProblem?.userProblemId) return

  if (selectedItem.value?.userProblemId === userProblem.userProblemId) {
    selectedItem.value = {
      ...selectedItem.value,
      ...userProblem,
      problem: userProblem.problem ?? selectedItem.value.problem,
    }
  }

  listSectionRef.value?.refresh()
}
</script>

<template>
  <SignupPage
    v-if="!isLoggedIn && authPage === 'signup'"
    @back-to-login="showLoginPage"
    @signup-success="onSignupSuccess"
  />
  <LoginPage
    v-else-if="!isLoggedIn"
    :success-message="loginSuccessMessage"
    @login-success="onLoginSuccess"
    @signup-click="showSignupPage"
  />
  <div v-else class="outer-container">
    <div class="app-container">
      <div class="top-bar">
        <div class="page-tabs">
          <button
            :class="['page-tab', { active: activeTab === 'main' }]"
            @click="showProblemPage"
          >
            메인 페이지
          </button>
          <button
            :class="['page-tab', { active: activeTab === 'my' }]"
            @click="showMyProblemPage"
          >
            내 문제
          </button>
          <button
            v-if="isAdmin"
            :class="['page-tab', { active: activeTab === 'users' }]"
            @click="showUserListPage"
          >
            유저 조회
          </button>
        </div>
        <div class="top-bar-right">
          <button v-if="isAdmin" class="user-link" @click="showBatchTriggerPage">배치 트리거</button>
          <button class="user-link" @click="showUserDetailPage">
            {{ currentUser }}님 환영합니다
          </button>
          <button @click="logout" class="logout-btn">로그아웃</button>
        </div>
      </div>
      <div class="main-content">
        <KeepAlive>
          <BrowsePage v-if="activeTab === 'main'" @problem-added="onProblemAdded" />
        </KeepAlive>
        <template v-if="activeTab === 'users' && isAdmin">
          <UserDetailPage
            v-if="selectedAdminUsername"
            :username="selectedAdminUsername"
            :current-username="currentUser"
            api-base-path="/api/admin/users"
            :show-actions="false"
            :show-admin-actions="true"
            @back="showUserListPage"
          />
          <UserSuspensionHistoryPage
            v-else-if="isUserSuspensionHistoryPath"
            @back="showUserListPage"
          />
          <UserListPage
            v-else
            @back="showProblemPage"
            @select-user="showAdminUserDetailPage"
            @show-suspension-history="showUserSuspensionHistoryPage"
          />
        </template>
        <template v-else-if="activeTab !== 'main'">
          <BatchTriggerPage
            v-if="isBatchTriggerPath && isAdmin"
            @back="showProblemPage"
          />
          <UserDetailPage
            v-else-if="mainPage === 'user-detail'"
            :username="currentUser"
            @back="showProblemPage"
            @edit-profile="showUserProfileEditPage"
            @delete-success="logout"
          />
          <UserProfileEditPage
            v-else-if="mainPage === 'user-password-edit'"
            :username="currentUser"
            @back="showUserDetailPage"
          />
          <DetailSection
            v-else
            :selected-item="selectedItem"
            :username="currentUser"
            @user-problem-updated="onUserProblemUpdated"
          />
        </template>
        <ListSection
          v-if="activeTab !== 'users'"
          ref="listSectionRef"
          @select-item="onSelectItem"
        />
      </div>
      <footer class="app-footer">
        Codeforces 문제 데이터 출처: <a href="https://huggingface.co/datasets/open-r1/codeforces" target="_blank">open-r1/codeforces</a> (CC-BY-4.0) | 문제 데이터는 권리자 요청 시 삭제할 수 있습니다. 연락처: yyy5044@naver.com
      </footer>
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
  justify-content: space-between;
  align-items: flex-end;
  padding: 8px 16px 0;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  font-size: 14px;
  color: #666;
}

/* 크롬 탭 스타일: 활성 탭이 아래 흰 영역과 이어져 보이도록 */
.page-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: -1px;
}

.page-tab {
  padding: 8px 20px;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  background: none;
  font-size: 14px;
  color: #888;
  cursor: pointer;
}

.page-tab:hover {
  color: #333;
}

.page-tab.active {
  background: white;
  border-color: #eee;
  color: #1a56db;
  font-weight: 600;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 8px;
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

.app-footer {
  padding: 6px 16px;
  text-align: center;
  font-size: 11px;
  color: #999;
  border-top: 1px solid #eee;
  background: #f8f9fa;
}

.app-footer a {
  color: #999;
  text-decoration: underline;
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
