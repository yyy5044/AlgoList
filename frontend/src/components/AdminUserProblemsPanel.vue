<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import Prism from 'prismjs'
import 'prismjs/components/prism-java'
import 'prismjs/components/prism-python'
import 'prismjs/components/prism-c'
import 'prismjs/components/prism-cpp'
import 'prismjs/plugins/line-numbers/prism-line-numbers'
import 'prismjs/themes/prism-tomorrow.css'
import 'prismjs/plugins/line-numbers/prism-line-numbers.css'
import * as adminApi from '../api/admin'
import { renderDescription } from '../utils/renderDescription'

const props = defineProps({
  username: {
    type: String,
    default: '',
  },
  displayUsername: {
    type: String,
    default: '',
  },
  active: {
    type: Boolean,
    default: false,
  },
})

const adminProblems = ref([])
const selectedAdminProblem = ref(null)
const adminSolutions = ref([])
const selectedAdminSolutionId = ref(null)
const selectedAdminSolution = ref(null)
const isLoadingAdminProblems = ref(false)
const isLoadingAdminSolutions = ref(false)
const isLoadingAdminSolution = ref(false)
const adminProblemsLoaded = ref(false)
const adminProblemsError = ref('')
const adminSolutionsError = ref('')
const adminSolutionError = ref('')
const adminLoadingSolutionId = ref(null)
const adminProblemsPanel = ref(null)

const selectedAdminProblemDescriptionHtml = computed(() => {
  return renderDescription(
    selectedAdminProblem.value?.problem?.description,
    selectedAdminProblem.value?.problem?.site,
  )
})

function getPrismLanguage(language) {
  if (language === 'python') return 'python'
  if (language === 'cpp') return 'cpp'

  return 'java'
}

function getSolutionLanguageClass(solution) {
  return `language-${getPrismLanguage(solution?.language)}`
}

function getSolutionTypeLabel(type) {
  if (type === 'FIRST') return '첫시도'
  if (type === 'RECAP') return '복습'
  if (type === 'OPT') return '최적화'

  return type || '풀이'
}

function getSolutionFileExtension(language) {
  if (language === 'python') return 'py'
  if (language === 'cpp') return 'cpp'

  return 'java'
}

function getSolutionFileName(solution) {
  const problemNumber = selectedAdminProblem.value?.problem?.number || 'problem'
  const username = props.displayUsername || props.username || 'user'

  return `Main_${problemNumber}_${username}_${solution.algorithm}_${getSolutionTypeLabel(solution.type)}.${getSolutionFileExtension(solution.language)}`
}

function resetAdminSolutionState() {
  adminSolutions.value = []
  selectedAdminSolutionId.value = null
  selectedAdminSolution.value = null
  adminSolutionsError.value = ''
  adminSolutionError.value = ''
  isLoadingAdminSolutions.value = false
  isLoadingAdminSolution.value = false
  adminLoadingSolutionId.value = null
}

function resetAdminProblemState() {
  adminProblems.value = []
  selectedAdminProblem.value = null
  isLoadingAdminProblems.value = false
  adminProblemsLoaded.value = false
  adminProblemsError.value = ''
  resetAdminSolutionState()
}

async function loadAdminProblems() {
  if (!props.active || !props.username || adminProblemsLoaded.value) return

  try {
    isLoadingAdminProblems.value = true
    adminProblemsError.value = ''
    adminProblems.value = await adminApi.fetchUserProblems(props.username)
    adminProblemsLoaded.value = true
  } catch (error) {
    console.error('관리자 회원 문제 목록 조회 실패:', error)
    adminProblemsError.value =
      error?.status === 404 ? '회원을 찾을 수 없습니다.' : '작성한 문제를 불러오지 못했습니다.'
  } finally {
    isLoadingAdminProblems.value = false
  }
}

async function selectAdminProblem(problem) {
  selectedAdminProblem.value = problem
  resetAdminSolutionState()

  try {
    isLoadingAdminSolutions.value = true
    adminSolutionsError.value = ''
    adminSolutions.value = await adminApi.fetchUserProblemSolutions(props.username, problem.userProblemId)
  } catch (error) {
    console.error('관리자 회원 풀이 목록 조회 실패:', error)
    adminSolutionsError.value =
      error?.status === 404 ? '문제 정보를 찾을 수 없습니다.' : '풀이 목록을 불러오지 못했습니다.'
  } finally {
    isLoadingAdminSolutions.value = false
  }
}

function resetAdminSolutionDetail() {
  selectedAdminSolutionId.value = null
  selectedAdminSolution.value = null
  adminSolutionError.value = ''
  isLoadingAdminSolution.value = false
  adminLoadingSolutionId.value = null
}

function highlightSelectedAdminSolution() {
  const codeBlock = adminProblemsPanel.value?.querySelector('.admin-solution-code code')
  if (!codeBlock) return

  codeBlock.removeAttribute('data-highlighted')
  Prism.highlightElement(codeBlock)
}

async function toggleAdminSolution(solutionId) {
  if (selectedAdminSolutionId.value === solutionId) {
    resetAdminSolutionDetail()
    return
  }

  selectedAdminSolutionId.value = solutionId
  selectedAdminSolution.value = null
  adminSolutionError.value = ''
  adminLoadingSolutionId.value = solutionId

  try {
    isLoadingAdminSolution.value = true
    const solution = await adminApi.fetchUserSolution(
      props.username,
      selectedAdminProblem.value.userProblemId,
      solutionId,
    )

    if (adminLoadingSolutionId.value !== solutionId) return

    selectedAdminSolution.value = solution
    await nextTick()
    requestAnimationFrame(highlightSelectedAdminSolution)
  } catch (error) {
    console.error('관리자 회원 풀이 상세 조회 실패:', error)
    adminSolutionError.value =
      error?.status === 404 ? '풀이 정보를 찾을 수 없습니다.' : '소스코드를 불러오지 못했습니다.'
  } finally {
    if (adminLoadingSolutionId.value === solutionId) {
      adminLoadingSolutionId.value = null
      isLoadingAdminSolution.value = false
    }
  }
}

watch(
  () => props.username,
  () => {
    resetAdminProblemState()
    if (props.active) {
      loadAdminProblems()
    }
  },
)

watch(
  () => props.active,
  (active) => {
    if (active) {
      loadAdminProblems()
    }
  },
  { immediate: true },
)
</script>

<template>
  <section ref="adminProblemsPanel" class="admin-problems-panel">
    <p v-if="isLoadingAdminProblems" class="guide-message">작성한 문제를 불러오는 중입니다.</p>
    <p v-if="adminProblemsError" class="error">{{ adminProblemsError }}</p>

    <div v-if="!isLoadingAdminProblems && adminProblems.length > 0" class="admin-problem-layout">
      <ul class="admin-problem-list">
        <li
          v-for="problem in adminProblems"
          :key="problem.userProblemId"
          :class="[
            'admin-problem-item',
            { active: selectedAdminProblem?.userProblemId === problem.userProblemId },
          ]"
          @click="selectAdminProblem(problem)"
        >
          <img
            :src="`/icons/${problem.problem.site}.png`"
            :alt="problem.problem.site"
            class="admin-problem-site-icon"
          />
          <div class="admin-problem-summary">
            <strong>[{{ problem.problem.number }}] {{ problem.problem.title }}</strong>
            <span>{{ problem.problem.site }} · {{ problem.problem.difficulty || '난이도 없음' }}</span>
          </div>
        </li>
      </ul>

      <div class="admin-problem-detail">
        <div v-if="selectedAdminProblem" class="admin-problem-detail-content">
          <div class="admin-problem-detail-header">
            <img
              :src="`/icons/${selectedAdminProblem.problem.site}.png`"
              :alt="selectedAdminProblem.problem.site"
              class="admin-problem-site-icon"
            />
            <a :href="selectedAdminProblem.problem.link" target="_blank" class="admin-problem-title-link">
              <h3>[{{ selectedAdminProblem.problem.number }}] {{ selectedAdminProblem.problem.title }}</h3>
            </a>
          </div>

          <div class="info-list">
            <div class="info-row">
              <span class="info-label">등급</span>
              <span class="info-value">{{ selectedAdminProblem.grade || '미지정' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">풀이 횟수</span>
              <span class="info-value">{{ selectedAdminProblem.solveCount || 0 }}회</span>
            </div>
            <div class="info-row">
              <span class="info-label">최근 풀이</span>
              <span class="info-value">{{ selectedAdminProblem.lastSolvedDate || '없음' }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">알고리즘 분류</span>
              <div class="category-tags">
                <span
                  v-for="category in selectedAdminProblem.problem.category || []"
                  :key="category"
                  class="category-tag"
                >
                  {{ category }}
                </span>
              </div>
            </div>
          </div>

          <div v-if="selectedAdminProblem.problem.description" class="admin-description-section">
            <div class="admin-description-content" v-html="selectedAdminProblemDescriptionHtml"></div>
          </div>

          <div class="admin-solutions-section">
            <div class="admin-solutions-header">
              <h3>소스코드 목록</h3>
              <span>{{ adminSolutions.length }}개</span>
            </div>
            <p v-if="isLoadingAdminSolutions" class="guide-message">풀이 목록을 불러오는 중입니다.</p>
            <p v-if="adminSolutionsError" class="error">{{ adminSolutionsError }}</p>
            <template v-if="!isLoadingAdminSolutions && adminSolutions.length > 0">
              <template
                v-for="solution in adminSolutions"
                :key="solution.solutionId"
              >
                <div
                  :class="[
                    'admin-solution-item',
                    { active: selectedAdminSolutionId === solution.solutionId },
                  ]"
                  @click="toggleAdminSolution(solution.solutionId)"
                >
                  <span>{{ getSolutionFileName(solution) }}</span>
                  <small>{{ solution.language }} · {{ getSolutionTypeLabel(solution.type) }}</small>
                </div>
                <p
                  v-if="adminSolutionError && selectedAdminSolutionId === solution.solutionId"
                  class="admin-solution-detail-status error"
                >
                  {{ adminSolutionError }}
                </p>
                <p
                  v-else-if="isLoadingAdminSolution && adminLoadingSolutionId === solution.solutionId"
                  class="admin-solution-detail-status"
                >
                  소스코드를 불러오는 중입니다.
                </p>
                <pre
                  v-else-if="selectedAdminSolution?.solutionId === solution.solutionId"
                  class="admin-solution-code line-numbers"
                  :class="getSolutionLanguageClass(selectedAdminSolution)"
                ><code :class="getSolutionLanguageClass(selectedAdminSolution)">{{ selectedAdminSolution.code }}</code></pre>
              </template>
            </template>
            <p v-else-if="!isLoadingAdminSolutions" class="empty-list">등록된 소스코드가 없습니다.</p>
          </div>
        </div>
        <p v-else class="empty-list">문제를 선택하면 상세 정보와 소스코드를 확인할 수 있습니다.</p>
      </div>
    </div>

    <p v-else-if="!isLoadingAdminProblems && !adminProblemsError" class="empty-list">
      등록된 문제가 없습니다.
    </p>
  </section>
</template>

<style scoped>
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

.admin-problems-panel {
  min-width: 0;
}

.admin-problem-layout {
  display: grid;
  grid-template-columns: minmax(260px, 340px) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}

.admin-problem-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.admin-problem-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.admin-problem-item:last-child {
  border-bottom: none;
}

.admin-problem-item:hover,
.admin-problem-item.active {
  background: #f0f6fd;
}

.admin-problem-site-icon {
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
}

.admin-problem-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.admin-problem-summary strong {
  color: #333;
  font-size: 14px;
  word-break: break-word;
}

.admin-problem-summary span {
  color: #888;
  font-size: 12px;
}

.admin-problem-detail {
  min-width: 0;
}

.admin-problem-detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-problem-title-link {
  color: inherit;
  text-decoration: none;
}

.admin-problem-title-link:hover h3 {
  color: #1a56db;
}

.admin-problem-detail-header h3,
.admin-solutions-header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
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

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.category-tag {
  padding: 3px 10px;
  border-radius: 12px;
  background: #e8f0fe;
  color: #1a56db;
  font-size: 12px;
}

.admin-description-section {
  padding: 18px 0;
  border-top: 1px solid #eee;
}

.admin-description-content {
  color: #444;
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}

.admin-description-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.admin-description-content :deep(pre) {
  padding: 12px;
  overflow-x: auto;
  border-radius: 6px;
  background: #f5f5f5;
}

.admin-description-content :deep(table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
}

.admin-description-content :deep(th),
.admin-description-content :deep(td) {
  padding: 8px 12px;
  border: 1px solid #ddd;
  font-size: 13px;
}

.admin-solutions-section {
  padding-top: 18px;
  border-top: 1px solid #eee;
}

.admin-solutions-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.admin-solutions-header span {
  color: #888;
  font-size: 13px;
}

.admin-solution-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  margin-top: 8px;
  border-radius: 6px;
  background: #f8f9fa;
  cursor: pointer;
}

.admin-solution-item:hover,
.admin-solution-item.active {
  background: #d0e3ff;
}

.admin-solution-item.active {
  border-radius: 6px 6px 0 0;
}

.admin-solution-item span {
  min-width: 0;
  color: #333;
  font-size: 14px;
  word-break: break-all;
}

.admin-solution-item small {
  flex: 0 0 auto;
  color: #777;
  font-size: 12px;
}

.admin-solution-detail-status {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #eee;
  border-top: none;
  border-radius: 0 0 6px 6px;
  color: #666;
  font-size: 13px;
}

.admin-solution-code {
  margin: 0;
  max-height: 420px;
  overflow: auto;
  border-radius: 0 0 6px 6px;
  font-size: 13px;
  white-space: pre;
}

.empty-list {
  padding: 24px 12px;
  color: #999;
  font-size: 14px;
  text-align: center;
}

@media (max-width: 640px) {
  .admin-problem-layout {
    grid-template-columns: 1fr;
  }

  .admin-solution-item {
    flex-direction: column;
  }
}
</style>
