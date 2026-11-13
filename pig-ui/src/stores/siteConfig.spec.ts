import { beforeEach, describe, expect, it } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { useSiteConfig, type SiteConfig } from './siteConfig';

describe('siteConfig 站点标题（约定 C1）', () => {
	beforeEach(() => {
		setActivePinia(createPinia());
	});

	it('默认标题为「自定义云平台」', () => {
		const store = useSiteConfig();
		expect(store.siteConfig.title).toBe('自定义云平台');
		expect(store.siteConfig.title).not.toBe('PIG ADMIN');
	});

	it('后端未下发 title 时回退默认标题', () => {
		const store = useSiteConfig();
		// C1 错误分支：info() 失败或 SITE_TITLE 缺失，且 data.site 可能为空对象
		store.setSiteConfig({} as SiteConfig);
		expect(store.siteConfig.title).toBe('自定义云平台');
	});

	it('title 为 null 时回退默认标题（C1 约定的 SITE_TITLE 缺失分支）', () => {
		const store = useSiteConfig();
		// 后端 SITE_TITLE 参数行缺失时下发的是 title: null，不是缺 key；
		// 展开 null 会覆盖兜底值，必须显式回落，否则页面主标题为空。
		store.setSiteConfig({ title: null } as unknown as SiteConfig);
		expect(store.siteConfig.title).toBe('自定义云平台');
	});

	it('title 为空串时回退默认标题', () => {
		const store = useSiteConfig();
		store.setSiteConfig({ title: '' } as SiteConfig);
		expect(store.siteConfig.title).toBe('自定义云平台');
	});

	it('后端下发的 title 优先于默认标题', () => {
		const store = useSiteConfig();
		store.setSiteConfig({ title: '管理端配置的标题' } as SiteConfig);
		expect(store.siteConfig.title).toBe('管理端配置的标题');
	});

	it('其它字段缺失时仍保留默认值', () => {
		const store = useSiteConfig();
		store.setSiteConfig({ title: '管理端配置的标题' } as SiteConfig);
		expect(store.siteConfig.smsLoginEnable).toBe(true);
		expect(store.siteConfig.passwordRule).toBe('letterNumber');
	});
});
