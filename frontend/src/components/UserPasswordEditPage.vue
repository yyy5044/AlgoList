<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  username: String,
})

const emit = defineEmits(['back'])

const form = ref({
  nickname: '',
  bio: '',
  password: '',
  passwordConfirm: '',
})
const profile = ref({
  image: null,
  imageUrl: '',
  previewUrl: '',
  imageLoadFailed: false,
})
const errorMessage = ref('')
const successMessage = ref('')
const isLoading = ref(false)
const isSubmitting = ref(false)
const fileInput = ref(null)

const displayName = computed(() => form.value.nickname || props.username || '사용자')
const profileInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const profilePreview = computed(() => profile.value.previewUrl || profile.value.imageUrl)

async function loadUser() {
  if (!props.username) return

  try {
    isLoading.value = true
    errorMessage.value = ''
    const response = await fetch(`/api/users/${encodeURIComponent(props.username)}`, {
      credentials: 'include',
    })

    if (response.ok) {
      const user = await response.json()
      form.value.nickname = user.nickname || ''
      form.value.bio = user.bio || ''
      profile.value.imageUrl = user.profileImageUrl || ''
      profile.value.imageLoadFailed = false
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

onBeforeUnmount(() => {
  revokePreviewUrl()
})

function revokePreviewUrl() {
  if (profile.value.previewUrl) {
    URL.revokeObjectURL(profile.value.previewUrl)
    profile.value.previewUrl = ''
  }
}

function onProfileImageChange(event) {
  const file = event.target.files?.[0]
  revokePreviewUrl()
  profile.value.image = file || null
  profile.value.imageLoadFailed = false

  if (file) {
    profile.value.previewUrl = URL.createObjectURL(file)
  }
}

function clearProfileImageSelection() {
  profile.value.image = null
  revokePreviewUrl()
  profile.value.imageLoadFailed = false

  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

async function readErrorMessage(response, fallbackMessage) {
  try {
    const data = await response.json()
    return data.message || fallbackMessage
  } catch {
    return fallbackMessage
  }
}

async function updateUser() {
  errorMessage.value = ''
  successMessage.value = ''

  const hasPassword = form.value.password.trim() || form.value.passwordConfirm.trim()
  if (hasPassword && form.value.password !== form.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  try {
    isSubmitting.value = true
    const formData = new FormData()
    formData.append('nickname', form.value.nickname)
    formData.append('bio', form.value.bio)

    if (form.value.password.trim()) {
      formData.append('password', form.value.password)
    }

    if (profile.value.image) {
      formData.append('profileImage', profile.value.image)
    }

    const response = await fetch(`/api/users/${encodeURIComponent(props.username)}`, {
      method: 'PUT',
      credentials: 'include',
      body: formData,
    })

    if (response.ok) {
      successMessage.value = '프로필이 수정되었습니다.'
      form.value.password = ''
      form.value.passwordConfirm = ''
      clearProfileImageSelection()
      await loadUser()
    } else {
      errorMessage.value = await readErrorMessage(response, '프로필 수정에 실패했습니다.')
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
      <h2>프로필 수정</h2>
      <p class="edit-subtitle">프로필과 계정 정보를 수정합니다.</p>

      <p v-if="isLoading" class="guide-message">회원 정보를 불러오는 중입니다.</p>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <p v-if="successMessage" class="success">{{ successMessage }}</p>

      <div class="profile-summary">
        <img
          v-if="profilePreview && !profile.imageLoadFailed"
          class="profile-image"
          :src="profilePreview"
          :alt="`${displayName} 프로필 이미지`"
          @error="profile.imageLoadFailed = true"
        />
        <div v-else class="profile-placeholder">{{ profileInitial }}</div>
        <strong class="profile-name">{{ displayName }}</strong>
        <span class="profile-username">@{{ username }}</span>
      </div>

      <div class="form-section">
        <label class="form-label" for="profileImage">프로필 사진</label>
        <input
          id="profileImage"
          ref="fileInput"
          class="file-input"
          type="file"
          accept="image/*"
          @change="onProfileImageChange"
        />
        <button
          v-if="profile.image"
          class="subtle-button"
          type="button"
          @click="clearProfileImageSelection"
        >
          선택 취소
        </button>
      </div>

      <div class="form-section">
        <label class="form-label" for="nickname">닉네임</label>
        <input id="nickname" v-model="form.nickname" placeholder="닉네임" />
      </div>

      <div class="form-grid">
        <div class="form-section">
          <label class="form-label" for="password">새 비밀번호</label>
          <input id="password" v-model="form.password" type="password" placeholder="변경할 때만 입력" />
        </div>
        <div class="form-section">
          <label class="form-label" for="passwordConfirm">비밀번호 확인</label>
          <input
            id="passwordConfirm"
            v-model="form.passwordConfirm"
            type="password"
            placeholder="새 비밀번호 확인"
            @keyup.enter="updateUser"
          />
        </div>
      </div>

      <div class="form-section">
        <label class="form-label" for="bio">자기소개</label>
        <textarea id="bio" v-model="form.bio" rows="5" placeholder="자기소개를 입력해주세요."></textarea>
      </div>

      <button @click="updateUser" :disabled="isSubmitting || isLoading">
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
  align-items: flex-start;
  flex: 1;
  min-height: 0;
  padding: 32px;
  overflow: auto;
  background: #f5f5f5;
}

.edit-panel {
  width: min(100%, 560px);
  margin: auto 0;
  padding: 36px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1);
  text-align: center;
}

.edit-panel h2 {
  margin-bottom: 4px;
  color: #333;
}

.edit-subtitle {
  color: #999;
  font-size: 14px;
  margin-bottom: 20px;
}

.guide-message,
.error,
.success {
  font-size: 13px;
  margin-bottom: 12px;
}

.guide-message {
  color: #999;
}

.error {
  color: #e74c3c;
}

.success {
  color: #2e7d32;
}

.profile-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 24px;
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

.profile-name {
  margin-top: 14px;
  color: #222;
  font-size: 22px;
  line-height: 1.2;
}

.profile-username {
  margin-top: 4px;
  color: #888;
  font-size: 13px;
}

.form-section {
  margin-bottom: 14px;
  text-align: left;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-label {
  display: block;
  margin-bottom: 7px;
  color: #666;
  font-size: 13px;
  font-weight: 600;
}

input,
textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #333;
  font-size: 14px;
  box-sizing: border-box;
}

textarea {
  min-height: 124px;
  resize: vertical;
}

.file-input {
  padding: 9px 12px;
  background: #fafafa;
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

.subtle-button {
  margin-top: 8px;
  background: white;
  color: #777;
  border: 1px solid #ddd;
  font-size: 13px;
}

.subtle-button:hover {
  background: #f8f9fa;
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

@media (max-width: 640px) {
  .edit-page {
    padding: 18px;
  }

  .edit-panel {
    padding: 28px 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
