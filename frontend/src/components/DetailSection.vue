<script setup>
import { computed, ref, watch } from 'vue'
import { renderDescription } from '@/utils/renderDescription'
import * as problemApi from '@/api/problems'
import SolutionManager from './SolutionManager.vue'

const props = defineProps({
  selectedItem: Object,
  username: String
})

// ── 번역 상태 ──
const translation = ref(null)      // 서버에서 받아온 번역 결과 (한 번 받으면 캐시)
const showTranslation = ref(false) // 번역문/원문 토글
const isTranslating = ref(false)   // 번역 요청 진행 중
const translateError = ref('')

// 코드포스 문제만 영어라서 번역 버튼을 노출한다
const isCodeforces = computed(() => props.selectedItem?.problem?.site === 'CODEFORCES')

// 다른 문제를 선택하면 번역 상태를 초기화한다
watch(() => props.selectedItem?.problem?.problemId, () => {
  translation.value = null
  showTranslation.value = false
  isTranslating.value = false
  translateError.value = ''
})

async function toggleTranslation() {
  // 이미 받아온 번역이 있으면 재요청 없이 토글만 한다
  if (translation.value) {
    showTranslation.value = !showTranslation.value
    return
  }
  isTranslating.value = true
  translateError.value = ''
  try {
    translation.value = await problemApi.translate(props.selectedItem.problem.problemId)
    showTranslation.value = true
  } catch (error) {
    console.error('번역 실패:', error)
    translateError.value = '번역에 실패했습니다. 잠시 후 다시 시도해주세요.'
  } finally {
    isTranslating.value = false
  }
}

const buttonLabel = computed(() => {
  if (isTranslating.value) return '번역 중...'
  if (showTranslation.value) return '원문 보기'
  return 'AI 번역'
})

// 번역 토글 상태에 따라 제목을 전환 (번호는 항상 원문 유지)
const displayTitle = computed(() =>
  showTranslation.value && translation.value
    ? translation.value.translatedTitle
    : props.selectedItem?.problem?.title
)

// site 에 따라 본문을 표시용 HTML로 변환 (BOJ / CODEFORCES 경로 분기)
// 번역문은 코드포스 형식(마크다운 + $$$LaTeX$$$)을 유지하므로 CODEFORCES 경로로 렌더한다
const descriptionHtml = computed(() => {
  if (showTranslation.value && translation.value) {
    return renderDescription(translation.value.translatedDescription, 'CODEFORCES')
  }
  return renderDescription(props.selectedItem?.problem?.description, props.selectedItem?.problem?.site)
})
</script>

<template>
  <div class="detail-section">
    <div v-if="selectedItem" class="detail-content">
      <!-- 상단: 사이트 아이콘 + 문제 번호 + 제목 + AI 번역 버튼 -->
      <div class="detail-header">
        <img :src="`/icons/${selectedItem.problem.site}.png`" :alt="selectedItem.problem.site" class="detail-site-icon" />
        <a :href="selectedItem.problem.link" target="_blank" class="detail-title-link">
          <h2>[{{ selectedItem.problem.number }}] {{ displayTitle }}</h2>
        </a>
        <button
          v-if="isCodeforces"
          class="translate-btn"
          :disabled="isTranslating"
          @click="toggleTranslation"
        >
          <span v-if="isTranslating" class="spinner"></span>
          <span v-else>{{ showTranslation ? '↩' : '✨' }}</span>
          {{ buttonLabel }}
        </button>
      </div>

      <p v-if="translateError" class="translate-error">{{ translateError }}</p>

      <!-- 문제 정보 -->
      <div class="info-group">
        <div class="info-row">
          <span class="info-label">난이도</span>
          <span class="info-value">{{ selectedItem.problem.difficulty }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">사이트</span>
          <span class="info-value">{{ selectedItem.problem.site }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">알고리즘 분류</span>
          <div class="category-tags">
            <span v-for="cat in selectedItem.problem.category" :key="cat" class="category-tag">
              {{ cat }}
            </span>
          </div>
        </div>
      </div>

      <!-- 내부 관리 데이터 -->
      <div class="info-group">
        <div class="info-row">
          <span class="info-label">문제 등급</span>
          <span v-if="selectedItem.grade" :class="['grade-badge', selectedItem.grade.toLowerCase()]">
            {{ selectedItem.grade }}
          </span>
          <span v-else class="grade-badge">미지정</span>
        </div>
        <div class="info-row">
          <span class="info-label">푼 횟수</span>
          <span class="info-value">{{ selectedItem.solveCount }}회</span>
        </div>
        <div class="info-row">
          <span class="info-label">최근 푼 날짜</span>
          <span class="info-value">{{ selectedItem.lastSolvedDate }}</span>
        </div>
      </div>
      <!-- 문제 본문 -->
      <div v-if="selectedItem.problem.description" class="description-section">
        <span v-if="showTranslation" class="translated-badge">AI 번역됨</span>
        <div class="description-content" v-html="descriptionHtml"></div>
      </div>
    </div>
    <div v-else class="detail-placeholder">
      <p>오른쪽 리스트에서 항목을 선택해주세요.</p>
    </div>
    <!--SolutionManager 컴포넌트-->
    <SolutionManager :selected-item="selectedItem" :username="username" />
  </div>
</template>

<style scoped>
.detail-title-link {
  text-decoration: none;
  color: inherit;
}

.detail-title-link:hover h2 {
  color: #1a56db;
}

.detail-section {
  flex: 1;
  padding: 32px;
  background-color: #ffffff;
  overflow-y: auto;
}

.detail-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 16px;
}

/* 상단 헤더 */
.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.detail-site-icon {
  width: 32px;
  height: 32px;
}

.detail-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

/* AI 번역 버튼 (제목 줄 오른쪽 끝) */
.translate-btn {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  background: linear-gradient(135deg, #1a56db, #4f7ff5);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.15s, transform 0.05s;
}

.translate-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.translate-btn:active:not(:disabled) {
  transform: translateY(1px);
}

.translate-btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.translate-error {
  color: #d32f2f;
  font-size: 13px;
  margin-bottom: 16px;
}

.translated-badge {
  margin-left: 8px;
  font-size: 11px;
  font-weight: 600;
  color: #1a56db;
  background-color: #e8f0fe;
  padding: 2px 8px;
  border-radius: 10px;
  vertical-align: middle;
}

/* 정보 그룹 */
.info-group {
  border-top: 1px solid #eee;
  padding: 20px 0;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 8px 0;
}

.info-label {
  width: 120px;
  font-size: 14px;
  color: #888;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #333;
}

/* 알고리즘 분류 태그 */
.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-tag {
  font-size: 12px;
  color: #1a56db;
  background-color: #e8f0fe;
  padding: 3px 10px;
  border-radius: 12px;
}

/* 등급 뱃지 */
.grade-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 12px;
}

.grade-badge.red {
  color: #d32f2f;
  background-color: #fde8e8;
}

.grade-badge.yellow {
  color: #f9a825;
  background-color: #fff8e1;
}

.grade-badge.green {
  color: #2e7d32;
  background-color: #e8f5e9;
}

/* 문제 본문 */
.description-section {
  border-top: 1px solid #eee;
  padding: 20px 0;
}

.description-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #333;
}

.description-content {
  font-size: 14px;
  line-height: 1.8;
  color: #444;
  word-break: break-word;
}

.description-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 8px 0;
}

.description-content :deep(p) {
  margin: 8px 0;
}

.description-content :deep(ol),
.description-content :deep(ul) {
  padding-left: 2em;
  margin: 4px 0;
}

.description-content :deep(li) {
  margin: 2px 0;
}

.description-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 13px;
}

.description-content :deep(table) {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
}

.description-content :deep(th),
.description-content :deep(td) {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
  font-size: 13px;
}

.description-content :deep(th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

/* Examples — Codeforces 스타일 */
.description-content :deep(.example-box) {
  border: 1px solid #ddd;
  border-radius: 6px;
  margin-bottom: 16px;
  overflow: hidden;
}

.description-content :deep(.example-header) {
  background-color: #f0f0f0;
  padding: 6px 12px;
  font-weight: 600;
  font-size: 13px;
  border-bottom: 1px solid #ddd;
}

.description-content :deep(.example-data) {
  margin: 0;
  padding: 10px 12px;
  background-color: #fff;
  border-radius: 0;
  border-bottom: 1px solid #ddd;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre;
}

.description-content :deep(.example-box .example-data:last-child) {
  border-bottom: none;
}
</style>
