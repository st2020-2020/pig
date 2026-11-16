import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const component = readFileSync(fileURLToPath(new URL('./login-records.vue', import.meta.url)), 'utf8');
const personal = readFileSync(fileURLToPath(new URL('./personal.vue', import.meta.url)), 'utf8');
const api = readFileSync(fileURLToPath(new URL('../../../../../api/admin/loginRecord.ts', import.meta.url)), 'utf8');
const zhCn = readFileSync(fileURLToPath(new URL('../i18n/zh-cn.ts', import.meta.url)), 'utf8');
const en = readFileSync(fileURLToPath(new URL('../i18n/en.ts', import.meta.url)), 'utf8');

/** 取出组件里所有 `personal.xxx` 形式的 i18n key（模板写法和 t() 写法都算） */
function usedKeys(source: string): string[] {
	const dollar = [...source.matchAll(/\$t\('personal\.(\w+)'\)/g)].map((matched) => matched[1]);
	const calls = [...source.matchAll(/t\('personal\.(\w+)'\)/g)].map((matched) => matched[1]);
	return [...new Set([...dollar, ...calls])];
}

/** 取出 personal 块里声明的全部 key */
function declaredPersonalKeys(source: string): string[] {
	const block = source.slice(source.indexOf('\tpersonal: {'));
	return [...block.matchAll(/^\t\t(\w+):/gm)].map((matched) => matched[1]);
}

describe('个人中心 · 登录记录标签', () => {
	it('接口封装指向自己的登录记录分页接口', () => {
		expect(api).toMatch(/url: '\/admin\/login-log\/page',\s*\n\s*method: 'get'/);
	});

	it('四列顺序固定：登录时间、IP、浏览器、结果', () => {
		const order = ['prop="createTime"', 'prop="remoteAddr"', 'prop="browser"'];
		let cursor = -1;
		for (const marker of order) {
			const index = component.indexOf(marker);
			expect(index, `缺少 ${marker}`).toBeGreaterThan(-1);
			expect(index).toBeGreaterThan(cursor);
			cursor = index;
		}
		// 结果列没有 prop，只认它前面的顺序位置：必须是最后一列
		const resultIndex = component.indexOf(`\$t('personal.result')`);
		expect(resultIndex).toBeGreaterThan(cursor);
	});

	it('结果列只有成功、失败两种标签', () => {
		expect(component).toContain(`:type="scope.row.result === 'success' ? 'success' : 'danger'"`);
		expect(component).toContain(`$t('personal.resultSuccess')`);
		expect(component).toContain(`$t('personal.resultFailure')`);
		expect([...component.matchAll(/<el-tag/g)]).toHaveLength(1);
	});

	it('空提示走表格空插槽', () => {
		expect(component).toMatch(/#empty[\s\S]*personal\.noLoginRecords/);
	});

	it('日期范围在封装层拆成开始、结束日期，不发数组参数', () => {
		expect(component).toContain('const { dateRange, ...rest } = params;');
		expect(component).toContain('startDate: dateRange?.[0]');
		expect(component).toContain('endDate: dateRange?.[1]');
		// 整个组件里没有把 dateRange 直接传给接口的地方
		expect(component).not.toMatch(/pageList\(\{[^}]*dateRange,/);
	});

	it('日期选择器是日期粒度、不可选窗口外的日期', () => {
		expect(component).toContain('type="daterange"');
		expect(component).toContain('value-format="YYYY-MM-DD"');
		expect(component).toContain(':disabled-date="disabledDate"');
		expect(component).toContain('const WINDOW_DAYS = 30;');
	});

	it('查询与重置按钮语义正确', () => {
		expect(component).toContain(`@click="handleQuery"`);
		expect(component).toContain(`@click="handleReset"`);
		expect(component).toContain(`$t('common.queryBtn')`);
		expect(component).toContain(`$t('common.resetBtn')`);
		// 加载中重复点只发一次请求
		expect(component).toMatch(/if \(state\.loading\) \{\s*return;\s*\}/);
	});

	it('每页 10/20/50 可选，分页绑定 state.pagination', () => {
		expect(component).toContain('pageSizes: [10, 20, 50]');
		expect(component).toContain('v-bind="state.pagination"');
		expect(component).toContain('@current-change="currentChangeHandle"');
		expect(component).toContain('@size-change="sizeChangeHandle"');
	});

	it('排序固定按登录时间倒序', () => {
		expect(component).toContain(`descs: ['create_time']`);
		expect(component).not.toContain('sort-change');
	});

	it('个人中心新增第 5 个标签，且首次进入不请求', () => {
		expect(personal).toContain('<el-tab-pane :label="$t(\'personal.loginRecordsTab\')" name="loginRecords" lazy>');
		expect(personal).toContain('<login-records />');
		// 排在 API 密钥之后
		expect(personal.indexOf(`personal.apiKeyTab`)).toBeLessThan(personal.indexOf(`personal.loginRecordsTab`));
	});

	it('用到的 i18n key 在中英文案里都存在', () => {
		const keys = usedKeys(component);
		expect(keys.length).toBeGreaterThan(0);
		for (const key of keys) {
			expect(zhCn, `zh-cn 缺少 personal.${key}`).toMatch(new RegExp(`^\\t\\t${key}:`, 'm'));
			expect(en, `en 缺少 personal.${key}`).toMatch(new RegExp(`^\\t\\t${key}:`, 'm'));
		}
	});

	it('两份文案的 personal key 集合完全一致', () => {
		expect(declaredPersonalKeys(zhCn).sort()).toEqual(declaredPersonalKeys(en).sort());
		expect(declaredPersonalKeys(zhCn).length).toBeGreaterThan(0);
	});
});
