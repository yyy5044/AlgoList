<script setup>
import { ref } from 'vue'
import * as problemApi from '@/api/problems'
import { renderDescription } from '@/utils/renderDescription'

const emit = defineEmits(['problem-added'])

const sites = [
  { key: 'BOJ', label: '백준', icon: '/icons/BOJ.png', enabled: true },
  { key: 'CODEFORCES', label: '코드포스', icon: '/icons/CODEFORCES.png', enabled: true },
]

const selectedSite = ref(null)
const problems = ref([])
const page = ref(1) // 1부터 시작
const pageSize = 10
const isLastPage = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

async function selectSite(site) {
  if (!site.enabled) return
  selectedSite.value = site
  page.value = 1
  await fetchPage()
}

async function fetchPage() {
  isLoading.value = true
  errorMessage.value = ''
  try {
    problems.value = await problemApi.browse(selectedSite.value.key, page.value, pageSize)
    // 받아온 개수가 pageSize보다 적으면 마지막 페이지
    isLastPage.value = problems.value.length < pageSize
  } catch (error) {
    console.error('문제 목록 조회 실패:', error)
    errorMessage.value = '문제 목록을 불러오지 못했습니다.'
    problems.value = []
  } finally {
    isLoading.value = false
  }
}

function nextPage() {
  if (isLastPage.value) return
  page.value++
  fetchPage()
}

function prevPage() {
  if (page.value <= 1) return
  page.value--
  fetchPage()
}

function backToSites() {
  selectedSite.value = null
  problems.value = []
  page.value = 1
}

// 난이도 앞 단어(Bronze, Silver...)로 색상 클래스 결정
function difficultyClass(difficulty) {
  const tier = difficulty?.split(' ')[0].toLowerCase()
  return ['bronze', 'silver', 'gold', 'platinum', 'diamond', 'ruby'].includes(tier)
    ? `tier-${tier}`
    : 'tier-unknown'
}

// 내 문제로 추가
const addedSet = ref(new Set()) // 이번 화면에서 추가한 문제 ID 모음 (버튼 ✓ 표시용)

async function addToMyList(problem) {
  if (addedSet.value.has(problem.problemId)) return
  try {
    await problemApi.add(problem.problemId)
    addedSet.value.add(problem.problemId)
    emit('problem-added')
  } catch (error) {
    // 400(서비스 중복 검증) / 409(DB 중복 키) 모두 "이미 등록된 문제"
    if (error.status === 400 || error.status === 409) {
      addedSet.value.add(problem.problemId)
      alert('이미 내 문제에 등록된 문제입니다.')
      return
    }
    console.error('문제 추가 실패:', error)
    alert('문제 추가에 실패했습니다.')
  }
}

// 상세 모달
const detailProblem = ref(null)
const detailLoading = ref(false)

async function openDetail(problem) {
  detailProblem.value = problem
  detailLoading.value = true
  try {
    // 목록 응답에는 본문이 없으므로 모달을 열 때 본문만 따로 받아온다
    const description = await problemApi.fetchDescription(problem.problemId)
    detailProblem.value = { ...detailProblem.value, description }
  } catch (error) {
    console.error('문제 상세 조회 실패:', error)
  } finally {
    detailLoading.value = false
  }
}

function closeDetail() {
  detailProblem.value = null
}
</script>

<template>
  <div class="browse-page">
    <!-- 1단계: 사이트 선택 -->
    <div v-if="!selectedSite" class="site-select">
      <h2 class="browse-title">문제 둘러보기</h2>
      <p class="browse-subtitle">사이트를 선택하면 등록된 문제 목록을 볼 수 있어요.</p>
      <div class="site-cards">
        <button
          v-for="site in sites"
          :key="site.key"
          :class="['site-card', { disabled: !site.enabled }]"
          @click="selectSite(site)"
        >
          <img v-if="site.icon" :src="site.icon" :alt="site.label" class="site-card-icon" />
          <span class="site-card-label">{{ site.label }}</span>
          <span v-if="!site.enabled" class="coming-soon">준비 중</span>
        </button>
      </div>
    </div>

    <!-- 2단계: 문제 목록 -->
    <div v-else class="problem-list-view">
      <div class="list-header">
        <button class="back-button" @click="backToSites">← 사이트 선택</button>
        <h2 class="list-title">{{ selectedSite.label }} 문제 목록</h2>
      </div>

      <p v-if="isLoading" class="status-text">불러오는 중...</p>
      <p v-else-if="errorMessage" class="status-text error">{{ errorMessage }}</p>
      <p v-else-if="problems.length === 0" class="status-text">
        {{ page === 1 ? '등록된 문제가 없습니다.' : '더 이상 문제가 없습니다.' }}
      </p>

      <ul v-else class="problem-list">
        <li
          v-for="p in problems"
          :key="p.problemId"
          class="problem-row"
          @click="openDetail(p)"
        >
          <img :src="`/icons/${p.site}.png`" :alt="p.site" class="row-icon" />
          <span class="row-title">[{{ p.number }}] {{ p.title }}</span>
          <span class="row-categories">
            <span v-for="cat in p.category?.slice(0, 3)" :key="cat" class="category-chip">
              {{ cat }}
            </span>
          </span>
          <span :class="['row-difficulty', difficultyClass(p.difficulty)]">
            {{ p.difficulty }}
          </span>
          <button
            :class="['add-button', { added: addedSet.has(p.problemId) }]"
            :disabled="addedSet.has(p.problemId)"
            @click.stop="addToMyList(p)"
          >
            {{ addedSet.has(p.problemId) ? '✓' : '+' }}
          </button>
        </li>
      </ul>

      <!-- 페이징 -->
      <div class="paging-bar">
        <button class="paging-button" :disabled="page <= 1" @click="prevPage">← 이전</button>
        <span class="paging-info">{{ page }} 페이지</span>
        <button class="paging-button" :disabled="isLastPage" @click="nextPage">다음 →</button>
      </div>
    </div>

    <!-- 문제 상세 모달 -->
    <Teleport to="body">
      <div v-if="detailProblem" class="modal-overlay" @click.self="closeDetail">
        <div class="detail-modal">
          <div class="detail-header">
            <img :src="`/icons/${detailProblem.site}.png`" :alt="detailProblem.site" class="detail-icon" />
            <h3 class="detail-title">[{{ detailProblem.number }}] {{ detailProblem.title }}</h3>
            <span :class="['row-difficulty', difficultyClass(detailProblem.difficulty)]">
              {{ detailProblem.difficulty }}
            </span>
          </div>
          <div class="detail-categories" v-if="detailProblem.category?.length">
            <span v-for="cat in detailProblem.category" :key="cat" class="category-chip">{{ cat }}</span>
          </div>
          <p v-if="detailLoading" class="status-text">본문을 불러오는 중...</p>
          <div
            v-else-if="detailProblem.description"
            class="description-content"
            v-html="renderDescription(detailProblem.description)"
          ></div>
          <p v-else class="status-text">본문이 없습니다.</p>
          <div class="detail-footer">
            <a :href="detailProblem.link" target="_blank" rel="noopener" class="link-button">
              원문 보러가기 ↗
            </a>
            <button class="close-button" @click="closeDetail">닫기</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.browse-page {
  flex: 1;
  overflow-y: auto;
  background: white;
}

/* ── 사이트 선택 ── */
.site-select {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.browse-title {
  font-size: 24px;
  color: #333;
}

.browse-subtitle {
  font-size: 14px;
  color: #888;
  margin-bottom: 12px;
}

.site-cards {
  display: flex;
  gap: 16px;
}

.site-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 160px;
  height: 140px;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: box-shadow 0.15s, transform 0.15s;
}

.site-card:hover:not(.disabled) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
  border-color: #1a56db;
}

.site-card.disabled {
  cursor: default;
  opacity: 0.5;
}

.site-card-icon {
  width: 48px;
  height: 48px;
}

.site-card-label {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.coming-soon {
  font-size: 12px;
  color: #999;
}

/* ── 문제 목록 ── */
.problem-list-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 32px;
}

.list-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.back-button {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.back-button:hover {
  background: #f5f5f5;
}

.list-title {
  font-size: 18px;
  color: #333;
}

.status-text {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 40px 0;
}

.status-text.error {
  color: #d32f2f;
}

.problem-list {
  list-style: none;
  flex: 1;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 8px;
}

.problem-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.15s;
}

.problem-row:last-child {
  border-bottom: none;
}

.problem-row:hover {
  background-color: #f0f4ff;
}

.row-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.row-title {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-categories {
  display: flex;
  gap: 4px;
  margin-left: auto;
  flex-shrink: 0;
}

.category-chip {
  font-size: 11px;
  color: #555;
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 10px;
}

.row-difficulty {
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  width: 90px;
  text-align: right;
}

.add-button {
  width: 28px;
  height: 28px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  font-size: 16px;
  color: #1a56db;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-button:hover:not(:disabled) {
  background: #f0f4ff;
  border-color: #1a56db;
}

.add-button.added {
  color: #22c55e;
  border-color: #22c55e;
  cursor: default;
}

/* 백준 티어 색상 */
.tier-bronze { color: #ad5600; }
.tier-silver { color: #435f7a; }
.tier-gold { color: #ec9a00; }
.tier-platinum { color: #27e2a4; }
.tier-diamond { color: #00b4fc; }
.tier-ruby { color: #ff0062; }
.tier-unknown { color: #888; }

/* ── 페이징 ── */
.paging-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px 0 4px;
}

.paging-button {
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.paging-button:hover:not(:disabled) {
  background: #f0f4ff;
  border-color: #1a56db;
}

.paging-button:disabled {
  opacity: 0.4;
  cursor: default;
}

.paging-info {
  font-size: 13px;
  color: #666;
}

/* ── 상세 모달 ── */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.detail-modal {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 720px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.detail-icon {
  width: 24px;
  height: 24px;
}

.detail-title {
  font-size: 18px;
  color: #333;
  flex: 1;
}

.detail-categories {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.description-content {
  flex: 1;
  overflow-y: auto;
  font-size: 14px;
  color: #333;
  border-top: 1px solid #eee;
  padding-top: 12px;
}

/* 전역 리셋(* { padding: 0 })이 리스트 들여쓰기를 제거하므로 복원 */
.description-content :deep(ol),
.description-content :deep(ul) {
  padding-left: 2em;
  margin: 4px 0;
}

.description-content :deep(li) {
  margin: 2px 0;
}

.description-content :deep(p) {
  margin: 8px 0;
}

.description-content :deep(img) {
  max-width: 100%;
}

.description-content :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
}

.description-content :deep(th),
.description-content :deep(td) {
  border: 1px solid #ddd;
  padding: 6px 10px;
}

.detail-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.link-button {
  font-size: 14px;
  color: #1a56db;
  text-decoration: none;
}

.link-button:hover {
  text-decoration: underline;
}

.close-button {
  padding: 8px 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.close-button:hover {
  background: #f5f5f5;
}
</style>
