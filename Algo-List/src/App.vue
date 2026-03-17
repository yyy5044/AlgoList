<script setup>
import { ref } from 'vue'

const items = ref([])

// 요소 선택 로직
const selectedItem = ref(null)

function selectItem(item) {
  selectedItem.value = item
}


// 요소 추가 함수
function addItem() {
  const maxId = items.value.length > 0
    ? Math.max(...items.value.map(item => item.id))
    : 0
  const newItem = {
    id: maxId + 1,
    title: `항목 ${maxId + 1}`,
    content: `${maxId + 1}번째 항목의 세부 내용입니다.`,
  }
  items.value.push(newItem)
}

// 요소 삭제 함수
function deleteItem(item) {
  items.value = items.value.filter(i => i.id !== item.id)
  if (selectedItem.value?.id === item.id) {
    selectedItem.value = null
  }
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
        <div class="list-header">
          <h3 class="list-title">리스트</h3>
          <button class="add-button" @click="addItem">+</button>
        </div>
        <ul class="list">
          <li
            v-for="item in items"
            :key="item.id"
            :class="['list-item', { active: selectedItem?.id === item.id }]"
            @click="selectItem(item)"
          >
            <span>{{ item.title }}</span>
            <button class="delete-button" @click.stop="deleteItem(item)">×</button>
          </li>
        </ul>
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
  padding: 16px 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  margin-bottom: 12px;
}

.list-title {
  margin: 0;
  padding: 0;
  font-size: 18px;
  color: #333;
}

.add-button {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background-color: #1a56db;
  color: white;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
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

.delete-button {
  background: none;
  border: none;
  color: #999;
  font-size: 16px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.delete-button:hover {
  background-color: #ff4d4d;
  color: white;
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