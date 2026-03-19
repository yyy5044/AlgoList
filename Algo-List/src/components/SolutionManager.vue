<script setup>
defineProps({
  selectedItem: Object
})
</script>

<template>
  <div class="solution-manager">
    <h3>풀이 관리</h3>
    <p v-if="selectedItem">{{ selectedItem.title }}의 풀이 목록이 여기에 표시됩니다.</p>
  </div>
</template>

<style scoped>
.solution-manager {
  border-top: 1px solid #eee;
  padding: 20px 0;
}

.solution-manager h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 12px;
}
</style>