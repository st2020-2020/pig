<template>
	<history ref="historyRef" />
	<div class="notice-bar mx-3 mt-3" role="status" v-if="notice || loaded">
		<template v-if="notice">
			<div class="notice-bar-main">
				<div class="flex items-center gap-2 min-w-0">
					<el-icon class="text-primary flex-shrink-0"><MuteNotification /></el-icon>
					<span class="text-[15px] font-semibold truncate">{{ notice.title }}</span>
				</div>
				<p class="mt-1 text-sm text-[var(--el-text-color-regular)] whitespace-pre-line line-clamp-3">{{ notice.content }}</p>
			</div>
			<div class="flex items-center flex-shrink-0">
				<el-button text type="primary" class="text-[15px]" @click="historyRef.open()">{{ t('home.historyNotice') }}</el-button>
				<el-button class="notice-bar-close" text circle size="small" :aria-label="t('notice.close')" @click="handleClose">
					<el-icon><Close /></el-icon>
				</el-button>
			</div>
		</template>
		<!-- 没有可显示的公告条内容（没发过、或这条已被本机关闭）时只留入口，历史公告仍可打开 -->
		<div v-else class="flex justify-end w-full">
			<el-button text type="primary" class="text-[15px]" @click="historyRef.open()">{{ t('home.historyNotice') }}</el-button>
		</div>
	</div>
</template>

<script setup lang="ts" name="homeNoticeBar">
import { fetchLatest } from '/@/api/admin/notice';
import { Local } from '/@/utils/storage';
import { useI18n } from 'vue-i18n';
import History from './history.vue';

const { t } = useI18n();

// 关闭记录只存在本机，key 里带上 noticeId，换一条新公告会重新出现
const closedKey = (noticeId: string) => `notice-closed-${noticeId}`;

const historyRef = ref();

const notice = ref<{ noticeId: string; title: string; content: string } | null>(null);

// 首次加载完成前不渲染，避免首页先闪一条只有入口的行
const loaded = ref(false);

// 加载最新一条已发布公告：拿不到对象或这条已被关闭时不渲染公告条内容
const loadLatest = async () => {
	try {
		const { data } = await fetchLatest();
		if (!data || !data.noticeId || Local.get(closedKey(data.noticeId))) {
			return;
		}
		notice.value = { noticeId: data.noticeId, title: data.title, content: data.content };
	} catch {
		// 公告是附加信息，接口失败静默不渲染，不打扰首页其它 widget
	} finally {
		loaded.value = true;
	}
};

// 关闭：立即隐藏并记住这条，刷新页面不再出现
const handleClose = () => {
	const noticeId = notice.value?.noticeId;
	notice.value = null;
	if (noticeId) {
		Local.set(closedKey(noticeId), Date.now());
	}
};

onMounted(() => {
	loadLatest();
});
</script>

<style scoped lang="scss">
.notice-bar {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 12px;
	padding: 12px 16px;
	background: var(--el-bg-color-overlay);
	border: 1px solid var(--el-border-color-lighter);
	border-radius: 12px;
	margin-bottom: 12px;
	position: relative;
}

.notice-bar-main {
	flex: 1;
	min-width: 0;
}

.notice-bar-close {
	flex-shrink: 0;
}
</style>
