<template>
	<div class="mt10 login-records">
		<el-form :inline="true" :model="state.queryForm" @keyup.enter="handleQuery" class="login-records-filter">
			<el-form-item :label="$t('personal.loginDateRange')" prop="dateRange">
				<el-date-picker
					v-model="state.queryForm.dateRange"
					:disabled-date="disabledDate"
					:end-placeholder="$t('personal.endDatePlaceholder')"
					:range-separator="$t('personal.dateRangeSeparator')"
					:start-placeholder="$t('personal.startDatePlaceholder')"
					type="daterange"
					value-format="YYYY-MM-DD"
				/>
			</el-form-item>
			<el-form-item>
				<el-button @click="handleQuery" :loading="state.loading" icon="Search" type="primary">
					{{ $t('common.queryBtn') }}
				</el-button>
				<el-button @click="handleReset" icon="Refresh">{{ $t('common.resetBtn') }}</el-button>
			</el-form-item>
		</el-form>
		<el-table
			:data="state.dataList"
			:cell-style="tableStyle.cellStyle"
			:header-cell-style="tableStyle.headerCellStyle"
			border
			class="personal-table"
			v-loading="state.loading"
		>
			<el-table-column :label="$t('personal.loginTime')" prop="createTime" width="170" />
			<el-table-column :label="$t('personal.ip')" prop="remoteAddr" width="130">
				<template #default="scope">{{ scope.row.remoteAddr || '-' }}</template>
			</el-table-column>
			<el-table-column :label="$t('personal.browser')" prop="browser" show-overflow-tooltip width="140">
				<template #default="scope">{{ scope.row.browser || $t('personal.browserUnknown') }}</template>
			</el-table-column>
			<el-table-column :label="$t('personal.result')" width="80">
				<template #default="scope">
					<el-tag :type="scope.row.result === 'success' ? 'success' : 'danger'">
						{{ scope.row.result === 'success' ? $t('personal.resultSuccess') : $t('personal.resultFailure') }}
					</el-tag>
				</template>
			</el-table-column>
			<template #empty>
				<div class="py-6 text-center">{{ $t('personal.noLoginRecords') }}</div>
			</template>
		</el-table>
		<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
	</div>
</template>

<script setup lang="ts" name="loginRecords">
import { BasicTableProps, useTable } from '/@/hooks/table';
import { pageList } from '/@/api/admin/loginRecord';

/**
 * 统计窗口：含今天在内的 30 个自然日，与服务端口径一致
 */
const WINDOW_DAYS = 30;

const state: BasicTableProps = reactive<BasicTableProps>({
	// 日期范围在封装层拆成 startDate / endDate：GET 参数会逐个 toString()，数组会变成 "a,b" 绑不上 LocalDate
	queryForm: { dateRange: [] },
	pageList: (params: any) => {
		const { dateRange, ...rest } = params;
		return pageList({ ...rest, startDate: dateRange?.[0], endDate: dateRange?.[1] });
	},
	descs: ['create_time'],
	pagination: {
		current: 1,
		size: 10,
		total: 0,
		pageSizes: [10, 20, 50],
	},
	createdIsNeed: true,
});

const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 查询：回到第 1 页；加载中重复点击只保留一次请求
const handleQuery = () => {
	if (state.loading) {
		return;
	}
	getDataList();
};

// 重置：清空日期范围，回到默认最近 30 天
const handleReset = () => {
	state.queryForm.dateRange = [];
	getDataList();
};

// 只允许选择最近 30 天的自然日，早于窗口的日期置灰不可选
const disabledDate = (date: Date) => {
	const today = new Date();
	const min = new Date(today.getFullYear(), today.getMonth(), today.getDate() - (WINDOW_DAYS - 1));
	const max = new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59, 999);
	return date.getTime() < min.getTime() || date.getTime() > max.getTime();
};
</script>

<style scoped lang="scss">
/* 与「API 密钥」标签里的表格保持一致：表头浅底、表体随主题 */
.personal-table {
	:deep(th.el-table__cell) {
		background-color: var(--el-table-header-bg-color) !important;
		color: var(--el-text-color-primary);
	}
}
</style>
