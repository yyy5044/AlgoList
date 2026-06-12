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
import { NETWORK_ERROR_MESSAGE, readErrorMessage } from '../utils/apiError'

const props = defineProps({
  selectedItem: Object,
  username: String
})

const maxSolutionsPerProblem = 20
const maxAlgorithmLength = 50
const maxCodeBytes = 60 * 1024

// 풀이 목록
const solutions = ref([])
const selectedSolutionId = ref(null)
const selectedSolution = ref(null)
const isLoadingSolution = ref(false)
const solutionDetailError = ref('')
const codeBlocks = ref([])
const loadingSolutionId = ref(null)

// 업로드 폼 데이터
const showUploadForm = ref(false)
const algorithm = ref('')
const type = ref('FIRST')
const language = ref('java')
const selectedFile = ref(null)
const fileError = ref('')
const errorMessage = ref('')
const isUploading = ref(false)
const fileInput = ref(null)
const isDragActive = ref(false)
let dragDepth = 0

const allowedFileExtensions = {
  java: ['java', 'txt'],
  python: ['py', 'txt'],
  cpp: ['cpp', 'cc', 'cxx', 'txt']
}

const fileAccept = computed(() => {
  return allowedFileExtensions[language.value]
    .map(extension => `.${extension}`)
    .join(',')
})

const isSolutionLimitReached = computed(() => {
  return solutions.value.length >= maxSolutionsPerProblem
})

function getPrismLanguage(language) {
  if (language === 'python') return 'python'
  if (language === 'cpp') return 'cpp'

  return 'java'
}

function getSolutionLanguageClass(solution) {
  return `language-${getPrismLanguage(solution?.language)}`
}

function isAllowedFile(file) {
  const extension = file.name.split('.').pop()?.toLowerCase()

  return allowedFileExtensions[language.value].includes(extension)
}

function getAllowedExtensionsText() {
  return allowedFileExtensions[language.value]
    .map(extension => `.${extension}`)
    .join(', ')
}

function validateFile(file) {
  if (!file) {
    fileError.value = '파일을 선택해주세요.'
    return false
  }

  if (!isAllowedFile(file)) {
    fileError.value = `선택한 언어에서는 ${getAllowedExtensionsText()} 파일만 업로드할 수 있습니다.`
    return false
  }

  fileError.value = ''
  return true
}

function getCodeByteLength(code) {
  return new TextEncoder().encode(code).length
}

function getCharacterLength(value) {
  return Array.from(value).length
}

function resetFileSelection() {
  selectedFile.value = null
  fileError.value = ''
  isDragActive.value = false
  dragDepth = 0
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

async function fetchSolutions(userProblemId) {
  const response = await fetch(`/api/solutions/${userProblemId}`, {
    credentials: 'include' // 세션 쿠키
  })
  solutions.value = await response.json()
}

function resetSolutionDetail() {
  selectedSolutionId.value = null
  selectedSolution.value = null
  solutionDetailError.value = ''
  isLoadingSolution.value = false
  loadingSolutionId.value = null
}

async function fetchSolutionDetail(solutionId) {
  solutionDetailError.value = ''
  loadingSolutionId.value = solutionId

  if (!selectedSolution.value) {
    selectedSolutionId.value = solutionId
  }

  try {
    isLoadingSolution.value = true
    const response = await fetch(`/api/solutions/detail/${solutionId}`, {
      credentials: 'include' // 세션 쿠키
    })

    if (!response.ok) {
      if (selectedSolutionId.value === solutionId) {
        solutionDetailError.value = await readErrorMessage(response, '풀이 상세 정보를 불러올 수 없습니다.')
      }
      return
    }

    const solution = await response.json()
    if (loadingSolutionId.value !== solutionId) {
      return
    }

    selectedSolutionId.value = solutionId
    selectedSolution.value = solution
    await nextTick()
    requestAnimationFrame(highlightOpenedSolutions)
  } catch (error) {
    console.error('풀이 상세 조회 실패:', error)
    if (selectedSolutionId.value === solutionId) {
      solutionDetailError.value = NETWORK_ERROR_MESSAGE
    }
  } finally {
    if (loadingSolutionId.value === solutionId) {
      loadingSolutionId.value = null
      isLoadingSolution.value = false
    }
  }
}

async function toggleSolutionDetail(solutionId) {
  if (selectedSolutionId.value === solutionId) {
    resetSolutionDetail()
    return
  }

  await fetchSolutionDetail(solutionId)
}

function isSolutionSelected(solutionId) {
  return selectedSolutionId.value === solutionId
}

function isSelectedSolutionLoaded(solutionId) {
  return selectedSolution.value?.solutionId === solutionId
}

function highlightOpenedSolutions() {
  codeBlocks.value
    .filter(Boolean)
    .forEach(codeBlock => {
      codeBlock.removeAttribute('data-highlighted')
      Prism.highlightElement(codeBlock)
    })
}

// 문제가 바뀔 때마다 풀이 목록 다시 가져오기
watch(() => props.selectedItem, async (newItem) => {
  if (newItem?.userProblemId) {
    try {
      await fetchSolutions(newItem.userProblemId)
      resetSolutionDetail()
    } catch (error) {
      console.error('풀이 목록 조회 실패:', error)
    }
  } else {
    solutions.value = []
    resetSolutionDetail()
  }
}, { immediate: true })

function selectFile(file) {
  if (validateFile(file)) {
    selectedFile.value = file
    return true
  }

  selectedFile.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  return false
}

function openFileDialog() {
  fileInput.value?.click()
}

function handleFileChange(event) {
  errorMessage.value = ''
  selectFile(event.target.files[0])
}

function handleDragEnter() {
  dragDepth += 1
  isDragActive.value = true
}

function handleDragLeave() {
  dragDepth = Math.max(dragDepth - 1, 0)
  isDragActive.value = dragDepth > 0
}

function handleDrop(event) {
  errorMessage.value = ''
  dragDepth = 0
  isDragActive.value = false
  selectFile(event.dataTransfer.files[0])
}

watch(language, () => {
  resetFileSelection()
})

function toggleUploadForm() {
  if (isSolutionLimitReached.value && !showUploadForm.value) {
    errorMessage.value = `문제당 풀이 업로드는 최대 ${maxSolutionsPerProblem}개까지 가능합니다.`
    return
  }

  showUploadForm.value = !showUploadForm.value
  errorMessage.value = ''
  resetFileSelection()
}

// 파일 업로드
async function uploadFile() {
  errorMessage.value = ''

  if (isSolutionLimitReached.value) {
    errorMessage.value = `문제당 풀이 업로드는 최대 ${maxSolutionsPerProblem}개까지 가능합니다.`
    return
  }

  if (!algorithm.value.trim()) {
    errorMessage.value = '풀이 정보를 모두 입력해주세요.'
    return
  }

  if (getCharacterLength(algorithm.value.trim()) > maxAlgorithmLength) {
    errorMessage.value = `주요 알고리즘은 최대 ${maxAlgorithmLength}자까지 입력할 수 있습니다.`
    return
  }

  const file = selectedFile.value
  if (!validateFile(file)) return

  const code = await file.text()

  if (getCodeByteLength(code) > maxCodeBytes) {
    errorMessage.value = '소스 코드는 최대 60KB까지 업로드할 수 있습니다.'
    return
  }

  const solution = {
    algorithm: algorithm.value,
    type: type.value,
    language: language.value,
    fileName: file.name,
    code: code,
    userProblemId: props.selectedItem.userProblemId
  }

  try {
    isUploading.value = true
    const response = await fetch('/api/solutions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(solution),
      credentials: 'include' // 세션 쿠키
    })
    if (!response.ok) {
      errorMessage.value = await readErrorMessage(response, '업로드에 실패했습니다.')
      return
    }

    await fetchSolutions(props.selectedItem.userProblemId)
    resetSolutionDetail()
    showUploadForm.value = false
    algorithm.value = ''
    type.value = 'FIRST'
    resetFileSelection()
  } catch (error) {
    console.error('업로드 실패:', error)
    errorMessage.value = NETWORK_ERROR_MESSAGE
  } finally {
    isUploading.value = false
  }
}

// 풀이 삭제
async function deleteSolution(id) {
  try {
    const response = await fetch(`/api/solutions/${id}`, {
      method: 'DELETE',
      credentials: 'include' // 세션 쿠키
    })
    if (!response.ok) {
      errorMessage.value = await readErrorMessage(response, '삭제에 실패했습니다.')
      return
    }

    solutions.value = solutions.value.filter(s => s.solutionId !== id)
    if (selectedSolutionId.value === id) {
      resetSolutionDetail()
    }
  } catch (error) {
    console.error('삭제 실패:', error)
    errorMessage.value = NETWORK_ERROR_MESSAGE
  }
}
</script>

<template>
  <div class="solution-manager" v-if="selectedItem">
    <div class="solution-header">
      <h3>풀이 관리 ({{ solutions.length }}/{{ maxSolutionsPerProblem }})</h3>
      <button :disabled="isSolutionLimitReached" @click="toggleUploadForm">+ 업로드</button>
    </div>
    <p v-if="isSolutionLimitReached" class="limit-notice">
      문제당 풀이 업로드는 최대 {{ maxSolutionsPerProblem }}개까지 가능합니다.
    </p>

    <!-- 업로드 폼 -->
    <div v-if="showUploadForm" class="upload-form">
      <div class="form-row">
        <label>주요 알고리즘</label>
        <input v-model="algorithm" :maxlength="maxAlgorithmLength" placeholder="예: BFS, DFS, DP..." />
      </div>
      <div class="form-row">
        <label>풀이 유형</label>
        <select v-model="type">
          <option value="FIRST">첫 시도</option>
          <option value="RECAP">복습</option>
          <option value="OPT">최적화</option>
        </select>
      </div>
      <div class="form-row">
        <label>언어</label>
        <select v-model="language">
          <option value="java">Java</option>
          <option value="python">Python</option>
          <option value="cpp">C++</option>
        </select>
      </div>
      <div class="form-row">
        <label>파일 선택</label>
        <div
          :class="['file-drop-zone', { active: isDragActive, selected: selectedFile }]"
          role="button"
          tabindex="0"
          @click="openFileDialog"
          @keydown.enter.prevent="openFileDialog"
          @keydown.space.prevent="openFileDialog"
          @dragenter.prevent="handleDragEnter"
          @dragover.prevent
          @dragleave.prevent="handleDragLeave"
          @drop.prevent="handleDrop"
        >
          <input ref="fileInput" class="file-input" type="file" :accept="fileAccept" @change="handleFileChange" />
          <strong>{{ selectedFile ? selectedFile.name : '파일을 드래그하거나 클릭해서 선택하세요' }}</strong>
          <span>{{ getAllowedExtensionsText() }} 파일 업로드 가능</span>
        </div>
        <p v-if="fileError" class="file-error">{{ fileError }}</p>
      </div>
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <button type="button" class="upload-btn" :disabled="isUploading" @click="uploadFile">
        {{ isUploading ? '업로드 중...' : '업로드' }}
      </button>
    </div>

    <!-- 풀이 목록 -->
    <div v-if="solutions.length > 0" class="solution-list">
      <template v-for="sol in solutions" :key="sol.solutionId">
        <div
          :class="['solution-item', { active: isSolutionSelected(sol.solutionId) }]"
          @click="toggleSolutionDetail(sol.solutionId)"
        >
          <span class="solution-info">
            Main_{{ selectedItem.problem.number }}_{{ username }}_{{ sol.algorithm }}_{{ sol.type === 'FIRST' ? '첫시도' : sol.type === 'RECAP' ? '복습' : '최적화' }}.{{ sol.language === 'java' ? 'java' : sol.language === 'python' ? 'py' : 'cpp' }}
          </span>
          <button class="delete-btn" @click.stop="deleteSolution(sol.solutionId)">삭제</button>
        </div>

        <!-- 풀이 상세 -->
        <div v-if="isLoadingSolution && loadingSolutionId === sol.solutionId && !selectedSolution" class="solution-detail-status">
          불러오는 중...
        </div>
        <p v-else-if="solutionDetailError && selectedSolutionId === sol.solutionId" class="error">{{ solutionDetailError }}</p>
        <div v-else-if="isSelectedSolutionLoaded(sol.solutionId)" class="solution-detail">
          <pre class="solution-code line-numbers" :class="getSolutionLanguageClass(selectedSolution)"><code ref="codeBlocks" :class="getSolutionLanguageClass(selectedSolution)">{{ selectedSolution.code }}</code></pre>
        </div>
      </template>
    </div>
    <p v-else class="no-solutions">등록된 풀이가 없습니다.</p>
  </div>
</template>

<style scoped>
.solution-manager {
  border-top: 1px solid #eee;
  padding: 20px 0;
}

.solution-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.solution-header h3 {
  font-size: 16px;
  color: #333;
}

.solution-header button {
  padding: 4px 12px;
  background: #4a90d9;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.solution-header button:disabled {
  background: #b7c7d9;
  cursor: not-allowed;
}

.limit-notice {
  margin: -4px 0 12px;
  color: #666;
  font-size: 13px;
}

.upload-form {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.form-row {
  margin-bottom: 8px;
}

.form-row label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.form-row input,
.form-row select {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.file-drop-zone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 108px;
  padding: 16px;
  border: 2px dashed #c9d6e2;
  border-radius: 6px;
  background: #ffffff;
  color: #54616d;
  cursor: pointer;
  text-align: center;
  transition: border-color 0.15s ease, background-color 0.15s ease, color 0.15s ease;
}

.file-drop-zone:hover,
.file-drop-zone:focus {
  border-color: #4a90d9;
  outline: none;
}

.file-drop-zone.active {
  border-color: #1a56db;
  background: #e8f0fe;
  color: #1a56db;
}

.file-drop-zone.selected {
  border-color: #4a90d9;
  background: #f1f7ff;
}

.file-drop-zone strong {
  max-width: 100%;
  margin-bottom: 6px;
  color: #333;
  font-size: 14px;
  word-break: break-all;
}

.file-drop-zone span {
  font-size: 12px;
}

.file-input {
  display: none;
}

.file-error {
  margin: 4px 0 0;
  color: #e74c3c;
  font-size: 12px;
}

.error {
  color: #e74c3c;
  font-size: 13px;
  margin: 0 0 12px;
}

.upload-btn {
  width: 100%;
  padding: 6px 8px;
  background: #4a90d9;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.upload-btn:disabled {
  background: #b7c7d9;
  cursor: not-allowed;
}

.solution-list {
  display: flex;
  flex-direction: column;
}

.solution-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 8px;
}

.solution-item.active {
  background: #d0e3ff;
  border-radius: 6px 6px 0 0;
}

.solution-info {
  font-size: 14px;
  color: #333;
}

.delete-btn {
  padding: 2px 8px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.no-solutions {
  color: #999;
  font-size: 14px;
}

.solution-detail-status {
  padding: 8px 12px;
  border: 1px solid #eee;
  border-top: none;
  color: #666;
  font-size: 13px;
}

.solution-detail {
  border: 1px solid #eee;
  border-top: none;
  border-radius: 0 0 6px 6px;
  overflow: hidden;
}

.solution-code {
  margin: 0;
  padding: 12px;
  max-height: 360px;
  overflow: auto;
  background: #1f2933;
  color: #f8f9fa;
  font-size: 13px;
  white-space: pre;
}
</style>
