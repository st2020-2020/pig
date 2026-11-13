<template>
	<div class="layout-padding">
		<div class="layout-padding-auto layout-padding-view">
			<el-row class="ml10" v-show="showSearch">
				<el-form :inline="true" :model="state.queryForm" @keyup.enter="getDataList" ref="queryRef">
					<el-form-item :label="t('device.deviceName')" prop="deviceName">
						<el-input :placeholder="t('device.inputDeviceNameTip')" style="max-width: 180px" v-model="state.queryForm.deviceName" />
					</el-form-item>
					<el-form-item :label="t('device.deviceSerialNo')" prop="deviceSerialNo">
						<el-input :placeholder="t('device.inputDeviceSerialNoTip')" style="max-width: 180px" v-model="state.queryForm.deviceSerialNo" />
					</el-form-item>
					<el-form-item :label="t('device.onlineStatus')" prop="onlineStatus">
						<el-select :placeholder="t('device.selectOnlineStatusTip')" clearable style="max-width: 140px" v-model="state.queryForm.onlineStatus">
							<el-option :label="t('device.online')" value="1" />
							<el-option :label="t('device.offline')" value="0" />
						</el-select>
					</el-form-item>
					<el-form-item>
						<el-button @click="getDataList" icon="search" type="primary">
							{{ $t('common.queryBtn') }}
						</el-button>
						<el-button @click="resetQuery" icon="Refresh">{{ $t('common.resetBtn') }}</el-button>
					</el-form-item>
				</el-form>
			</el-row>
			<el-row>
				<div class="mb8" style="width: 100%">
					<el-button @click="formDialogRef.openDialog()" class="ml10" icon="folder-add" type="primary" v-auth="'sys_device_add'">
						{{ $t('common.addBtn') }}
					</el-button>
					<el-button plain :disabled="multiple" @click="handleDelete(selectObjs)" class="ml10" icon="Delete" type="primary" v-auth="'sys_device_del'">
						{{ $t('common.delBtn') }}
					</el-button>
					<right-toolbar
						@queryTable="getDataList"
						class="ml10"
						style="float: right; margin-right: 20px"
						v-model:showSearch="showSearch"
					></right-toolbar>
				</div>
			</el-row>
			<el-table
				:data="state.dataList"
				@selection-change="handleSelectionChange"
				style="width: 100%"
				v-loading="state.loading"
				border
				:cell-style="tableStyle.cellStyle"
				:header-cell-style="tableStyle.headerCellStyle"
			>
				<el-table-column align="center" type="selection" width="40" />
				<el-table-column :label="t('device.index')" type="index" width="60" />
				<el-table-column :label="t('device.deviceName')" prop="deviceName" min-width="140" show-overflow-tooltip />
				<el-table-column :label="t('device.deviceSerialNo')" prop="deviceSerialNo" min-width="150" show-overflow-tooltip />
				<el-table-column :label="t('device.deviceAddress')" prop="deviceAddress" min-width="160" show-overflow-tooltip />
				<el-table-column :label="t('device.firmwareVersion')" prop="firmwareVersion" min-width="120" show-overflow-tooltip />
				<el-table-column :label="t('device.onlineStatus')" prop="onlineStatus" width="120">
					<template #default="scope">
						<el-tag :type="scope.row.onlineStatus === '1' ? 'success' : 'info'">
							{{ scope.row.onlineStatus === '1' ? t('device.online') : t('device.offline') }}
						</el-tag>
					</template>
				</el-table-column>
				<el-table-column :label="t('device.createTime')" prop="createTime" width="180" show-overflow-tooltip />
				<el-table-column :label="$t('common.action')" width="200">
					<template #default="scope">
						<el-button icon="edit-pen" @click="formDialogRef.openDialog(scope.row.deviceId)" text type="primary" v-auth="'sys_device_edit'"
							>{{ $t('common.editBtn') }}
						</el-button>

						<el-button icon="delete" @click="handleDelete([scope.row.deviceId])" text type="primary" v-auth="'sys_device_del'"
							>{{ $t('common.delBtn') }}
						</el-button>
					</template>
				</el-table-column>
			</el-table>
			<pagination @current-change="currentChangeHandle" @size-change="sizeChangeHandle" v-bind="state.pagination" />
		</div>

		<!-- 编辑、新增  -->
		<form-dialog @refresh="getDataList()" ref="formDialogRef" />
	</div>
</template>

<script lang="ts" name="adminDevice" setup>
import { BasicTableProps, useTable } from '/@/hooks/table';
import { delObj, fetchList } from '/@/api/admin/device';
import { useMessage, useMessageBox } from '/@/hooks/message';
import { useI18n } from 'vue-i18n';

// 引入组件
const FormDialog = defineAsyncComponent(() => import('./form.vue'));
const { t } = useI18n();

// 定义变量内容
const formDialogRef = ref();
// 搜索变量
const queryRef = ref();
const showSearch = ref(true);
// 多选变量
const selectObjs = ref([]) as any;
const multiple = ref(true);

const state: BasicTableProps = reactive<BasicTableProps>({
	queryForm: {},
	pageList: fetchList,
});

//  table hook
const { getDataList, currentChangeHandle, sizeChangeHandle, tableStyle } = useTable(state);

// 清空搜索条件
const resetQuery = () => {
	queryRef.value.resetFields();
	getDataList();
};

// 多选事件
const handleSelectionChange = (objs: { deviceId: string }[]) => {
	selectObjs.value = objs.map(({ deviceId }) => deviceId);
	multiple.value = !objs.length;
};

// 删除操作
const handleDelete = async (ids: string[]) => {
	try {
		await useMessageBox().confirm(t('common.delConfirmText'));
	} catch {
		return;
	}

	try {
		await delObj(ids);
		getDataList();
		useMessage().success(t('common.delSuccessText'));
	} catch (err: any) {
		useMessage().error(err.msg);
	}
};
</script>
