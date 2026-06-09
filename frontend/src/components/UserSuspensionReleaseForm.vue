<script setup>
import { ref, watch } from 'vue'

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

const releaseReason = ref('')

watch(
  () => props.username,
  () => {
    releaseReason.value = ''
  },
)

function submitForm() {
  emit('submit', {
    releaseReason: releaseReason.value.trim(),
  })
}
</script>

<template>
  <div class="modal-backdrop">
    <form class="release-dialog" @submit.prevent="submitForm">
      <div class="dialog-header">
        <div>
          <h3>정지 해제</h3>
          <p>@{{ username }}</p>
        </div>
        <button type="button" class="close-button" :disabled="isSubmitting" @click="emit('close')">
          닫기
        </button>
      </div>

      <label>
        <span>해제 사유</span>
        <textarea
          v-model="releaseReason"
          rows="4"
          maxlength="255"
          placeholder="해제 사유를 입력하세요"
          :disabled="isSubmitting"
        ></textarea>
      </label>

      <div class="dialog-actions">
        <button type="button" class="cancel-button" :disabled="isSubmitting" @click="emit('close')">
          취소
        </button>
        <button type="submit" class="submit-button" :disabled="isSubmitting">
          {{ isSubmitting ? '해제 처리 중...' : '해제하기' }}
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

.release-dialog {
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

textarea {
  width: 100%;
  padding: 10px 12px;
  color: #333;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

textarea:disabled {
  color: #999;
  background: #f8f9fa;
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
  background: #2e7d32;
  color: white;
  border: 1px solid #2e7d32;
}

button:disabled {
  color: #bbb;
  background: #f8f9fa;
  border-color: #eee;
  cursor: default;
}
</style>
