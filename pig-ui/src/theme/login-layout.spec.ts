import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';
import { inflateSync } from 'node:zlib';
import * as sass from 'sass';

const loginScssPath = fileURLToPath(new URL('./login.scss', import.meta.url));
const loginVuePath = fileURLToPath(new URL('../views/login/index.vue', import.meta.url));
const qrcodeVuePath = fileURLToPath(new URL('../views/login/component/qrcode.vue', import.meta.url));
const bgPngPath = fileURLToPath(new URL('../assets/login/backgroud.png', import.meta.url));

const loginVue = readFileSync(loginVuePath, 'utf8');
const qrcodeVue = readFileSync(qrcodeVuePath, 'utf8');
const compiledCss = sass.compile(loginScssPath).css;

/** 取出编译产物里某个 `@media (min-width: Npx)` 块的内容 */
function mediaBlock(css: string, minWidth: number): string {
	const start = css.indexOf(`@media (min-width: ${minWidth}px)`);
	if (start === -1) return '';
	let depth = 0;
	const from = css.indexOf('{', start);
	for (let i = from; i < css.length; i++) {
		if (css[i] === '{') depth++;
		else if (css[i] === '}') {
			depth--;
			if (depth === 0) return css.slice(from + 1, i);
		}
	}
	return '';
}

/** 取出某个选择器在给定 CSS 文本里的声明块 */
function ruleBody(css: string, selector: string): string {
	const pattern = new RegExp(`${selector.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\{([^}]*)\\}`);
	const matched = pattern.exec(css);
	return matched ? matched[1] : '';
}

/** 从声明块里读一个属性值 */
function prop(body: string, name: string): string {
	const pattern = new RegExp(`(?:^|;)\\s*${name}\\s*:\\s*([^;]+)`);
	const matched = pattern.exec(body);
	return matched ? matched[1].trim() : '';
}

/**
 * 解码 backgroud.png（8bit 调色板 + tRNS），返回画布尺寸与「墨迹右边界」占图片宽度的比例。
 * 插画外的区域全透明，透明区并不算画面内容，卡片能否右移取决于墨迹的真实边界，
 * 所以这里按真实像素算，而不是靠肉眼估一个 0.5 / 0.6 之类的经验值。
 */
function measureInkRightFraction(path: string): { width: number; height: number; fraction: number } {
	const buf = readFileSync(path);
	if (buf.toString('ascii', 1, 4) !== 'PNG') throw new Error('不是 PNG');

	let width = 0;
	let height = 0;
	let bitDepth = 0;
	let colorType = 0;
	let interlace = 0;
	let trns = Buffer.alloc(0);
	const idat: Buffer[] = [];

	let offset = 8;
	while (offset < buf.length) {
		const length = buf.readUInt32BE(offset);
		const type = buf.toString('ascii', offset + 4, offset + 8);
		const data = buf.subarray(offset + 8, offset + 8 + length);
		if (type === 'IHDR') {
			width = data.readUInt32BE(0);
			height = data.readUInt32BE(4);
			bitDepth = data[8];
			colorType = data[9];
			interlace = data[12];
		} else if (type === 'tRNS') {
			trns = data;
		} else if (type === 'IDAT') {
			idat.push(data);
		} else if (type === 'IEND') {
			break;
		}
		offset += 12 + length;
	}

	if (bitDepth !== 8 || colorType !== 3 || interlace !== 0) {
		throw new Error(`预期 8bit 非隔行调色板 PNG，实际 bitDepth=${bitDepth} colorType=${colorType}`);
	}

	// 调色板透明度：tRNS 没覆盖到的索引默认不透明
	const alphaOf = (index: number) => (index < trns.length ? trns[index] : 255);

	const raw = inflateSync(Buffer.concat(idat));
	const stride = width; // 8bit 调色板图每像素 1 字节
	const pixels = Buffer.alloc(stride * height);

	// 逐行反滤波（PNG 规范 9.2）
	for (let y = 0; y < height; y++) {
		const filter = raw[y * (stride + 1)];
		const line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
		const cur = pixels.subarray(y * stride, (y + 1) * stride);
		const prev = y > 0 ? pixels.subarray((y - 1) * stride, y * stride) : null;
		for (let x = 0; x < stride; x++) {
			const a = x > 0 ? cur[x - 1] : 0;
			const b = prev ? prev[x] : 0;
			const c = prev && x > 0 ? prev[x - 1] : 0;
			let value = line[x];
			switch (filter) {
				case 0:
					break;
				case 1:
					value += a;
					break;
				case 2:
					value += b;
					break;
				case 3:
					value += (a + b) >> 1;
					break;
				case 4: {
					const p = a + b - c;
					const pa = Math.abs(p - a);
					const pb = Math.abs(p - b);
					const pc = Math.abs(p - c);
					value += pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
					break;
				}
				default:
					throw new Error(`未知的 PNG 滤波类型 ${filter}`);
			}
			cur[x] = value & 0xff;
		}
	}

	let rightmost = -1;
	for (let y = 0; y < height; y++) {
		for (let x = width - 1; x > rightmost; x--) {
			if (alphaOf(pixels[y * stride + x]) > 40) {
				rightmost = x;
				break;
			}
		}
	}
	return { width, height, fraction: rightmost / width };
}

const ink = measureInkRightFraction(bgPngPath);

// 布局参数全部从编译后的 CSS / 组件源码里读，避免测试里写死的副本和真实样式脱节：
// 改了 scss 而没改这里，断言就会指向真实取值，而不是自我印证。
const CARD_WIDTH = Number(/md:w-\[(\d+)px\]/.exec(loginVue)?.[1] ?? 0);
/** .login-bg 在 xl 断点里的 width: NNvw */
const BG_WIDTH_VW = Number(/(\d+(?:\.\d+)?)vw/.exec(prop(ruleBody(mediaBlock(compiledCss, 1280), '.login-bg'), 'width'))?.[1] ?? 0) / 100;
/** 从某个断点的 @media 块里读 .login-page 的 padding-right */
const paddingAt = (minWidth: number) => Number(/(\d+)px/.exec(prop(ruleBody(mediaBlock(compiledCss, minWidth), '.login-page'), 'padding-right'))?.[1] ?? 0);
const CARD_PADDING_XL = paddingAt(1280);
const CARD_PADDING_2XL = paddingAt(1536);
// qrcode.vue: w-[116px] 的方框 + p-1.5 左右内边距 + 1px 边框；右侧偏移 right-8 / 2xl:right-14
const QR_BOX = Number(/w-\[(\d+)px\]/.exec(qrcodeVue)?.[1] ?? 0) + 12 + 2;
const QR_RIGHT_OFFSET = Number(/right-(\d+)/.exec(qrcodeVue)?.[1] ?? 0) * 4;
const QR_RIGHT_OFFSET_2XL = Number(/2xl:right-(\d+)/.exec(qrcodeVue)?.[1] ?? 0) * 4;

/** 按 CSS 里的实际规则，算出某个视口下卡片与二维码的左右边界 */
function layoutAt(vw: number) {
	const paddingRight = vw >= 1536 ? CARD_PADDING_2XL : vw >= 1280 ? CARD_PADDING_XL : 0;
	const cardLeft = vw - paddingRight - CARD_WIDTH;
	const cardRight = vw - paddingRight;
	const inkRight = ink.fraction * BG_WIDTH_VW * vw;
	const qrRightOffset = vw >= 1536 ? QR_RIGHT_OFFSET_2XL : QR_RIGHT_OFFSET;
	const qrLeft = vw - qrRightOffset - QR_BOX;
	return { cardLeft, cardRight, inkRight, qrLeft };
}

describe('登录页背景插画（resources/backgroud.png）', () => {
	it('资源已入库且是 2158x1196 的 PNG', () => {
		expect(ink.width).toBe(2158);
		expect(ink.height).toBe(1196);
	});

	it('index.vue 引用 backgroud.png', () => {
		expect(loginVue).toContain("import loginBg from '/@/assets/login/backgroud.png'");
		expect(loginVue).toContain("class=\"login-bg\"");
	});

	it('不再引用原 4 张旧 SVG 背景', () => {
		for (const old of ['login-bg.svg', 'login-bg-dark.svg', 'login-bg-phone.svg', 'login-bg-phone-dark.svg']) {
			expect(loginVue, `index.vue 仍引用 ${old}`).not.toContain(old);
		}
	});

	// 负责人已定：暗黑模式统一也用 backgroud.png，不再按主题分支选图
	it('浅色/暗黑用同一张图，不按 isDark 分支', () => {
		expect(loginVue).not.toContain('isDark');
		expect(loginVue).toContain('loginBg');
	});

	it('.login-bg 在窄屏铺满整屏', () => {
		const base = ruleBody(compiledCss, '.login-bg');
		expect(prop(base, 'position')).toBe('absolute');
		expect(prop(base, 'background-size')).toBe('cover');
		expect(prop(base, 'z-index')).toBe('0');
	});
});

describe('桌面端登录卡片右移，避开插画与右下角二维码', () => {
	it('xl 起卡片改为靠右并留出右边距', () => {
		const block = mediaBlock(compiledCss, 1280);
		expect(block, '缺少 @media (min-width: 1280px)').not.toBe('');
		const card = ruleBody(block, '.login-page');
		expect(prop(card, 'justify-content')).toBe('flex-end');
		expect(prop(card, 'padding-right')).toBe(`${CARD_PADDING_XL}px`);
	});

	it('2xl 起右边距增大到 232px', () => {
		const block = mediaBlock(compiledCss, 1536);
		expect(prop(ruleBody(block, '.login-page'), 'padding-right')).toBe(`${CARD_PADDING_2XL}px`);
	});

	it('窄屏（<xl）不做右移，保持居中', () => {
		// 右移规则只应出现在 media query 里，全局作用域不能有 padding-right / flex-end
		const globalPart = compiledCss.split('@media')[0];
		expect(prop(ruleBody(globalPart, '.login-page'), 'padding-right')).toBe('');
		expect(prop(ruleBody(globalPart, '.login-page'), 'justify-content')).toBe('');
	});

	it('xl 起背景改为按 60vw 定宽并完整显示，避免透明区被 cover 放大', () => {
		const block = mediaBlock(compiledCss, 1280);
		const bg = ruleBody(block, '.login-bg');
		expect(prop(bg, 'width')).toBe('60vw');
		expect(prop(bg, 'background-size')).toBe('contain');
		expect(prop(bg, 'left')).toBe('0');
		expect(prop(bg, 'right')).toBe('auto');
	});
});

describe('几何不变式：卡片与插画、二维码互不遮挡（用图片真实墨迹边界计算）', () => {
	it('插画墨迹只占图片左侧约 61.5%', () => {
		expect(ink.fraction).toBeGreaterThan(0.60);
		expect(ink.fraction).toBeLessThan(0.63);
	});

	for (const vw of [1280, 1440, 1536, 1920]) {
		it(`${vw}px：卡片不压插画、不压二维码、不越界`, () => {
			const { cardLeft, cardRight, inkRight, qrLeft } = layoutAt(vw);

			expect(cardLeft, `卡片左边 ${cardLeft} 压到插画墨迹 ${inkRight}`).toBeGreaterThan(inkRight);
			expect(cardRight, `卡片右边 ${cardRight} 压到二维码 ${qrLeft}`).toBeLessThan(qrLeft);
			// 卡片不贴死右边缘（否则等于没留右边距）
			expect(cardRight).toBeLessThan(vw);
			// 间距至少 24px，不能只是「刚好不重叠」
			expect(qrLeft - cardRight).toBeGreaterThanOrEqual(24);
			expect(cardLeft - inkRight).toBeGreaterThanOrEqual(24);
		});
	}

	it('卡片宽度、二维码尺寸的假设仍与组件源码一致', () => {
		expect(loginVue).toContain('md:w-[560px]');
		expect(qrcodeVue).toContain('h-[116px] w-[116px]');
		expect(qrcodeVue).toContain('right-8');
		expect(qrcodeVue).toContain('2xl:right-14');
	});
});
