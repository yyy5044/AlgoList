<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['select-item'])

// 탭
const tabs = ['문제', '분류', 'Git']
const currentTab = ref('문제')

// 검색
const searchQuery = ref('')

// 리스트 데이터
const items = ref([
  {
    id: 1,
    // 외부 API에서 받는 데이터
    title: '게리맨더링',
    number: '17471',
    difficulty: 'gold3',
    category: ['수학', '그래프 이론', '그래프 탐색', 'BFS'],
    site: 'BOJ',
    link: 'https://www.acmicpc.net/problem/17471',
    // 내부에서 관리하는 데이터
    grade: 'RED',
    solveCount: 3,
    lastSolvedDate: '2026-03-19',
  },
  {
    id: 2,
    title: '활주로 건설',
    number: '4014',
    difficulty: 'gold3',
    category: ['DP', '시뮬레이션'],
    site: 'SWEA',
    link: 'https://swexpertacademy.com/main/code/problem/problemDetail.do?contestProbId=AWIeW7FakkUDFAVH',
    grade: 'GREEN',
    solveCount: 1,
    lastSolvedDate: '2026-03-15',
  },
])

// 선택 / 메뉴
const selectedItem = ref(null)
const openMenuId = ref(null)

function selectItem(item) {
  selectedItem.value = item
  openMenuId.value = null
  emit('select-item', item)
}

function toggleMenu(item) {
  openMenuId.value = openMenuId.value === item.id ? null : item.id
}

// 검색 필터
const filteredItems = computed(() => {
  if (!searchQuery.value) return items.value
  const query = searchQuery.value.toLowerCase()
  return items.value.filter(item =>
    item.title.toLowerCase().includes(query) ||
    item.number.includes(query) ||
    item.category.some(cat => cat.toLowerCase().includes(query))
  )
})

// 요소 추가 로직
// 1. 문제 검색 
// 검색 모달
const isSearchModalOpen = ref(false)
const problemSearchQuery = ref('')

function openSearchModal() {
  isSearchModalOpen.value = true
  problemSearchQuery.value = ''
}

function closeSearchModal() {
  isSearchModalOpen.value = false
  problemSearchQuery.value = ''
}


// 개별 삭제
function deleteItem(item) {
  items.value = items.value.filter(i => i.id !== item.id)
  if (selectedItem.value?.id === item.id) {
    selectedItem.value = null
    emit('select-item', null)
  }
  openMenuId.value = null
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
    checkedIds.value = checkedIds.value.filter(i => i !== id)
  } else {
    checkedIds.value.push(id)
  }
}

function deleteChecked() {
  items.value = items.value.filter(item => !checkedIds.value.includes(item.id))
  if (selectedItem.value && checkedIds.value.includes(selectedItem.value.id)) {
    selectedItem.value = null
    emit('select-item', null)
  }
  cancelDeleteMode()
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
      <button class="action-button delete-confirm" @click="deleteChecked" v-if="isDeleteMode">삭제</button>
      <button class="action-button cancel" @click="cancelDeleteMode" v-if="isDeleteMode">취소</button>
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
  <Teleport to ="body">
    <div v-if="isSearchModalOpen" class="modal-overlay" @click.self="closeSearchModal">
      <div class="modal-content">
        <h3 class="modal-title">문제 검색</h3>
        <div class="modal-search-bar">
          <input
            v-model="problemSearchQuery"
            type="text"
            class="modal-search-input"
            placeholder="문제 번호 또는 제목을 입력하세요"
          />
          <button class="modal-search-button">검색</button>
        </div>
        <button class="modal-close-button" @click="closeSearchModal">취소</button>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
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