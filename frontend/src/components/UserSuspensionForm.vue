<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  username: {
    type: String,
    required: true,
  },
  isSubmitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close', 'submit'])

const reason = ref('')
const suspendedUntil = ref('')
const validationMessage = ref('')

const minSuspendedUntil = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 10)
})

watch(
  () => props.username,
  () => {
    reason.value = ''
    suspendedUntil.value = ''
    validationMessage.value = ''
  },
)

function submitForm() {
  validationMessage.value = ''

  if (!suspendedUntil.value) {
    validationMessage.value = '정지 종료일을 입력해주세요.'
    return
  }

  if (suspendedUntil.value < minSuspendedUntil.value) {
    validationMessage.value = '정지 종료일은 오늘 또는 이후 날짜여야 합니다.'
    return
  }

  emit('submit', {
    reason: reason.value.trim(),
    suspendedUntil: suspendedUntil.value,
  })
}
</script>

<template>
  <div class="modal-backdrop">
    <form class="suspension-dialog" @submit.prevent="submitForm">
      <div class="dialog-header">
        <div>
          <h3>회원 정지</h3>
          <p>@{{ username }}</p>
        </div>
        <button type="button" class="close-button" :disabled="isSubmitting" @click="emit('close')">
          닫기
        </button>
      </div>

      <label>
        <span>정지 종료일</span>
        <input
          v-model="suspendedUntil"
          type="date"
          :min="minSuspendedUntil"
          :disabled="isSubmitting"
        />
      </label>

      <label>
        <span>정지 사유</span>
        <textarea
          v-model="reason"
          rows="4"
          maxlength="255"
          placeholder="정지 사유를 입력하세요"
          :disabled="isSubmitting"
        ></textarea>
      </label>

      <p v-if="validationMessage" class="validation-message">{{ validationMessage }}</p>

      <div class="dialog-actions">
        <button type="button" class="cancel-button" :disabled="isSubmitting" @click="emit('close')">
          취소
        </button>
        <button type="submit" class="submit-button" :disabled="isSubmitting">
          {{ isSubmitting ? '정지 처리 중...' : '정지하기' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(17, 24, 39, 0.45);
}

.suspension-dialog {
  width: min(100%, 420px);
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.22);
  text-align: left;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-header h3 {
  margin-bottom: 4px;
  color: #333;
  font-size: 20px;
}

.dialog-header p {
  color: #777;
  font-size: 13px;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 14px;
}

label span {
  color: #666;
  font-size: 13px;
  font-weight: 600;
}

input,
textarea {
  width: 100%;
  padding: 10px 12px;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
}

textarea {
  resize: vertical;
}

input:disabled,
textarea:disabled {
  color: #999;
  background: #f8f9fa;
}

.validation-message {
  margin-bottom: 12px;
  color: #e74c3c;
  font-size: 13px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

button {
  height: 38px;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}

.close-button {
  height: 32px;
  padding: 0 10px;
  background: white;
  color: #777;
  border: 1px solid #ddd;
}

.cancel-button {
  background: white;
  color: #777;
  border: 1px solid #ddd;
}

.submit-button {
  background: #e74c3c;
  color: white;
  border: 1px solid #e74c3c;
}

button:disabled {
  color: #bbb;
  background: #f8f9fa;
  border-color: #eee;
  cursor: default;
}
</style>
