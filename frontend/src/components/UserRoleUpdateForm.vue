<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  username: {
    type: String,
    required: true,
  },
  currentRole: {
    type: String,
    default: 'USER',
  },
  isSubmitting: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['close', 'submit'])

const selectedRole = ref(props.currentRole || 'USER')

const canSubmit = computed(() => selectedRole.value && selectedRole.value !== props.currentRole)

watch(
  () => props.currentRole,
  (role) => {
    selectedRole.value = role || 'USER'
  },
)

function submitForm() {
  if (!canSubmit.value) return
  emit('submit', {
    role: selectedRole.value,
  })
}
</script>

<template>
  <div class="modal-backdrop">
    <form class="role-dialog" @submit.prevent="submitForm">
      <div class="dialog-header">
        <div>
          <h3>권한 변경</h3>
          <p>@{{ username }}</p>
        </div>
        <button type="button" class="close-button" :disabled="isSubmitting" @click="emit('close')">
          닫기
        </button>
      </div>

      <label>
        <span>권한</span>
        <select v-model="selectedRole" :disabled="isSubmitting">
          <option value="USER">USER</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </label>

      <div class="dialog-actions">
        <button type="button" class="cancel-button" :disabled="isSubmitting" @click="emit('close')">
          취소
        </button>
        <button type="submit" class="submit-button" :disabled="isSubmitting || !canSubmit">
          {{ isSubmitting ? '변경 중...' : '변경하기' }}
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

.role-dialog {
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

select {
  width: 100%;
  height: 38px;
  padding: 0 10px;
  color: #333;
  background: white;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

select:disabled {
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
  background: #4a90d9;
  color: white;
  border: 1px solid #4a90d9;
}

button:disabled {
  color: #bbb;
  background: #f8f9fa;
  border-color: #eee;
  cursor: default;
}
</style>
