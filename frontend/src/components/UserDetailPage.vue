<script setup>
import { computed, onMounted, ref } from 'vue'

const props = defineProps({
  username: String,
  apiBasePath: {
    type: String,
    default: '/api/users',
  },
  showActions: {
    type: Boolean,
    default: true,
  },
})

const emit = defineEmits(['back', 'edit-profile', 'delete-success'])

const user = ref({
  username: props.username,
  nickname: '',
  profileImageUrl: '',
  bio: '',
  role: '',
})
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const isDeleting = ref(false)
const imageLoadFailed = ref(false)

const displayName = computed(() => user.value.nickname || user.value.username || '사용자')
const profileInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())

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
</script>

<template>
  <div class="user-page">
    <div class="user-panel">
      <h2>회원 상세</h2>
      <p class="user-subtitle">내 계정 정보를 확인할 수 있습니다.</p>

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
      <button class="back-button" @click="emit('back')">뒤로가기</button>
    </div>
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
