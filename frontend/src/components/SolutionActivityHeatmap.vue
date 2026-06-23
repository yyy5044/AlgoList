<script setup>
import { computed } from 'vue'

const props = defineProps({
  activity: {
    type: Object,
    default: () => ({
      startDate: '',
      endDate: '',
      days: [],
    }),
  },
  isLoading: {
    type: Boolean,
    default: false,
  },
  errorMessage: {
    type: String,
    default: '',
  },
})

const weekdayLabels = ['', '월', '', '수', '', '금', '']
const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월']

const activityDays = computed(() => props.activity?.days || [])
const hasActivityRange = computed(() => props.activity?.startDate && props.activity?.endDate)
const totalCount = computed(() => activityDays.value.reduce((total, day) => total + normalizeCount(day.count), 0))
const activeDayCount = computed(() => activityDays.value.filter((day) => normalizeCount(day.count) > 0).length)

const countByDate = computed(() => {
  return new Map(activityDays.value.map((day) => [day.date, normalizeCount(day.count)]))
})

const cells = computed(() => {
  if (!hasActivityRange.value) return []

  const startDate = parseDate(props.activity.startDate)
  const endDate = parseDate(props.activity.endDate)
  const firstCellDate = addDays(startDate, -startDate.getDay())
  const lastCellDate = addDays(endDate, 6 - endDate.getDay())
  const result = []

  for (let cursor = firstCellDate; cursor <= lastCellDate; cursor = addDays(cursor, 1)) {
    const date = formatDate(cursor)
    const inRange = cursor >= startDate && cursor <= endDate
    const count = inRange ? countByDate.value.get(date) || 0 : 0

    result.push({
      date,
      count,
      inRange,
      level: inRange ? getLevel(count) : 0,
    })
  }

  return result
})

const weeks = computed(() => {
  const result = []

  for (let index = 0; index < cells.value.length; index += 7) {
    result.push(cells.value.slice(index, index + 7))
  }

  return result
})

const monthLabels = computed(() => {
  const labels = []
  let lastMonth = -1

  weeks.value.forEach((week, weekIndex) => {
    const firstInRangeDay = week.find((day) => day.inRange)
    if (!firstInRangeDay) return

    const date = parseDate(firstInRangeDay.date)
    const month = date.getMonth()

    if (month !== lastMonth) {
      labels.push({
        key: `${date.getFullYear()}-${month}`,
        text: monthNames[month],
        weekIndex,
      })
      lastMonth = month
    }
  })

  return labels
})

function normalizeCount(value) {
  const count = Number(value)
  return Number.isFinite(count) && count > 0 ? count : 0
}

function parseDate(value) {
  const [year, month, date] = value.split('-').map(Number)
  return new Date(year, month - 1, date)
}

function addDays(date, days) {
  const nextDate = new Date(date)
  nextDate.setDate(nextDate.getDate() + days)
  return nextDate
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getLevel(count) {
  if (count === 0) return 0
  if (count <= 1) return 1
  if (count <= 3) return 2
  if (count <= 6) return 3
  return 4
}

function getTooltip(day) {
  return `${day.date}: ${day.count}회 제출`
}
</script>

<template>
  <section class="activity-section" aria-label="풀이 잔디">
    <div class="activity-header">
      <div>
        <h3>풀이 잔디</h3>
        <p>최근 1년 동안 {{ totalCount }}회 제출</p>
      </div>
      <span class="activity-badge">{{ activeDayCount }}일</span>
    </div>

    <p v-if="isLoading" class="activity-status">풀이 기록을 불러오는 중입니다.</p>
    <p v-else-if="errorMessage" class="activity-error">{{ errorMessage }}</p>
    <div v-else-if="hasActivityRange" class="activity-scroll">
      <div class="activity-grid" :style="{ '--week-count': weeks.length }">
        <div class="month-labels" aria-hidden="true">
          <span
            v-for="label in monthLabels"
            :key="label.key"
            class="month-label"
            :style="{ gridColumn: `${label.weekIndex + 1}` }"
          >
            {{ label.text }}
          </span>
        </div>

        <div class="weekday-labels" aria-hidden="true">
          <span v-for="(label, index) in weekdayLabels" :key="index">{{ label }}</span>
        </div>

        <div class="heatmap-weeks">
          <div v-for="(week, weekIndex) in weeks" :key="weekIndex" class="heatmap-week">
            <span
              v-for="day in week"
              :key="day.date"
              :class="['activity-cell', `level-${day.level}`, { muted: !day.inRange }]"
              :title="day.inRange ? getTooltip(day) : ''"
              :aria-label="day.inRange ? getTooltip(day) : undefined"
            />
          </div>
        </div>
      </div>
    </div>
    <p v-else class="activity-status">표시할 풀이 기록이 없습니다.</p>
  </section>
</template>

<style scoped>
.activity-section {
  padding: 16px;
  margin-bottom: 18px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fff;
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.activity-header h3 {
  margin: 0 0 4px;
  color: #333;
  font-size: 16px;
}

.activity-header p {
  margin: 0;
  color: #777;
  font-size: 13px;
}

.activity-badge {
  flex: 0 0 auto;
  min-width: 56px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #f1f8f4;
  color: #2f7d45;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.activity-status,
.activity-error {
  margin: 0;
  font-size: 13px;
}

.activity-status {
  color: #888;
}

.activity-error {
  color: #e74c3c;
}

.activity-scroll {
  overflow-x: auto;
  padding-bottom: 4px;
}

.activity-grid {
  display: inline-grid;
  grid-template-columns: 24px auto;
  grid-template-rows: 16px auto;
  gap: 4px;
  min-width: max-content;
}

.month-labels {
  display: grid;
  grid-column: 2;
  grid-row: 1;
  grid-template-columns: repeat(var(--week-count), 12px);
  column-gap: 3px;
  align-items: center;
}

.month-label {
  color: #777;
  font-size: 11px;
  line-height: 1;
  white-space: nowrap;
}

.weekday-labels {
  display: grid;
  grid-column: 1;
  grid-row: 2;
  grid-template-rows: repeat(7, 12px);
  row-gap: 3px;
  color: #777;
  font-size: 11px;
  line-height: 12px;
}

.heatmap-weeks {
  display: grid;
  grid-column: 2;
  grid-row: 2;
  grid-template-columns: repeat(var(--week-count), 12px);
  column-gap: 3px;
}

.heatmap-week {
  display: grid;
  grid-template-rows: repeat(7, 12px);
  row-gap: 3px;
}

.activity-cell {
  width: 12px;
  height: 12px;
  border: 1px solid rgba(27, 31, 36, 0.06);
  border-radius: 2px;
}

.activity-cell.muted {
  visibility: hidden;
}

.level-0 {
  background: #ebedf0;
}

.level-1 {
  background: #9be9a8;
}

.level-2 {
  background: #40c463;
}

.level-3 {
  background: #30a14e;
}

.level-4 {
  background: #216e39;
}

@media (max-width: 640px) {
  .activity-header {
    flex-direction: column;
  }

  .activity-badge {
    min-width: 0;
  }
}
</style>
