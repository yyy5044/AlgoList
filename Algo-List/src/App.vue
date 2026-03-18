<script setup>
import { ref, computed } from 'vue'

// 탭
const tabs = ['문제', '분류', 'Git']
const currentTab = ref('문제')

// 검색
const searchQuery = ref('')

// 리스트 데이터
const items = ref([
  { id: 1, category: ['BFS'], number: '1941', title: '소문난 칠공주', },
  { id: 2, category: ['DP', '시뮬레이션'], number: '4014', title: '활주로 건설', },
  { id: 3, category: ['조합', 'BFS'], number: '2309', title: '번데기 먹기', },
])

// 선택 / 메뉴
const selectedItem = ref(null)
const openMenuId = ref(null)

function selectItem(item) {
  selectedItem.value = item
  openMenuId.value = null
}

// 등호 세 개면 타입까지 비교
// 이미 열려있는 메뉴를 다시 누르면 null이 할당되어서 닫힘
function toggleMenu(item) { 
  openMenuId.value = openMenuId.value === item.id ? null : item.id
} 


// 검색 필터
const filteredItems = computed(() => { // 람다식으로 계산 방식 지정
  if (!searchQuery.value) return items.value // 검색어 없으면 리스트 그대로 반환
  const query = searchQuery.value.toLowerCase() // 검색어 소문자로 변환
  return items.value.filter(item => // .filter로 조건에 맞는 리스트 반환
    item.title.toLowerCase().includes(query) ||
    item.number.includes(query) ||
    item.category.some(cat => cat.toLowerCase().includes(query)) // 분류 하나라도 포함하면 전부 잡기
  )
})

// 추가
function addItem() {
  const maxId = items.value.length > 0 // 배열이 비어있으면 maxId는 0, 배열이 있으면 가장 큰 id를 maxId에 할당
    ? Math.max(...items.value.map(item => item.id)) // map으로 문제들의 id만 가지고 새로운 배열 만들고, 그중에서 최대값 찾기
    : 0 
  const newItem = { // 요소 추가 시 id는 maxId + 1
    id: maxId + 1,
    category: ['미분류'],
    number: '0000',
    title: `새 문제`,
  }
  items.value.push(newItem)
}

// 개별 삭제
function deleteItem(item) {
  items.value = items.value.filter(i => i.id !== item.id)
  if (selectedItem.value?.id === item.id) {
    selectedItem.value = null
  }
  openMenuId.value = null
}

// 다중 삭제 (현재는 선택된 항목 삭제)
function deleteSelected() {
  if (selectedItem.value) {
    deleteItem(selectedItem.value)
  }
}

// 수정 (일단 제목 변경으로 간단히)
function editItem(item) {
  item.title = item.title + ' (수정됨)'
  openMenuId.value = null
}

</script>

<template>
  <div class="outer-container">
    <div class="app-container">
      <!-- 왼쪽: 세부 내용 -->
      <div class="detail-section">
        <div v-if="selectedItem" class="detail-content">
          <h2>{{ selectedItem.title }}</h2>
          <p>{{ selectedItem.content }}</p>
        </div>
        <div v-else class="detail-placeholder">
          <p>오른쪽 리스트에서 항목을 선택해주세요.</p>
        </div>
      </div>

      <!-- 오른쪽: 리스트 -->
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

        <!-- 액션 버튼 (추가 / 다중삭제) -->
        <div class="action-bar">
          <button class="action-button add" @click="addItem">+</button>
          <button class="action-button trash" @click="deleteSelected">🗑</button>
        </div>

        <!-- 리스트 -->
        <ul class="list">
          <li
            v-for="item in filteredItems"
            :key="item.id"
            :class="['list-item', { active: selectedItem?.id === item.id }]"
            @click="selectItem(item)"
          >
            <div class="item-info">
              <span
                v-for="cat in item.category"
                :key="cat"
                class="item-category"
              >
                {{ cat }}
              </span>
              <span class="item-title">[{{ item.number }}] {{ item.title }}</span>
            </div>
            <button class="menu-button" @click.stop="toggleMenu(item)">⋮</button>

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
    </div>
  </div>
</template>

<style scoped>
.outer-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #e9ecef;
}

.app-container {
  display: flex;
  width: 1500px;
  height: 1000px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 왼쪽 세부 내용 영역 */
.detail-section {
  flex: 1;
  padding: 32px;
  background-color: #ffffff;
}

.detail-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 16px;
}

.detail-content h2 {
  margin: 0 0 16px 0;
  font-size: 24px;
  color: #333;
}

.detail-content p {
  font-size: 16px;
  color: #555;
  line-height: 1.6;
}

/* 오른쪽 리스트 영역 */
.list-section {
  width: 300px;
  border-left: 1px solid #e0e0e0;
  background-color: #f8f9fa;
  display: flex;
  flex-direction: column;
}

.add-button:hover {
  background-color: #1544b5;
}

.list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.list-item {
  position: relative;
  padding: 14px 20px;
  cursor: pointer;
  border-bottom: 1px solid #eee;
  font-size: 15px;
  color: #444;
  transition: background-color 0.15s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.list-item:hover {
  background-color: #e9ecef;
}

.list-item.active {
  background-color: #d0e3ff;
  color: #1a56db;
  font-weight: 600;
}

.delete-button:hover {
  background-color: #ff4d4d;
  color: white;
}

/* 탭 */
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

/* 검색 */
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

/* 액션 버튼 */
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

/* 리스트 항목 */
.item-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.item-category {
  font-size: 11px;
  color: #1a56db;
  background-color: #e8f0fe;
  padding: 2px 8px;
  border-radius: 10px;
  width: fit-content;
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

/* 드롭다운 메뉴 */
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

/* 하단 설정 */
.list-footer {
  padding: 10px 12px;
  border-top: 1px solid #e0e0e0;
  margin-top: auto;
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


<style>
/* 전역 스타일(페이지 전체에 적용) */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}
</style>