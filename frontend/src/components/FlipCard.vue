<template>
  <div class="flip-card" @click="emit('flip')">
    <div class="flip-card-inner" :class="{ flipped: flipped }">
      <div class="flip-card-face flip-card-front" :style="frontStyle">
        <slot name="front" />
      </div>
      <div class="flip-card-face flip-card-back" :style="backStyle">
        <slot name="back" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  flipped: boolean
  frontStyle?: Record<string, string>
  backStyle?: Record<string, string>
}>()
const emit = defineEmits<{ (e: 'flip'): void }>()
</script>

<style scoped>
.flip-card {
  cursor: pointer;
}
.flip-card-face {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}
</style>
