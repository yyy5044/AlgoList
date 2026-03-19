<script setup>
defineProps({
  selectedItem: Object
})
</script>

<template>
  <div class="detail-section">
    <div v-if="selectedItem" class="detail-content">
      <!-- 상단: 사이트 아이콘 + 문제 번호 + 제목 -->
      <div class="detail-header">
        <img :src="`/icons/${selectedItem.site}.png`" :alt="selectedItem.site" class="detail-site-icon" />
        <a :href="selectedItem.link" target="_blank" class="detail-title-link">
          <h2>[{{ selectedItem.number }}] {{ selectedItem.title }}</h2>
        </a>
      </div>

      <!-- 문제 정보 -->
      <div class="info-group">
        <div class="info-row">
          <span class="info-label">난이도</span>
          <span class="info-value">{{ selectedItem.difficulty }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">사이트</span>
          <span class="info-value">{{ selectedItem.site }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">알고리즘 분류</span>
          <div class="category-tags">
            <span v-for="cat in selectedItem.category" :key="cat" class="category-tag">
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
    </div>
    <div v-else class="detail-placeholder">
      <p>오른쪽 리스트에서 항목을 선택해주세요.</p>
    </div>
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
</style>