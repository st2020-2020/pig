<template>
	<el-drawer v-model="visible" direction="rtl" size="40%" :title="t('home.historyNotice')">
		<el-table
			:data="state.dataList"
			:cell-style="tableStyle.cellStyle"
			:header-cell-style="tableStyle.headerCellStyle"
			border
			v-loading="state.loading"
			@cell-click="handleCellClick"
		>
			<el-table-column type="index" :label="t('home.index')" width="60" />
			<el-table-column prop="title" :label="t('home.newsTitle')" show-overflow-tooltip />
			<el-table-column :label="t('home.newsStatus')" width="100">
				<template #default="scope">
					<el-tag type="info" v-if="scope.row.status === '2'">{{ t('notice.offline') }}</el-tag>
				</template>
			</el-table-column>
			<el-table-column :label="t('home.historyNoticePublishTime')" width="170">
				<template #default="scope">{{ scope.row.publishTime || '—' }}</template>
			</el-table-column>
			<template #empty>
				<div class="py-6 text-center">
					<span v-if="!state.loading">{{ t('home.historyNoticeEmpty') }}</span>
				</div>
			</template>
		</el-table>
		<pagination @size-change="sizeChangeHandle" @current-change="currentChangeHandle" v-bind="state.pagination" />

		<el-dialog v-model="detailVisible" :title="t('home.historyNotice')" width="640px">
			<div class="mx-auto max-w-[640px]">
				<h1 class="mb-4 text-2xl font-bold leading-tight">{{ currentNotice?.title }}</h1>
				<p class="mb-6 text-sm text-[var(--el-text-color-secondary)]">{{ currentNotice?.publishTime || '—' }}</p>
				<div class="mb-6 border-t border-[var(--el-border-color-lighter)]"></div>
				<div class="whitespace-pre-line break-words">{{ currentNotice?.content }}</div>
			</div>
		</el-dialog>
	</el-drawer>
</template>

<script setup lang="ts" name="homeNoticeHistory">
import { BasicTableProps, useTable } from '/@/hooks/table';
import { fetchHistoryPage } from '/@/api/admin/notice';
import { useMessage } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const visible = ref(false);
const detailVisible = ref(false);
const currentNotice = ref<any>(null);

// 出错时清空并落到空态，只弹一次提示；不把错误再抛给 useTable，避免弹两次
async function loadHistory(params: any) {
	try {
		return await fetchHistoryPage(params);
	} catch {
		useMessage().error(t('home.historyNoticeFetchError'));
		return { data: { records: [], total: 0 } };
	}
}

const state: BasicTableProps = reactive<BasicTableProps>({
	queryForm: {},
	pageList: loadHistory,
	createdIsNeed: false,
	pagination: {
		current: 1,
		size: 10,
		total: 0,
		pageSizes: [10, 20, 50],
	},
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 每次打开抽屉都回到第 1 页，避免停在旧页码
const open = () => {
	visible.value = true;
	getDataList();
};

const handleCellClick = (row: any) => {
	currentNotice.value = row;
	detailVisible.value = true;
};

defineExpose({
	open,
});
</script>
