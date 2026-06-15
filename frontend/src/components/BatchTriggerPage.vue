<script setup>
import { reactive } from 'vue'
import * as adminApi from '@/api/admin'

defineEmits(['back'])

// ── 배치 작업 목록 ──
// 새 배치가 생기면 여기에 { key, label, run, stop } 한 항목만 추가하면 버튼이 생긴다.
// run/stop 은 백엔드 트리거 API를 부르는 함수(@/api/admin).
const jobs = [
  {
    key: 'codeforces',
    label: '코드포스 문제 수집',
    description: '허깅페이스 open-r1/codeforces 데이터셋을 수집해 DB에 적재합니다.',
    run: adminApi.runCodeforcesIngest,
    stop: adminApi.stopCodeforcesIngest,
  },
]

// key -> { loading, message, ok }
const status = reactive({})

function run(job) {
  call(job, job.run, '실행', '실행 요청을 보냈습니다.')
}

function stop(job) {
  call(job, job.stop, '중지', '중지되었습니다.')
}

// actionLabel: 로딩 중 표시("중지 중…"/"실행 중…")에 쓰는 동작 이름
async function call(job, action, actionLabel, okMessage) {
  status[job.key] = { loading: true, actionLabel, message: '', ok: null }
  try {
    const result = await action()
    status[job.key] = {
      loading: false,
      actionLabel,
      message: result?.message || okMessage,
      ok: true,
    }
  } catch (error) {
    status[job.key] = {
      loading: false,
      actionLabel,
      message: `실패 (${error?.status ?? '오류'})`,
      ok: false,
    }
  }
}
</script>

<template>
  <div class="batch-trigger-page">
    <div class="header">
      <h2>배치 트리거</h2>
      <button class="back-btn" @click="$emit('back')">← 돌아가기</button>
    </div>
    <p class="desc">관리자가 수동으로 배치 작업을 실행합니다.</p>

    <ul class="job-list">
      <li v-for="job in jobs" :key="job.key" class="job-row">
        <div class="job-info">
          <span class="job-label">{{ job.label }}</span>
          <span class="job-desc">{{ job.description }}</span>
        </div>
        <div class="job-action">
          <button class="run-btn" :disabled="status[job.key]?.loading" @click="run(job)">실행</button>
          <button class="stop-btn" :disabled="status[job.key]?.loading" @click="stop(job)">중지</button>
          <span
            v-if="status[job.key]"
            :class="['job-status', status[job.key].loading ? 'loading' : (status[job.key].ok ? 'ok' : 'fail')]"
          >
            <span v-if="status[job.key].loading" class="spinner" aria-hidden="true"></span>
            {{ status[job.key].loading ? `${status[job.key].actionLabel} 중…` : status[job.key].message }}
          </span>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.batch-trigger-page {
  flex: 1;
  padding: 32px;
  background: #fff;
  overflow-y: auto;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.header h2 {
  margin: 0;
  font-size: 22px;
  color: #333;
}

.back-btn {
  background: none;
  border: none;
  color: #666;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  color: #1a56db;
}

.desc {
  color: #888;
  font-size: 14px;
  margin-bottom: 20px;
}

.job-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.job-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.job-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-label {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.job-desc {
  font-size: 13px;
  color: #999;
}

.job-action {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.run-btn {
  padding: 6px 18px;
  background: #1a56db;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.run-btn:disabled {
  background: #9db8e8;
  cursor: default;
}

.stop-btn {
  padding: 6px 18px;
  background: #fff;
  color: #d32f2f;
  border: 1px solid #d32f2f;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.stop-btn:disabled {
  color: #e3a9a9;
  border-color: #e3a9a9;
  cursor: default;
}

.job-status {
  font-size: 13px;
  display: inline-flex;
  align-items: center;
}

.job-status.ok {
  color: #2e7d32;
}

.job-status.fail {
  color: #d32f2f;
}

.job-status.loading {
  color: #666;
}

.spinner {
  display: inline-block;
  width: 11px;
  height: 11px;
  border: 2px solid #d0d0d0;
  border-top-color: #1a56db;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  margin-right: 6px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
