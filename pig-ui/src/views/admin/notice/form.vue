<template>
	<el-dialog
		:title="form.noticeId ? $t('common.editBtn') : $t('common.addBtn')"
		v-model="visible"
		width="600"
		:close-on-click-modal="false"
		draggable
	>
		<el-form ref="dataFormRef" :model="form" :rules="dataRules" label-width="90px" v-loading="loading">
			<el-form-item :label="t('notice.title')" prop="title">
				<el-input v-model="form.title" :placeholder="t('notice.inputTitleTip')" maxlength="100" show-word-limit />
			</el-form-item>
			<el-form-item :label="t('notice.status')" prop="status" required>
				<el-radio-group v-model="form.status">
					<el-radio border value="0">{{ t('notice.draft') }}</el-radio>
					<el-radio border value="1">{{ t('notice.published') }}</el-radio>
					<el-radio border value="2">{{ t('notice.offline') }}</el-radio>
				</el-radio-group>
				<div class="text-xs text-[var(--el-text-color-secondary)] mt-1" v-if="form.status === '1'">
					{{ t('notice.publishTip') }}
				</div>
			</el-form-item>
			<el-form-item :label="t('notice.content')" prop="content">
				<el-input v-model="form.content" type="textarea" :rows="8" :placeholder="t('notice.inputContentTip')" maxlength="2000" show-word-limit />
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

<script setup lang="ts" name="adminNoticeDialog">
import { useMessage } from '/@/hooks/message';
import { getObj, addObj, putObj } from '/@/api/admin/notice';
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
	noticeId: '',
	title: '',
	content: '',
	status: '0',
});

// 纯空格也算空：后端的 @NotBlank 会拒绝，前端先一步拦下来不发请求
const trimmedNotBlank = (message: string) => ({
	validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
		// 空值交给 required 规则，这里只补「有值但全是空白」的情况，避免同一字段出现两条相同提示
		if (value && !String(value).trim()) {
			callback(new Error(message));
			return;
		}
		callback();
	},
	trigger: 'blur',
});

// 定义校验规则
const dataRules = computed(() => ({
	title: [
		{ required: true, message: t('notice.titleRequired'), trigger: 'blur' },
		trimmedNotBlank(t('notice.titleRequired')),
		{ max: 100, message: t('notice.titleMaxLength'), trigger: 'blur' },
	],
	content: [
		{ required: true, message: t('notice.contentRequired'), trigger: 'blur' },
		trimmedNotBlank(t('notice.contentRequired')),
		{ max: 2000, message: t('notice.contentMaxLength'), trigger: 'blur' },
	],
}));

// 打开弹窗
const openDialog = async (id: string) => {
	visible.value = true;
	form.noticeId = '';
	form.status = '0';

	// 重置表单数据
	await nextTick();
	dataFormRef.value?.resetFields();

	// 获取公告信息
	if (id) {
		form.noticeId = id;
		await getNoticeData(id);
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

		form.noticeId ? await putObj(form) : await addObj(form);
		useMessage().success(t(form.noticeId ? 'common.editSuccessText' : 'common.addSuccessText'));
		visible.value = false;
		emit('refresh');
	} catch (err: any) {
		useMessage().error(err.msg);
	} finally {
		loading.value = false;
	}
};

// 初始化公告数据
const getNoticeData = async (id: string) => {
	try {
		const { data } = await getObj(id);
		// 只回填表单可编辑字段，避免把发布时间、添加时间等带进提交体
		form.title = data?.title ?? '';
		form.content = data?.content ?? '';
		form.status = data?.status ?? '0';
	} catch (err: any) {
		useMessage().error(err.msg || t('notice.fetchNoticeDataError'));
	}
};

// 暴露变量
defineExpose({
	openDialog,
});
</script>
