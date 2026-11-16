<template>
  <div>
    <notice-bar/>
    <widgets/>
    <notice v-model:show="showNotice"/>
  </div>
</template>

<script setup lang="ts" name="dashboard">
const NoticeBar = defineAsyncComponent(() => import('./notice-bar/index.vue'));
const Widgets = defineAsyncComponent(() => import('./widgets/index.vue'));
const Notice = defineAsyncComponent(() => import('./notice/index.vue'));
// 演示环境
const isPreview = import.meta.env.VITE_PREVIEW_MODEL === 'true';
const showNotice = ref(isPreview);
</script>
