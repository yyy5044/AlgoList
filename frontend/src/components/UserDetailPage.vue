<script setup>
import { computed, onMounted, ref } from 'vue'
import UserSuspensionReleaseForm from './UserSuspensionReleaseForm.vue'
import UserSuspensionForm from './UserSuspensionForm.vue'

const props = defineProps({
  username: String,
  currentUsername: {
    type: String,
    default: '',
  },
  apiBasePath: {
    type: String,
    default: '/api/users',
  },
  showActions: {
    type: Boolean,
    default: true,
  },
  showAdminActions: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits([
  'back',
  'edit-profile',
  'delete-success',
  'suspend-success',
  'release-suspension-success',
])

const user = ref({
  username: props.username,
  nickname: '',
  profileImageUrl: '',
  bio: '',
  role: '',
  accountStatus: '',
})
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const isDeleting = ref(false)
const isReleasingSuspension = ref(false)
const isUpdatingRole = ref(false)
const isSuspending = ref(false)
const isReleaseFormOpen = ref(false)
const isSuspensionFormOpen = ref(false)
const imageLoadFailed = ref(false)
const selectedRole = ref('')

const displayName = computed(() => user.value.nickname || user.value.username || '사용자')
const profileInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const subtitle = computed(() =>
  props.showAdminActions ? '회원 정보를 확인하고 관리할 수 있습니다.' : '내 계정 정보를 확인할 수 있습니다.',
)
const canSuspend = computed(() => {
  return (
    props.showAdminActions &&
    !isLoading.value &&
    user.value.role !== 'ADMIN' &&
    user.value.accountStatus !== 'SUSPENDED' &&
    user.value.accountStatus !== 'DELETED'
  )
})
const canReleaseSuspension = computed(() => {
  return props.showAdminActions && !isLoading.value && user.value.accountStatus === 'SUSPENDED'
})
const canUpdateRole = computed(() => {
  return (
    props.showAdminActions &&
    !isLoading.value &&
    !isUpdatingRole.value &&
    user.value.username !== props.currentUsername &&
    user.value.accountStatus !== 'DELETED' &&
    selectedRole.value &&
    selectedRole.value !== user.value.role
  )
})
const roleUpdateButtonText = computed(() => {
  if (isLoading.value) return '회원 정보 확인 중'
  if (user.value.username === props.currentUsername) return '내 권한 변경 불가'
  if (user.value.accountStatus === 'DELETED') return '삭제된 회원'
  if (selectedRole.value === user.value.role) return '현재 권한'
  return '권한 변경'
})
const suspendButtonText = computed(() => {
  if (isLoading.value) return '회원 정보 확인 중'
  if (user.value.role === 'ADMIN') return '관리자는 정지할 수 없음'
  if (user.value.accountStatus === 'SUSPENDED') return '이미 정지된 회원'
  if (user.value.accountStatus === 'DELETED') return '삭제된 회원'
  return '회원 정지'
})
const releaseButtonText = computed(() => {
  if (isLoading.value) return '회원 정보 확인 중'
  return '정지 해제'
})

async function loadUser() {
  if (!props.username) return

  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await fetch(`${props.apiBasePath}/${encodeURIComponent(props.username)}`, {
      credentials: 'include',
    })

    if (response.ok) {
      user.value = await response.json()
      selectedRole.value = user.value.role || 'USER'
      imageLoadFailed.value = false
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
    const response = await fetch(`${props.apiBasePath}/${encodeURIComponent(props.username)}`, {
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

async function suspendUser(payload) {
  try {
    isSuspending.value = true
    errorMessage.value = ''
    successMessage.value = ''
    const response = await fetch(`${props.apiBasePath}/${encodeURIComponent(props.username)}/suspensions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(payload),
    })

    if (response.ok) {
      successMessage.value = '회원 정지가 완료되었습니다.'
      isSuspensionFormOpen.value = false
      await loadUser()
      emit('suspend-success')
    } else {
      let message = '회원 정지에 실패했습니다.'
      try {
        const data = await response.json()
        message = data.message || message
      } catch (error) {
        console.log(error)
      }
      errorMessage.value = message
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isSuspending.value = false
  }
}

async function releaseUserSuspension(payload) {
  try {
    isReleasingSuspension.value = true
    errorMessage.value = ''
    successMessage.value = ''
    const response = await fetch(
      `${props.apiBasePath}/${encodeURIComponent(props.username)}/suspensions/release`,
      {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(payload),
      },
    )

    if (response.ok) {
      successMessage.value = '정지 해제가 완료되었습니다.'
      isReleaseFormOpen.value = false
      await loadUser()
      emit('release-suspension-success')
    } else {
      let message = '정지 해제에 실패했습니다.'
      try {
        const data = await response.json()
        message = data.message || message
      } catch (error) {
        console.log(error)
      }
      errorMessage.value = message
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isReleasingSuspension.value = false
  }
}

async function updateUserRole() {
  try {
    isUpdatingRole.value = true
    errorMessage.value = ''
    successMessage.value = ''
    const response = await fetch(`${props.apiBasePath}/${encodeURIComponent(props.username)}/role`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ role: selectedRole.value }),
    })

    if (response.ok) {
      successMessage.value = '권한 변경이 완료되었습니다.'
      await loadUser()
    } else {
      let message = '권한 변경에 실패했습니다.'
      try {
        const data = await response.json()
        message = data.message || message
      } catch (error) {
        console.log(error)
      }
      errorMessage.value = message
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = '서버에 연결할 수 없습니다.'
  } finally {
    isUpdatingRole.value = false
  }
}
</script>

<template>
  <div class="user-page">
    <div class="user-panel">
      <h2>회원 상세</h2>
      <p class="user-subtitle">{{ subtitle }}</p>

      <p v-if="isLoading" class="guide-message">회원 정보를 불러오는 중입니다.</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

      <div class="profile-summary">
        <img
          v-if="user.profileImageUrl && !imageLoadFailed"
          class="profile-image"
          :src="user.profileImageUrl"
          :alt="`${displayName} 프로필 이미지`"
          @error="imageLoadFailed = true"
        />
        <div v-else class="profile-placeholder">{{ profileInitial }}</div>
        <div class="profile-name-wrap">
          <strong class="profile-name">{{ displayName }}</strong>
          <span class="profile-username">@{{ user.username }}</span>
        </div>
      </div>

      <div class="info-list">
        <div class="info-row">
          <span class="info-label">권한</span>
          <span class="role-badge">{{ user.role || 'USER' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">계정 상태</span>
          <span class="status-badge">{{ user.accountStatus || 'ACTIVE' }}</span>
        </div>
      </div>

      <div class="bio-section">
        <span class="bio-label">자기소개</span>
        <p class="bio-text">{{ user.bio || '등록된 자기소개가 없습니다.' }}</p>
      </div>

      <template v-if="showActions">
        <button @click="emit('edit-profile')">수정</button>
        <button class="delete-button" @click="deleteUser" :disabled="isDeleting">
          {{ isDeleting ? '탈퇴 중...' : '회원 탈퇴' }}
        </button>
      </template>
      <template v-if="showAdminActions">
        <div class="admin-role-control">
          <label>
            <span>권한 변경</span>
            <select v-model="selectedRole" :disabled="isLoading || isUpdatingRole">
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </label>
          <button
            class="role-update-button"
            :disabled="!canUpdateRole"
            @click="updateUserRole"
          >
            {{ isUpdatingRole ? '변경 중...' : roleUpdateButtonText }}
          </button>
        </div>
        <button
          v-if="user.accountStatus === 'SUSPENDED'"
          class="release-button"
          :disabled="!canReleaseSuspension || isReleasingSuspension"
          @click="isReleaseFormOpen = true"
        >
          {{ isReleasingSuspension ? '해제 처리 중...' : releaseButtonText }}
        </button>
        <button
          v-else
          class="suspend-button"
          :disabled="!canSuspend || isSuspending"
          @click="isSuspensionFormOpen = true"
        >
          {{ isSuspending ? '정지 처리 중...' : suspendButtonText }}
        </button>
      </template>
      <button class="back-button" @click="emit('back')">뒤로가기</button>
    </div>

    <UserSuspensionForm
      v-if="isSuspensionFormOpen"
      :username="user.username"
      :is-submitting="isSuspending"
      @close="isSuspensionFormOpen = false"
      @submit="suspendUser"
    />

    <UserSuspensionReleaseForm
      v-if="isReleaseFormOpen"
      :username="user.username"
      :is-submitting="isReleasingSuspension"
      @close="isReleaseFormOpen = false"
      @submit="releaseUserSuspension"
    />
  </div>
</template>

<style scoped>
.user-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  flex: 1;
  min-height: 0;
  padding: 32px;
  overflow: auto;
  background: #f5f5f5;
}

.user-panel {
  background: white;
  margin: auto 0;
  padding: 36px;
  border-radius: 12px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1);
  width: min(100%, 520px);
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

.profile-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0 24px;
}

.profile-image,
.profile-placeholder {
  width: 112px;
  height: 112px;
  border-radius: 50%;
}

.profile-image {
  object-fit: cover;
  border: 3px solid #eef2f7;
}

.profile-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef5fc;
  color: #2f6fab;
  border: 3px solid #dbeafe;
  font-size: 42px;
  font-weight: 700;
}

.profile-name-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 14px;
}

.profile-name {
  color: #222;
  font-size: 22px;
  line-height: 1.2;
}

.profile-username {
  color: #888;
  font-size: 13px;
}

.info-list {
  border: 1px solid #eee;
  border-radius: 8px;
  margin-bottom: 14px;
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

.role-badge {
  padding: 4px 10px;
  background: #f2f7fd;
  color: #2f6fab;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.status-badge {
  padding: 4px 10px;
  background: #f8f9fa;
  color: #555;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.bio-section {
  padding: 18px;
  margin-bottom: 18px;
  min-height: 128px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
  text-align: left;
}

.bio-label {
  display: block;
  margin-bottom: 10px;
  color: #888;
  font-size: 13px;
}

.bio-text {
  color: #333;
  font-size: 15px;
  white-space: pre-wrap;
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

.suspend-button {
  margin-top: 10px;
  background: #e74c3c;
}

.suspend-button:hover {
  background: #d43f30;
}

.admin-role-control {
  padding: 14px;
  margin-bottom: 10px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
  text-align: left;
}

.admin-role-control label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
}

.admin-role-control span {
  color: #888;
  font-size: 13px;
}

.admin-role-control select {
  width: 100%;
  height: 38px;
  padding: 0 10px;
  color: #333;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.admin-role-control select:disabled {
  color: #999;
  background: #f8f9fa;
}

.role-update-button {
  background: #4a90d9;
}

.role-update-button:hover {
  background: #3a7bc8;
}

.release-button {
  margin-top: 10px;
  background: #2e7d32;
}

.release-button:hover {
  background: #256628;
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
