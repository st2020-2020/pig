import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import * as sass from 'sass';

const loginScssPath = fileURLToPath(new URL('./login.scss', import.meta.url));
const themeIndexScssPath = fileURLToPath(new URL('./index.scss', import.meta.url));

/** 编译 login.scss，返回压缩后的 CSS 文本 */
function compileLoginScss(): string {
	return sass.compile(loginScssPath).css;
}

/** 取出编译产物中所有规则的 `选择器 => 声明块`，用于跨文件比对 */
function allRules(css: string): { selector: string; body: string }[] {
	const rules: { selector: string; body: string }[] = [];
	const pattern = /([^{}]+)\{([^{}]*)\}/g;
	let matched: RegExpExecArray | null;
	while ((matched = pattern.exec(css)) !== null) {
		rules.push({ selector: matched[1].trim(), body: matched[2] });
	}
	return rules;
}

/** 取出声明块里指定属性的所有取值（保留 !important 原文） */
function declarations(body: string, prop: string): string[] {
	return body
		.split(';')
		.map((item) => item.trim())
		.filter((item) => item.toLowerCase().startsWith(`${prop}:`))
		.map((item) => item.slice(item.indexOf(':') + 1).trim());
}

/** 取出编译产物中某个选择器的所有声明块（多个选择器时用于逐条断言），颜色统一小写便于比较 */
function pickRules(css: string, selector: string): string[] {
	const rules: string[] = [];
	const pattern = new RegExp(`\\${selector}\\s*\\{([^}]*)\\}`, 'g');
	let matched: RegExpExecArray | null;
	while ((matched = pattern.exec(css)) !== null) {
		rules.push(matched[1].toLowerCase());
	}
	return rules;
}

describe('登录按钮配色（亮橙 #FFA940 → #FA8C16）', () => {
	const css = compileLoginScss();

	it('.login-btn 静态态使用橙色渐变', () => {
		const [base] = pickRules(css, '.login-btn');
		expect(base).toContain('linear-gradient(135deg, #ffa940 0%, #fa8c16 100%)');
	});

	it('.login-btn 静态态阴影为橙色系', () => {
		const [base] = pickRules(css, '.login-btn');
		expect(base).toContain('rgba(250, 140, 22, 0.3)');
	});

	it(':hover 仍是同一档橙色，不引入其它色相', () => {
		const [hover] = pickRules(css, '.login-btn:hover');
		expect(hover).toContain('linear-gradient(135deg, #ffa940 0%, #fa8c16 100%)');
		expect(hover).toContain('rgba(250, 140, 22, 0.4)');
	});

	it('.is-loading 仍是同一档橙色', () => {
		const [loading] = pickRules(css, '.login-btn.is-loading');
		expect(loading).toContain('linear-gradient(135deg, #ffa940 0%, #fa8c16 100%)');
		expect(loading).toContain('rgba(250, 140, 22, 0.25)');
	});

	it('四个状态的按钮背景都不含原来的蓝色', () => {
		const states = [
			...pickRules(css, '.login-btn'),
			...pickRules(css, '.login-btn:hover'),
			...pickRules(css, '.login-btn:active'),
			...pickRules(css, '.login-btn.is-loading'),
		];
		expect(states).toHaveLength(4);
		const joined = states.join('\n').toUpperCase();
		expect(joined).not.toContain('#618EFF');
		expect(joined).not.toContain('#0058FF');
	});

	// 回归护栏：V2/V4 曾因投影被 element.scss 的蓝色规则压过而失败。
	// element.scss 的 `.el-button:not(...):not(...):not(...).el-button--primary`
	// 特异度是 0,5,0（:hover 版 0,6,0），远高于 .login-btn 的 0,1,0，
	// 所以 .login-btn 的 box-shadow 必须带 !important 才能赢下投影。
	it('四个状态的 box-shadow 都带 !important（否则投影被蓝色规则压过）', () => {
		const expected = [
			{ selector: '.login-btn', value: 'rgba(250, 140, 22, 0.3)' },
			{ selector: '.login-btn:hover', value: 'rgba(250, 140, 22, 0.4)' },
			{ selector: '.login-btn:active', value: 'rgba(250, 140, 22, 0.35)' },
			{ selector: '.login-btn.is-loading', value: 'rgba(250, 140, 22, 0.25)' },
		];
		for (const { selector, value } of expected) {
			const bodies = pickRules(css, selector);
			expect(bodies.length, `${selector} 应恰好有一条规则`).toBe(1);
			const shadows = declarations(bodies[0], 'box-shadow');
			expect(shadows.length, `${selector} 应声明 box-shadow`).toBe(1);
			const shadow = shadows[0].toLowerCase();
			expect(shadow, `${selector} 的投影不是橙色系`).toContain(value.toLowerCase());
			expect(shadow, `${selector} 的投影缺少 !important`).toContain('!important');
		}
	});

	it('整包主题里压过 .login-btn 的蓝色投影规则均无 !important，登录按钮投影归橙色', () => {
		const themeCss = sass.compile(themeIndexScssPath).css;
		const rules = allRules(themeCss);

		// element.scss 里抬高特异度的两条蓝色 primary 投影
		const conflicting = rules.filter(
			(rule) =>
				rule.selector.includes('.el-button--primary') && !rule.selector.includes('.login-btn') && declarations(rule.body, 'box-shadow').length > 0
		);
		expect(conflicting.length, '应能找到 element.scss 的 primary 投影规则').toBeGreaterThan(0);
		for (const rule of conflicting) {
			for (const shadow of declarations(rule.body, 'box-shadow')) {
				expect(shadow.toLowerCase(), `${rule.selector} 若带 !important 就会盖过登录按钮`).not.toContain('!important');
			}
		}

		// 登录按钮四条投影都在整包里，且都带 !important
		const loginShadows = rules.filter((rule) => rule.selector.includes('.login-btn')).flatMap((rule) => declarations(rule.body, 'box-shadow'));
		expect(loginShadows).toHaveLength(4);
		for (const shadow of loginShadows) {
			expect(shadow.toLowerCase()).toContain('!important');
			expect(shadow.toLowerCase()).toContain('rgba(250, 140, 22');
			expect(shadow.toLowerCase()).not.toContain('64, 158, 255');
		}
	});
});
