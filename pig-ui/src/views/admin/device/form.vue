<template>
	<el-dialog
		:title="form.deviceId ? $t('common.editBtn') : $t('common.addBtn')"
		v-model="visible"
		width="600"
		:close-on-click-modal="false"
		draggable
	>
		<el-form ref="dataFormRef" :model="form" :rules="dataRules" label-width="90px" v-loading="loading">
			<el-form-item :label="t('device.deviceName')" prop="deviceName">
				<el-input v-model="form.deviceName" :placeholder="t('device.inputDeviceNameTip')" maxlength="100" />
			</el-form-item>
			<el-form-item :label="t('device.deviceSerialNo')" prop="deviceSerialNo">
				<el-input v-model="form.deviceSerialNo" :placeholder="t('device.inputDeviceSerialNoTip')" maxlength="100" />
			</el-form-item>
			<el-form-item :label="t('device.deviceAddress')" prop="deviceAddress">
				<el-input v-model="form.deviceAddress" :placeholder="t('device.inputDeviceAddressTip')" maxlength="255" />
			</el-form-item>
			<el-form-item :label="t('device.firmwareVersion')" prop="firmwareVersion">
				<el-input v-model="form.firmwareVersion" :placeholder="t('device.inputFirmwareVersionTip')" maxlength="64" />
			</el-form-item>
			<el-form-item :label="t('device.onlineStatus')" prop="onlineStatus">
				<el-radio-group v-model="form.onlineStatus">
					<el-radio border value="1">{{ t('device.online') }}</el-radio>
					<el-radio border value="0">{{ t('device.offline') }}</el-radio>
				</el-radio-group>
			</el-form-item>
		</el-form>
		<template #footer>
			<span class="dialog-footer">
				<el-button @click="visible = false">{{ $t('common.cancelButtonText') }}</el-button>
				<el-button type="primary" @click="onSubmit" :disabled="loading">{{ $t('common.confirmButtonText') }}</el-button>
			</span>
		</template>
	</el-dialog>
</template>

<script setup lang="ts" name="adminDeviceDialog">
import { useMessage } from '/@/hooks/message';
import { getObj, addObj, putObj } from '/@/api/admin/device';
import { useI18n } from 'vue-i18n';

// 定义子组件向父组件传值/事件
const emit = defineEmits(['refresh']);

const { t } = useI18n();

// 定义变量内容
const dataFormRef = ref();
const visible = ref(false);
const loading = ref(false);

// 提交表单数据
const form = reactive({
	deviceId: '',
	deviceName: '',
	deviceSerialNo: '',
	deviceAddress: '',
	firmwareVersion: '',
	onlineStatus: '0',
});

// 定义校验规则
const dataRules = computed(() => ({
	deviceName: [{ required: true, message: t('device.deviceNameRequired'), trigger: 'blur' }],
	deviceSerialNo: [{ required: true, message: t('device.deviceSerialNoRequired'), trigger: 'blur' }],
	onlineStatus: [{ required: true, message: t('device.onlineStatusRequired'), trigger: 'blur' }],
}));

// 打开弹窗
const openDialog = async (id: string) => {
	visible.value = true;
	form.deviceId = '';

	// 重置表单数据
	await nextTick();
	dataFormRef.value?.resetFields();

	// 获取设备信息
	if (id) {
		form.deviceId = id;
		await getDeviceData(id);
	}
};

// 提交
const onSubmit = async () => {
	// 立即设置 loading，防止重复点击
	if (loading.value) return;
	loading.value = true;

	try {
		const valid = await dataFormRef.value.validate().catch(() => {});
		if (!valid) {
			loading.value = false;
			return false;
		}

		form.deviceId ? await putObj(form) : await addObj(form);
		useMessage().success(t(form.deviceId ? 'common.editSuccessText' : 'common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

// 初始化设备数据
const getDeviceData = async (id: string) => {
	try {
		const { data } = await getObj(id);
		// 只回填表单可编辑字段，避免把添加时间等审计字段带进提交体
		form.deviceName = data?.deviceName ?? '';
		form.deviceSerialNo = data?.deviceSerialNo ?? '';
		form.deviceAddress = data?.deviceAddress ?? '';
		form.firmwareVersion = data?.firmwareVersion ?? '';
		form.onlineStatus = data?.onlineStatus ?? '0';
	} catch (err: any) {
		useMessage().error(err.msg || t('device.fetchDeviceDataError'));
	}
};

// 暴露变量
defineExpose({
	openDialog,
});
</script>
