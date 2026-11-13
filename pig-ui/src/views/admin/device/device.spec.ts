import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const devicePage = readFileSync(fileURLToPath(new URL('./index.vue', import.meta.url)), 'utf8');
const deviceForm = readFileSync(fileURLToPath(new URL('./form.vue', import.meta.url)), 'utf8');
const deviceApi = readFileSync(fileURLToPath(new URL('../../../api/admin/device.ts', import.meta.url)), 'utf8');
const zhCn = readFileSync(fileURLToPath(new URL('./i18n/zh-cn.ts', import.meta.url)), 'utf8');
const en = readFileSync(fileURLToPath(new URL('./i18n/en.ts', import.meta.url)), 'utf8');

/** 取出源码里所有 `device.xxx` 形式的 i18n key */
function usedKeys(source: string): string[] {
	return [...source.matchAll(/t\('device\.(\w+)'\)/g)].map((matched) => matched[1]);
}

describe('设备管理页（约定 C1~C5）', () => {
	it('接口封装与约定路径一致', () => {
		expect(deviceApi).toMatch(/url: '\/admin\/device\/page',\s*\n\s*method: 'get'/);
		expect(deviceApi).toMatch(/url: '\/admin\/device',\s*\n\s*method: 'post'/);
		expect(deviceApi).toMatch(/url: '\/admin\/device',\s*\n\s*method: 'put'/);
		expect(deviceApi).toMatch(/url: '\/admin\/device',\s*\n\s*method: 'delete'/);
		expect(deviceApi).toContain(`url: '/admin/device/details/' + id`);
	});

	it('表格列出六个设备字段', () => {
		const fields = ['deviceName', 'deviceSerialNo', 'deviceAddress', 'firmwareVersion', 'onlineStatus', 'createTime'];
		for (const field of fields) {
			expect(devicePage, `缺少 ${field} 列`).toContain(`prop="${field}"`);
		}
	});

	it('在离线状态用两种标签区分在线与离线', () => {
		expect(devicePage).toContain(`scope.row.onlineStatus === '1' ? 'success' : 'info'`);
		expect(devicePage).toContain(`t('device.online')`);
		expect(devicePage).toContain(`t('device.offline')`);
	});

	it('按钮权限与后端注解一致', () => {
		for (const permission of ['sys_device_add', 'sys_device_edit', 'sys_device_del']) {
			expect(devicePage).toContain(`v-auth="'${permission}'"`);
		}
	});

	it('页面上用到的 i18n key 在中英文案里都存在', () => {
		const keys = [...new Set([...usedKeys(devicePage), ...usedKeys(deviceForm)])];
		expect(keys.length).toBeGreaterThan(0);
		for (const key of keys) {
			expect(zhCn, `zh-cn 缺少 device.${key}`).toMatch(new RegExp(`\\b${key}:`));
			expect(en, `en 缺少 device.${key}`).toMatch(new RegExp(`\\b${key}:`));
		}
	});

	it('新增表单不提交添加时间', () => {
		expect(deviceForm).not.toContain('createTime');
	});
});
