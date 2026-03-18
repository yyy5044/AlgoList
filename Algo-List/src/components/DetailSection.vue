<script setup>
defineProps({
  selectedItem: Object
})
</script>

<template>
  <div class="detail-section">
    <div v-if="selectedItem" class="detail-content">
      <h2>{{ selectedItem.title }}</h2>
      <p>{{ selectedItem.content }}</p>
    </div>
    <div v-else class="detail-placeholder">
      <p>오른쪽 리스트에서 항목을 선택해주세요.</p>
    </div>
  </div>
</template>

<style scoped>
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
</style>