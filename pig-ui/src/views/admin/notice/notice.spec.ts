import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const noticePage = readFileSync(fileURLToPath(new URL('./index.vue', import.meta.url)), 'utf8');
const noticeForm = readFileSync(fileURLToPath(new URL('./form.vue', import.meta.url)), 'utf8');
const noticeApi = readFileSync(fileURLToPath(new URL('../../../api/admin/notice.ts', import.meta.url)), 'utf8');
const zhCn = readFileSync(fileURLToPath(new URL('./i18n/zh-cn.ts', import.meta.url)), 'utf8');
const en = readFileSync(fileURLToPath(new URL('./i18n/en.ts', import.meta.url)), 'utf8');

/** 取出源码里所有 `notice.xxx` 形式的 i18n key */
function usedKeys(source: string): string[] {
	return [...source.matchAll(/t\('notice\.(\w+)'\)/g)].map((matched) => matched[1]);
}

/** 取出 i18n 文案文件里声明的全部 key */
function declaredKeys(source: string): string[] {
	return [...source.matchAll(/^\t\t(\w+):/gm)].map((matched) => matched[1]);
}

describe('系统公告管理页（约定 C2~C6）', () => {
	it('接口封装与约定路径一致', () => {
		expect(noticeApi).toMatch(/url: '\/admin\/notice\/page',\s*\n\s*method: 'get'/);
		expect(noticeApi).toMatch(/url: '\/admin\/notice',\s*\n\s*method: 'post'/);
		expect(noticeApi).toMatch(/url: '\/admin\/notice',\s*\n\s*method: 'put'/);
		expect(noticeApi).toMatch(/url: '\/admin\/notice',\s*\n\s*method: 'delete'/);
		expect(noticeApi).toContain(`url: '/admin/notice/details/' + id`);
		// 首页公告条用的 latest 封装
		expect(noticeApi).toMatch(/url: '\/admin\/notice\/latest',\s*\n\s*method: 'get'/);
		expect(noticeApi).toContain('export function fetchLatest()');
	});

	it('表格列出六个公告字段', () => {
		const fields = ['title', 'content', 'status', 'publishTime', 'createTime', 'createBy'];
		for (const field of fields) {
			expect(noticePage, `缺少 ${field} 列`).toContain(`prop="${field}"`);
		}
	});

	it('发布人为空时显示破折号', () => {
		expect(noticePage).toContain(`scope.row.createBy || '—'`);
	});

	it('查询条件区有发布人筛选并绑定到查询参数', () => {
		expect(noticePage).toContain('v-model="state.queryForm.createBy"');
		// prop 必须有，否则重置按钮清不掉这一项
		expect(noticePage).toMatch(/<el-form-item[^>]*prop="createBy"[\s\S]*?v-model="state\.queryForm\.createBy"/);
	});

	it('状态列三态各自渲染，非法值不会显示成已发布', () => {
		expect(noticePage).toContain(`<el-tag type="success" v-if="scope.row.status === '1'">{{ t('notice.published') }}</el-tag>`);
		expect(noticePage).toContain(`<el-tag type="info" v-else-if="scope.row.status === '0'">{{ t('notice.draft') }}</el-tag>`);
		expect(noticePage).toContain(`<el-tag type="warning" v-else-if="scope.row.status === '2'">{{ t('notice.offline') }}</el-tag>`);
		// 没有兜底分支，非法值一个标签都不渲染
		expect(noticePage).not.toContain(`v-else>{{ t('notice.published') }}`);
	});

	it('发布时间为空时显示破折号', () => {
		expect(noticePage).toContain(`scope.row.publishTime || '—'`);
	});

	it('按钮权限与后端注解一致', () => {
		for (const permission of ['sys_notice_add', 'sys_notice_edit', 'sys_notice_del']) {
			expect(noticePage).toContain(`v-auth="'${permission}'"`);
		}
	});

	it('页面上用到的 i18n key 在中英文案里都存在', () => {
		const keys = [...new Set([...usedKeys(noticePage), ...usedKeys(noticeForm)])];
		expect(keys.length).toBeGreaterThan(0);
		for (const key of keys) {
			expect(zhCn, `zh-cn 缺少 notice.${key}`).toMatch(new RegExp(`\\b${key}:`));
			expect(en, `en 缺少 notice.${key}`).toMatch(new RegExp(`\\b${key}:`));
		}
	});

	it('两份文案的 key 集合完全一致', () => {
		expect(declaredKeys(zhCn).sort()).toEqual(declaredKeys(en).sort());
		expect(declaredKeys(zhCn).length).toBeGreaterThan(0);
	});
});

describe('系统公告表单弹窗（约定 C3/C4）', () => {
	it('标题必填且限长 100，内容必填且限长 2000', () => {
		expect(noticeForm).toContain(`message: t('notice.titleRequired')`);
		expect(noticeForm).toContain("{ max: 100, message: t('notice.titleMaxLength')");
		expect(noticeForm).toContain(`message: t('notice.contentRequired')`);
		expect(noticeForm).toContain("{ max: 2000, message: t('notice.contentMaxLength')");
		expect(noticeForm).toContain('maxlength="100"');
		expect(noticeForm).toContain('maxlength="2000"');
		// 纯空格也算空，前端先拦下来不发请求
		expect(noticeForm).toContain("trimmedNotBlank(t('notice.titleRequired'))");
		expect(noticeForm).toContain("trimmedNotBlank(t('notice.contentRequired'))");
	});

	it('状态用单选框三态，新增默认草稿', () => {
		expect(noticeForm).toContain('<el-radio-group v-model="form.status">');
		for (const value of ['0', '1', '2']) {
			expect(noticeForm, `缺少状态 ${value} 的选项`).toContain(`<el-radio border value="${value}">`);
		}
		expect(noticeForm).toContain(`status: '0'`);
		expect(noticeForm).toContain(`form.status = '0';`);
	});

	it('正文用多行纯文本输入', () => {
		expect(noticeForm).toContain('type="textarea"');
		expect(noticeForm).not.toContain('<Editor');
	});

	it('新增表单不提交发布时间与添加时间', () => {
		expect(noticeForm).not.toContain('publishTime');
		expect(noticeForm).not.toContain('createTime');
	});
});
