<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { NETWORK_ERROR_MESSAGE, readErrorMessage } from '../utils/apiError'

const emit = defineEmits(['back-to-login', 'signup-success'])

const form = ref({
  username: '',
  nickname: '',
  password: '',
  passwordConfirm: '',
})
const profile = ref({
  image: null,
  previewUrl: '',
  imageLoadFailed: false,
})
const errorMessage = ref('')
const isSubmitting = ref(false)
const fileInput = ref(null)

const displayName = computed(() => form.value.nickname || form.value.username || '사용자')
const profileInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const profilePreview = computed(() => profile.value.previewUrl)

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

async function signup() {
  errorMessage.value = ''

  if (!form.value.username.trim() || !form.value.password.trim() || !form.value.passwordConfirm.trim()) {
    errorMessage.value = '아이디와 비밀번호를 모두 입력해주세요.'
    return
  }

  if (form.value.password !== form.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  if (!isValidPassword(form.value.password.trim())) {
    errorMessage.value = '비밀번호는 8자 이상이며 영어와 숫자를 모두 포함해야 합니다.'
    return
  }

  try {
    isSubmitting.value = true
    const formData = new FormData()
    formData.append('username', form.value.username)
    formData.append('password', form.value.password)
    formData.append('nickname', form.value.nickname)

    if (profile.value.image) {
      formData.append('profileImage', profile.value.image)
    }

    const response = await fetch('/api/users', {
      method: 'POST',
      credentials: 'include',
      body: formData,
    })

    if (response.ok) {
      emit('signup-success', '회원가입이 완료되었습니다. 로그인해주세요.')
    } else {
      errorMessage.value = await readErrorMessage(response, '회원가입에 실패했습니다.')
    }
  } catch (error) {
    console.log(error)
    errorMessage.value = NETWORK_ERROR_MESSAGE
  } finally {
    isSubmitting.value = false
  }
}

function isValidPassword(password) {
  return password.length >= 8 && /[A-Za-z]/.test(password) && /\d/.test(password)
}
</script>

<template>
  <div class="signup-container">
    <div class="signup-box">
      <h1>AlgoList</h1>
      <p class="signup-subtitle">알고리즘 문제 관리 서비스</p>

      <div class="profile-summary">
        <img
          v-if="profilePreview && !profile.imageLoadFailed"
          class="profile-image"
          :src="profilePreview"
          :alt="`${displayName} 프로필 이미지`"
          @error="profile.imageLoadFailed = true"
        />
        <div v-else class="profile-placeholder">{{ profileInitial }}</div>
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
        <label class="form-label" for="username">
          아이디 <span class="required-mark">*</span>
        </label>
        <input id="username" v-model="form.username" placeholder="아이디" @keyup.enter="signup" />
      </div>

      <div class="form-section">
        <label class="form-label" for="password">
          비밀번호 <span class="required-mark">*</span>
        </label>
        <input
          id="password"
          v-model="form.password"
          type="password"
          placeholder="비밀번호"
          @keyup.enter="signup"
        />
      </div>

      <div class="form-section">
        <label class="form-label" for="passwordConfirm">
          비밀번호 확인 <span class="required-mark">*</span>
        </label>
        <input
          id="passwordConfirm"
          v-model="form.passwordConfirm"
          type="password"
          placeholder="비밀번호 확인"
          @keyup.enter="signup"
        />
      </div>

      <div class="form-section">
        <label class="form-label" for="nickname">닉네임</label>
        <input id="nickname" v-model="form.nickname" placeholder="닉네임" @keyup.enter="signup" />
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
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
  min-height: 100vh;
  padding: 24px;
  background: #f5f5f5;
  box-sizing: border-box;
}

.signup-box {
  background: white;
  padding: 36px;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: min(100%, 400px);
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

.profile-summary {
  display: flex;
  justify-content: center;
  padding: 4px 0 20px;
}

.profile-image,
.profile-placeholder {
  width: 96px;
  height: 96px;
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
  font-size: 36px;
  font-weight: 700;
}

.form-section {
  margin-bottom: 12px;
  text-align: left;
}

.form-label {
  display: block;
  margin-bottom: 7px;
  color: #666;
  font-size: 13px;
  font-weight: 600;
}

.required-mark {
  color: #e74c3c;
}

input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #333;
  font-size: 14px;
  box-sizing: border-box;
}

.file-input {
  padding: 9px 12px;
  background: #fafafa;
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
