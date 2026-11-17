import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const component = readFileSync(fileURLToPath(new URL('./history.vue', import.meta.url)), 'utf8');
const noticeBar = readFileSync(fileURLToPath(new URL('./index.vue', import.meta.url)), 'utf8');
const api = readFileSync(fileURLToPath(new URL('../../../api/admin/notice.ts', import.meta.url)), 'utf8');
const zhCn = readFileSync(fileURLToPath(new URL('../i18n/zh-cn.ts', import.meta.url)), 'utf8');
const en = readFileSync(fileURLToPath(new URL('../i18n/en.ts', import.meta.url)), 'utf8');

/** 取出组件里所有 `home.xxx` 形式的 i18n key（模板写法和 t() 写法都算） */
function usedKeys(source: string): string[] {
	const calls = [...source.matchAll(/t\('home\.(\w+)'\)/g)].map((matched) => matched[1]);
	return [...new Set(calls)];
}

/** 取出 home 块里声明的全部 key */
function declaredHomeKeys(source: string): string[] {
	const block = source.slice(source.indexOf('\thome: {'));
	return [...block.matchAll(/^\t\t(\w+):/gm)].map((matched) => matched[1]);
}

describe('首页 · 历史公告（约定 C1 / AC1~AC12）', () => {
	it('接口封装指向历史公告分页接口', () => {
		expect(api).toMatch(/url: '\/admin\/notice\/history\/page',\s*\n\s*method: 'get'/);
		expect(api).toContain('export function fetchHistoryPage');
	});

	it('列表四列顺序固定：序号、标题、状态、发布时间', () => {
		const order = ['type="index"', 'prop="title"', 'home.newsStatus', 'home.historyNoticePublishTime'];
		let cursor = -1;
		for (const marker of order) {
			const index = component.indexOf(marker);
			expect(index, `缺少 ${marker}`).toBeGreaterThan(-1);
			expect(index).toBeGreaterThan(cursor);
			cursor = index;
		}
	});

	it('只有已下线状态渲染标签，其余状态不渲染', () => {
		expect(component).toContain(`<el-tag type="info" v-if="scope.row.status === '2'">{{ t('notice.offline') }}</el-tag>`);
		expect([...component.matchAll(/<el-tag/g)]).toHaveLength(1);
	});

	it('发布时间为空时显示破折号，标题超长省略并悬停显示全文', () => {
		expect(component).toContain(`scope.row.publishTime || '—'`);
		expect(component).toContain('show-overflow-tooltip');
	});

	it('空提示走表格空插槽，加载中不显示空态文案', () => {
		expect(component).toMatch(/#empty[\s\S]*home\.historyNoticeEmpty/);
		expect(component).toContain(`<span v-if="!state.loading">{{ t('home.historyNoticeEmpty') }}</span>`);
		// 表格带加载遮罩
		expect(component).toContain('v-loading="state.loading"');
	});

	it('每页 10/20/50 可选，分页绑定 state.pagination', () => {
		expect(component).toContain('pageSizes: [10, 20, 50]');
		expect(component).toContain('v-bind="state.pagination"');
		expect(component).toContain('@current-change="currentChangeHandle"');
		expect(component).toContain('@size-change="sizeChangeHandle"');
		// 首屏不发请求，由首页入口点开时才拉数据
		expect(component).toContain('createdIsNeed: false');
	});

	it('打开抽屉时回到第 1 页重新拉数据', () => {
		expect(component).toMatch(/const open = \(\) => \{\s*visible\.value = true;\s*getDataList\(\);/);
		expect(component).toContain('defineExpose({');
		expect(component).toContain('open,');
		// 抽屉与详情都不写关闭回调，关闭详情保留页码、重开自动回第 1 页
		expect(component).not.toContain('@close');
		expect(component).not.toContain('Local.');
	});

	it('请求失败只弹一次提示并兜底成空页，抽屉不关闭', () => {
		expect(component).toMatch(/catch \{[\s\S]*useMessage\(\)\.error\(t\('home\.historyNoticeFetchError'\)\)[\s\S]*records: \[\], total: 0/);
		expect([...component.matchAll(/useMessage\(\)\.error/g)]).toHaveLength(1);
		// 抽屉可见性只由入口控制，出错不改 visible
		expect(component).not.toMatch(/catch \{[\s\S]{0,200}visible\.value = false/);
	});

	it('详情用弹窗展示纯文本正文，保留换行、不解析 HTML', () => {
		expect(component).toContain('<el-dialog');
		expect(component).toContain('width="640px"');
		expect(component).toContain('whitespace-pre-line break-words');
		expect(component).toContain('{{ currentNotice?.content }}');
		expect(component).not.toContain('v-html');
		// 只读，没有任何操作按钮
		expect(component).not.toMatch(/<el-button/);
	});

	it('首页公告条引入入口组件并常驻「历史公告」按钮', () => {
		expect(noticeBar).toContain(`import History from './history.vue';`);
		expect(noticeBar).toContain('<history ref="historyRef" />');
		expect(noticeBar).toContain(`@click="historyRef.open()"`);
	});

	it('用到的 i18n key 在中英文案里都存在', () => {
		const keys = [...new Set([...usedKeys(component), ...usedKeys(noticeBar)])];
		expect(keys.length).toBeGreaterThan(0);
		for (const key of keys) {
			expect(zhCn, `zh-cn 缺少 home.${key}`).toMatch(new RegExp(`^\\t\\t${key}:`, 'm'));
			expect(en, `en 缺少 home.${key}`).toMatch(new RegExp(`^\\t\\t${key}:`, 'm'));
		}
	});

	it('两份文案的 home key 集合完全一致', () => {
		expect(declaredHomeKeys(zhCn).sort()).toEqual(declaredHomeKeys(en).sort());
		expect(declaredHomeKeys(zhCn).length).toBeGreaterThan(0);
	});
});
