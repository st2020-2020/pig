import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const homeIndex = readFileSync(fileURLToPath(new URL('../index.vue', import.meta.url)), 'utf8');
const noticeBar = readFileSync(fileURLToPath(new URL('./index.vue', import.meta.url)), 'utf8');

describe('首页公告条（约定 C1 / AC12）', () => {
	it('挂在首页 <widgets/> 之前，处于页面最顶部', () => {
		const barAt = homeIndex.indexOf('<notice-bar/>');
		const widgetsAt = homeIndex.indexOf('<widgets/>');
		expect(barAt).toBeGreaterThan(-1);
		expect(widgetsAt).toBeGreaterThan(-1);
		expect(barAt).toBeLessThan(widgetsAt);
		// 首页原有演示弹窗保留
		expect(homeIndex).toContain('<notice v-model:show="showNotice"/>');
	});

	it('挂载时调用 latest 接口', () => {
		expect(noticeBar).toContain('fetchLatest');
		expect(noticeBar).toContain('onMounted(() => {');
		expect(noticeBar).toContain('loadLatest();');
	});

	it('拿到对象时渲染标题与正文', () => {
		expect(noticeBar).toContain('{{ notice.title }}');
		expect(noticeBar).toContain('{{ notice.content }}');
		// 正文保留换行
		expect(noticeBar).toContain('whitespace-pre-line');
	});

	it('关闭控件带关闭文案作为无障碍标签', () => {
		expect(noticeBar).toContain(`:aria-label="t('notice.close')"`);
		expect(noticeBar).toContain('@click="handleClose"');
	});

	it('点关闭后把 noticeId 记进本地存储', () => {
		// key 里含 noticeId，同一浏览器不再被这条挡住
		expect(noticeBar).toContain('`notice-closed-${noticeId}`');
		expect(noticeBar).toContain('Local.set(closedKey(noticeId), Date.now())');
		// 再次加载时已关闭的这条不渲染
		expect(noticeBar).toContain('Local.get(closedKey(data.noticeId))');
	});

	it('接口返回 null 或失败时不渲染、不弹错误提示', () => {
		// 单一根节点 v-if，未拿到对象时没有任何 DOM
		expect(noticeBar).toContain('<div v-if="notice"');
		expect(noticeBar).toContain('if (!data || !data.noticeId ||');
		// 出错静默，不能 import useMessage 去弹 error
		expect(noticeBar).not.toContain('useMessage');
		expect(noticeBar).not.toContain('.error(');
	});
});
