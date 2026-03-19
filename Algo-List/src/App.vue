<script setup>
import { ref } from 'vue'
import DetailSection from './components/DetailSection.vue'
import ListSection from './components/ListSection.vue'

const selectedItem = ref(null)

function onSelectItem(item) {
  selectedItem.value = item
}
</script>

<template>
  <div class="outer-container">
    <div class="app-container">
      <DetailSection :selectedItem="selectedItem" />
      <ListSection @select-item="onSelectItem" />
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
  width: 90vw;
  height: 85vh;
  max-width: 1500px;
  max-height: 1000px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}
</style>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  line-height: 1.4;
}
</style>
