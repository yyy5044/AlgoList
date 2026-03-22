<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  selectedItem: Object
})

// 풀이 목록
const solutions = ref([])

// 업로드 폼 데이터
const showUploadForm = ref(false)
const algorithm = ref('')
const type = ref('FIRST')
const language = ref('java')

// 문제가 바뀔 때마다 풀이 목록 다시 가져오기
watch(() => props.selectedItem, async (newItem) => {
  if (newItem) {
    try {
      const response = await fetch(`http://localhost:8080/api/solutions/${newItem.id}`, {
        credentials: 'include' // 세션 쿠키
      })
      solutions.value = await response.json()
    } catch (error) {
      console.error('풀이 목록 조회 실패:', error)
    }
  } else {
    solutions.value = []
  }
})

// 파일 업로드
async function uploadFile(event) {
  const file = event.target.files[0]
  if (!file) return

  const code = await file.text()

  const solution = {
    algorithm: algorithm.value,
    type: type.value,
    language: language.value,
    code: code,
    problemId: props.selectedItem.id
  }

  try {
    const response = await fetch('http://localhost:8080/api/solutions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(solution),
      credentials: 'include' // 세션 쿠키
    })
    const saved = await response.json()
    solutions.value.push(saved)
    showUploadForm.value = false
    algorithm.value = ''
    type.value = 'FIRST'
    event.target.value = ''
  } catch (error) {
    console.error('업로드 실패:', error)
  }
}

// 풀이 삭제
async function deleteSolution(id) {
  try {
    await fetch(`http://localhost:8080/api/solutions/${id}`, {
      method: 'DELETE',
      credentials: 'include' // 세션 쿠키
    })
    solutions.value = solutions.value.filter(s => s.id !== id)
  } catch (error) {
    console.error('삭제 실패:', error)
  }
}
</script>

<template>
  <div class="solution-manager" v-if="selectedItem">
    <div class="solution-header">
      <h3>풀이 관리</h3>
      <button @click="showUploadForm = !showUploadForm">+ 업로드</button>
    </div>

    <!-- 업로드 폼 -->
    <div v-if="showUploadForm" class="upload-form">
      <div class="form-row">
        <label>주요 알고리즘</label>
        <input v-model="algorithm" placeholder="예: BFS, DFS, DP..." />
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
        <input type="file" @change="uploadFile" />
      </div>
    </div>

    <!-- 풀이 목록 -->
    <div v-if="solutions.length > 0" class="solution-list">
      <div v-for="sol in solutions" :key="sol.id" class="solution-item">
        <span class="solution-info">
          [{{ sol.type }}] {{ sol.algorithm }} ({{ sol.language }})
        </span>
        <button class="delete-btn" @click="deleteSolution(sol.id)">삭제</button>
      </div>
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

.solution-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.solution-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-radius: 6px;
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
</style>
