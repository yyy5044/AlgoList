<script setup>
import { ref, computed, onMounted } from 'vue'

const emit = defineEmits(['select-item'])

// 탭
const tabs = ['문제', '분류', 'Git']
const currentTab = ref('문제')

// 문제 리스트 필터링용 검색어 변수
const searchQuery = ref('')

// 문제 리스트 변수
const items = ref([])

onMounted(async () => {
  // items[]는 처음에 비어있다가 프론트 시작시에 onMoundted로 API 요청 후 채운다.
  try {
    const response = await fetch('/api/problems', {
      credentials: 'include' // 세션 쿠키
    })
    items.value = await response.json()
  } catch (error) {
    console.error('목록 조회 실패:', error)
  }
})

// 선택 / 메뉴
const selectedItem = ref(null) // 리스트 내에서 문제를 선택하기 위한 변수
const openMenuId = ref(null) // 드롭다운 메뉴가 열린 항목의 id

function selectItem(item) {
  selectedItem.value = item
  openMenuId.value = null
  emit('select-item', item)
}

function toggleMenu(item) {
  openMenuId.value = openMenuId.value === item.id ? null : item.id
}

// 문제 리스트 검색 변수: searchQuery가 변경될 때마다 items에서 필터링해서 filteredItem으로 할당한다
const filteredItems = computed(() => {
  if (!searchQuery.value) return items.value
  const query = searchQuery.value.toLowerCase()
  return items.value.filter(
    (item) =>
      item.title.toLowerCase().includes(query) ||
      item.number.includes(query) ||
      item.category.some((cat) => cat.toLowerCase().includes(query)),
  )
})

// 요소 추가 로직
// 1. 문제 검색
// 검색 모달
const isSearchModalOpen = ref(false) // 모달이 열려 있으면 true, 닫혀 있으면 false
const problemSearchQuery = ref('') // 모달에 있는 검색창에 들어오는 입력은 problemSearchQuery에 할당됨

function openSearchModal() {
  isSearchModalOpen.value = true
  problemSearchQuery.value = ''
}

// closeSearchModal도 수정
function closeSearchModal() {
  isSearchModalOpen.value = false
  problemSearchQuery.value = ''
  searchResults.value = []
  hasSearched.value = false
  duplicateMessage.value = ''
}

// 2. 검색 함수: 8080 포트로 GET 요청
const searchResults = ref([]) // 검색 결과를 받는 리스트
const hasSearched = ref(false) // "검색 결과 없음"을 표시하기 위한 변수, searchResults가 0인데 hasSeared는 true일 때 -> 검색 결과 없음
const searchError = ref('') // 검색 실패 시에 에러 메세지 받을 변수

// 검색 모달에서 문제 검색 함수
async function searchProblem() {
  if (!problemSearchQuery.value) return
  try {
    searchError.value = ''
    const response = await fetch(
      `/api/search?query=${problemSearchQuery.value}`, {
        credentials: 'include' // 세션 쿠키
      }
    )
    searchResults.value = await response.json()
    hasSearched.value = true
  } catch (error) {
    searchError.value = '검색 중 오류가 발생했습니다. 다시 시도해주세요.'
    console.error('검색 실패:', error)
  }
}

// 검색 결과 중에서 문제 선택할 때 호출되는 함수
const duplicateMessage = ref('') // 문제 중복 추가 시도 시 메세지
async function selectSearchResult(result) {
    // 중복 체크: 같은 사이트 + 같은 문제 번호
  const isDuplicate = items.value.some(
    item => item.site === result.site && item.number === result.number
  )
  if (isDuplicate) {
    duplicateMessage.value = '이미 추가된 문제입니다.'
    return
  }

  try {
    const response = await fetch('/api/problems', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(result),
      credentials: 'include' // 세션 쿠키
    })
    const savedProblem = await response.json()
    // console.log(savedProblem) // 디버깅용: 응답 json이 잘 채워져 있는지
    items.value.push(savedProblem)
    openMenuId.value = null
    selectedItem.value = savedProblem // 문제 추가하고 나서 추가된 문제 선택
    emit('select-item', savedProblem)
    closeSearchModal()
  } catch (error) {
    console.error('문제 저장 실패:', error)
  }
}

// 개별 삭제
async function deleteItem(item) {
  try {
    await fetch(`/api/problems/${item.id}`, {
      method: 'DELETE',
      credentials: 'include' // 세션 쿠키
    })
    items.value = items.value.filter((i) => i.id !== item.id)
    if (selectedItem.value?.id === item.id) {
      selectedItem.value = null
      emit('select-item', null)
    }
    openMenuId.value = null
  } catch (error) {
    console.error('삭제 실패:', error)
  }
}

// 다중 삭제 모드
const isDeleteMode = ref(false)
const checkedIds = ref([])

function enterDeleteMode() {
  isDeleteMode.value = true
  checkedIds.value = []
}

function cancelDeleteMode() {
  isDeleteMode.value = false
  checkedIds.value = []
}

function toggleCheck(id) {
  if (checkedIds.value.includes(id)) {
    checkedIds.value = checkedIds.value.filter((i) => i !== id)
  } else {
    checkedIds.value.push(id)
  }
}

// 다중 삭제 함수
async function deleteChecked() {
  try {
    for (const id of checkedIds.value) {
      await fetch(`/api/problems/${id}`, {
        method: 'DELETE',
        credentials: 'include' // 세션 쿠키
      })
    }
    items.value = items.value.filter((item) => !checkedIds.value.includes(item.id))
    if (selectedItem.value && checkedIds.value.includes(selectedItem.value.id)) {
      selectedItem.value = null
      emit('select-item', null)
    }
    cancelDeleteMode()
  } catch (error) {
    console.error('다중 삭제 실패:', error)
  }
}

// 수정
function editItem(item) {
  item.title = item.title + ' (수정됨)'
  openMenuId.value = null
}
</script>

<template>
  <div class="list-section">
    <!-- 탭 -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab"
        :class="['tab-button', { active: currentTab === tab }]"
        @click="currentTab = tab"
      >
        {{ tab }}
      </button>
    </div>

    <!-- 검색창 -->
    <div class="search-bar">
      <input
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="검색어를 입력하세요"
      />
      <button class="search-button">🔍</button>
    </div>

    <!-- 액션 버튼 -->
    <div class="action-bar">
      <button class="action-button add" @click="openSearchModal" v-if="!isDeleteMode">+</button>
      <button class="action-button trash" @click="enterDeleteMode" v-if="!isDeleteMode">🗑</button>
      <button class="action-button delete-confirm" @click="deleteChecked" v-if="isDeleteMode">
        삭제
      </button>
      <button class="action-button cancel" @click="cancelDeleteMode" v-if="isDeleteMode">
        취소
      </button>
    </div>

    <!-- 리스트 -->
    <ul class="list">
      <li
        v-for="item in filteredItems"
        :key="item.id"
        :class="['list-item', { active: selectedItem?.id === item.id }]"
        @click="selectItem(item)"
      >
        <input
          v-if="isDeleteMode"
          type="checkbox"
          :checked="checkedIds.includes(item.id)"
          @click.stop="toggleCheck(item.id)"
          class="delete-checkbox"
        />
        <div class="item-info">
          <img :src="`/icons/${item.site}.png`" :alt="item.site" class="site-icon" />
          <span class="item-title">[{{ item.number }}] {{ item.title }}</span>
        </div>
        <button v-if="!isDeleteMode" class="menu-button" @click.stop="toggleMenu(item)">⋮</button>

        <!-- 드롭다운 메뉴 -->
        <div v-if="openMenuId === item.id" class="dropdown-menu">
          <button @click.stop="editItem(item)">수정</button>
          <button @click.stop="deleteItem(item)">삭제</button>
        </div>
      </li>
    </ul>

    <!-- 하단 설정 -->
    <div class="list-footer">
      <button class="setting-button">⚙</button>
    </div>
  </div>

  <!-- 검색 모달 -->
  <Teleport to="body">
    <!--검은색 배경(modal-overlay) 먼저 그리고, 검은색 배경을 클릭하면 closeSearchModal 호출-->
    <div v-if="isSearchModalOpen" class="modal-overlay" @click.self="closeSearchModal">
      <div class="modal-content">
        <h3 class="modal-title">문제 검색</h3>
        <div class="modal-search-bar">
          <input
            v-model="problemSearchQuery"
            type="text"
            class="modal-search-input"
            placeholder="문제 번호 또는 제목을 입력하세요"
            @keyup.enter="searchProblem"
          />
          <button class="modal-search-button" @click="searchProblem">검색</button>
        </div>
        <!-- 검색 결과 목록은 여기! modal-search-bar 바깥 -->
        <ul v-if="searchResults.length > 0" class="search-results">
          <li
            v-for="result in searchResults"
            :key="result.number"
            class="search-result-item"
            @click="selectSearchResult(result)"
          >
            <img :src="`/icons/${result.site}.png`" :alt="result.site" class="result-site-icon" />
            <span>[{{ result.number }}] {{ result.title }}</span>
            <span class="result-difficulty">{{ result.difficulty }}</span>
          </li>
        </ul>
        <p v-else-if="hasSearched" class="no-results">검색 결과가 없습니다.</p>
        <button class="modal-close-button" @click="closeSearchModal">취소</button>
        <p v-if="searchError" class="search-error">{{ searchError }}</p>
        <p v-if="duplicateMessage" class="search-error">{{ duplicateMessage }}</p>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
/* 검색 실패 시 메세지 */
.search-error {
  text-align: center;
  color: #d32f2f;
  font-size: 13px;
  background-color: #fde8e8;
  padding: 8px 12px;
  border-radius: 8px;
  margin-top: 8px;
}

/* 검색 결과 */
.search-results {
  list-style: none;
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
  font-size: 14px;
}

.search-result-item:last-child {
  border-bottom: none;
}

.search-result-item:hover {
  background-color: #f0f4ff;
}

.result-site-icon {
  width: 18px;
  height: 18px;
}

.result-difficulty {
  margin-left: auto;
  font-size: 12px;
  color: #888;
}

.no-results {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 16px 0;
}

/* 모달 */
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

.modal-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  width: 400px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.modal-title {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #333;
}

.modal-search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.modal-search-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.modal-search-input:focus {
  border-color: #1a56db;
}

.modal-search-button {
  padding: 10px 20px;
  background-color: #1a56db;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.modal-search-button:hover {
  background-color: #1544b5;
}

.modal-close-button {
  width: 100%;
  padding: 10px;
  background: none;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.modal-close-button:hover {
  background-color: #f5f5f5;
}

.list-section {
  width: 300px;
  border-left: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
}

.tab-bar {
  display: flex;
  border-bottom: 1px solid #e0e0e0;
}

.tab-button {
  flex: 1;
  padding: 10px 0;
  border: none;
  background: none;
  font-size: 14px;
  color: #888;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.tab-button.active {
  color: #1a56db;
  font-weight: 600;
  border-bottom: 2px solid #1a56db;
}

.search-bar {
  display: flex;
  padding: 10px 12px;
  gap: 6px;
}

.search-input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
}

.search-input:focus {
  border-color: #1a56db;
}

.search-button {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
}

.action-bar {
  display: flex;
  padding: 0 12px 8px;
  gap: 6px;
}

.action-button {
  width: 32px;
  height: 32px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-button.add:hover {
  background-color: #1a56db;
  color: white;
  border-color: #1a56db;
}

.action-button.trash:hover {
  background-color: #ff4d4d;
  color: white;
  border-color: #ff4d4d;
}

.action-button.delete-confirm {
  background-color: #ff4d4d;
  color: white;
  border-color: #ff4d4d;
  width: auto;
  padding: 0 12px;
  font-size: 13px;
}

.action-button.cancel {
  width: auto;
  padding: 0 12px;
  font-size: 13px;
}

.action-button.delete-confirm:hover {
  background-color: #e03e3e;
}

.action-button.cancel:hover {
  background-color: #e9ecef;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
  flex: 1;
  overflow-y: auto;
}

.list-item {
  position: relative;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
  color: #444;
  transition: background-color 0.15s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.list-item:hover {
  background-color: #e9ecef;
}

.list-item.active {
  background-color: #d0e3ff;
  color: #1a56db;
  font-weight: 600;
}

.delete-checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  flex-shrink: 0;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.site-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.item-title {
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-button {
  background: none;
  border: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 4px;
}

.menu-button:hover {
  color: #333;
}

.dropdown-menu {
  position: absolute;
  right: 12px;
  top: 100%;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
  overflow: hidden;
}

.dropdown-menu button {
  display: block;
  width: 100%;
  padding: 8px 20px;
  border: none;
  background: none;
  font-size: 13px;
  cursor: pointer;
  text-align: left;
}

.dropdown-menu button:hover {
  background-color: #f5f5f5;
}

.list-footer {
  padding: 10px 12px;
  border-top: 1px solid #e0e0e0;
}

.setting-button {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #888;
}

.setting-button:hover {
  color: #333;
}
</style>
