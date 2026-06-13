<script setup>
import { computed, ref, watch } from 'vue'
import { renderDescription } from '@/utils/renderDescription'
import { NETWORK_ERROR_MESSAGE, readErrorMessage } from '@/utils/apiError'
import SolutionManager from './SolutionManager.vue'

const props = defineProps({
  selectedItem: Object,
  username: String
})

const emit = defineEmits(['user-problem-updated'])

// 사이트 무관하게 본문을 표시용 HTML로 변환
const descriptionHtml = computed(() =>
  renderDescription(props.selectedItem?.problem?.description)
)

const gradeOptions = ['RED', 'YELLOW', 'GREEN']
const isGradeMenuOpen = ref(false)
const isUpdatingGrade = ref(false)
const gradeErrorMessage = ref('')

watch(() => props.selectedItem?.userProblemId, () => {
  isGradeMenuOpen.value = false
  gradeErrorMessage.value = ''
})

function getGradeClass(grade) {
  return grade ? grade.toLowerCase() : ''
}

function toggleGradeMenu() {
  if (!props.selectedItem?.userProblemId || isUpdatingGrade.value) return
  gradeErrorMessage.value = ''
  isGradeMenuOpen.value = !isGradeMenuOpen.value
}

async function updateGrade(grade) {
  if (!props.selectedItem?.userProblemId) return

  if (props.selectedItem.grade === grade) {
    isGradeMenuOpen.value = false
    return
  }

  try {
    isUpdatingGrade.value = true
    gradeErrorMessage.value = ''
    const response = await fetch(`/api/reminders/${props.selectedItem.userProblemId}/grade`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ grade }),
      credentials: 'include'
    })

    if (!response.ok) {
      gradeErrorMessage.value = await readErrorMessage(response, '문제 등급을 변경할 수 없습니다.')
      return
    }

    const updatedUserProblem = await response.json()
    emit('user-problem-updated', updatedUserProblem)
    isGradeMenuOpen.value = false
  } catch (error) {
    console.error('문제 등급 변경 실패:', error)
    gradeErrorMessage.value = NETWORK_ERROR_MESSAGE
  } finally {
    isUpdatingGrade.value = false
  }
}
</script>

<template>
  <div class="detail-section">
    <div v-if="selectedItem" class="detail-content">
      <!-- 상단: 사이트 아이콘 + 문제 번호 + 제목 -->
      <div class="detail-header">
        <img :src="`/icons/${selectedItem.problem.site}.png`" :alt="selectedItem.problem.site" class="detail-site-icon" />
        <a :href="selectedItem.problem.link" target="_blank" class="detail-title-link">
          <h2>[{{ selectedItem.problem.number }}] {{ selectedItem.problem.title }}</h2>
        </a>
      </div>

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
          <div class="grade-wrapper">
            <button type="button" class="grade-button" @click="toggleGradeMenu">
              <span v-if="selectedItem.grade" :class="['grade-badge', getGradeClass(selectedItem.grade)]">
                {{ selectedItem.grade }}
              </span>
              <span v-else class="grade-badge">미지정</span>
            </button>
            <div v-if="isGradeMenuOpen" class="grade-menu">
              <button
                v-for="grade in gradeOptions"
                :key="grade"
                type="button"
                :class="['grade-menu-item', { active: selectedItem.grade === grade }]"
                :disabled="isUpdatingGrade"
                @click="updateGrade(grade)"
              >
                {{ grade }}
              </button>
            </div>
            <p v-if="gradeErrorMessage" class="grade-error">{{ gradeErrorMessage }}</p>
          </div>
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
        <h3 class="description-title">문제 본문</h3>
        <div class="description-content" v-html="descriptionHtml"></div>
      </div>
    </div>
    <div v-else class="detail-placeholder">
      <p>오른쪽 리스트에서 항목을 선택해주세요.</p>
    </div>
    <!--SolutionManager 컴포넌트-->
    <SolutionManager
      :selected-item="selectedItem"
      :username="username"
      @user-problem-updated="emit('user-problem-updated', $event)"
    />
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
.grade-wrapper {
  position: relative;
}

.grade-button {
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
}

.grade-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
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

.grade-menu {
  position: absolute;
  top: 28px;
  left: 0;
  min-width: 112px;
  padding: 4px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  z-index: 20;
}

.grade-menu-item {
  display: block;
  width: 100%;
  padding: 7px 10px;
  border: none;
  border-radius: 6px;
  background: none;
  color: #333;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
}

.grade-menu-item:hover,
.grade-menu-item.active {
  background-color: #eef4ff;
  color: #1a56db;
  font-weight: 600;
}

.grade-menu-item:disabled {
  cursor: wait;
  opacity: 0.7;
}

.grade-error {
  margin-top: 6px;
  color: #d32f2f;
  font-size: 12px;
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
</style>
