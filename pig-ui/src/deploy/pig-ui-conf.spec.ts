import { describe, expect, it } from 'vitest';
import { fileURLToPath } from 'node:url';
import { readFileSync } from 'node:fs';

const conf = readFileSync(fileURLToPath(new URL('../../../server-deploy/pig-ui.conf', import.meta.url)), 'utf8');

/** 去掉注释行与行内被引号包裹的内容，只留 nginx 会当语法解析的部分 */
function outsideQuotes(line: string): { text: string; unclosed: boolean } {
	let text = '';
	let quote: string | null = null;
	for (const ch of line) {
		if (quote) {
			if (ch === quote) quote = null;
			continue;
		}
		if (ch === '"' || ch === "'") {
			quote = ch;
			continue;
		}
		text += ch;
	}
	return { text, unclosed: quote !== null };
}

const syntaxLines = conf
	.split(/\r?\n/)
	.map((line, index) => ({ line, no: index + 1 }))
	.filter(({ line }) => line.trim() && !line.trim().startsWith('#'));

describe('部署用 nginx 配置 · server-deploy/pig-ui.conf（AC10/AC11）', () => {
	it('引号外不出现花括号，除合法的块开始 { 与块结束 }', () => {
		// 上次线上事故：location ~* ^/assets/....{8,}\.(js|...)$ { 的花括号没加引号，
		// nginx 把 {8,} 当成块开始，报 unknown directive，整份配置加载失败
		const problems: string[] = [];
		for (const { line, no } of syntaxLines) {
			const { text, unclosed } = outsideQuotes(line);
			if (unclosed) problems.push(`第 ${no} 行引号未闭合: ${line.trim()}`);
			const openers = (text.match(/{/g) || []).length;
			const closers = (text.match(/}/g) || []).length;
			const legalBlockStart = openers === 1 && closers === 0 && /{\s*$/.test(text);
			const legalBlockEnd = openers === 0 && closers === 1 && text.trim() === '}';
			if (openers + closers > 0 && !legalBlockStart && !legalBlockEnd) {
				problems.push(`第 ${no} 行花括号在引号外: ${line.trim()}`);
			}
		}
		expect(problems, problems.join('\n')).toEqual([]);
	});

	it('块花括号整体配平', () => {
		let openers = 0;
		let closers = 0;
		for (const { line } of syntaxLines) {
			const { text } = outsideQuotes(line);
			openers += (text.match(/{/g) || []).length;
			closers += (text.match(/}/g) || []).length;
		}
		expect(openers).toBeGreaterThan(0);
		expect(openers).toBe(closers);
	});

	it('带量词花括号的静态资源正则整体加了双引号', () => {
		const assetLocation = syntaxLines.find(({ line }) => line.includes('/assets/') && line.trim().startsWith('location'));
		expect(assetLocation, '缺少 assets 静态资源 location').toBeDefined();
		const trimmed = assetLocation!.line.trim();
		// 形如 location ~* "^/assets/..." {
		expect(trimmed).toMatch(/^location\s+~\*\s+"[^"]+"\s*\{$/);
		// 引号内的反斜杠会被 nginx 当转义处理，点号应写成 [.] 而不是 \.
		expect(trimmed).not.toContain('\\');
	});

	it('入口页走 no-cache，hash 资源才 immutable', () => {
		expect(conf).toMatch(/location = \/index\.html \{/);
		expect(conf).toContain('Cache-Control "no-cache, must-revalidate"');
		expect(conf).toContain('Cache-Control "public, max-age=31536000, immutable"');
		// 入口页那条不允许出现长 max-age 或 immutable
		const indexBlock = conf.slice(conf.indexOf('location = /index.html'));
		expect(indexBlock.slice(0, indexBlock.indexOf('}'))).not.toContain('immutable');
	});

	it('静态站仍把深层地址回落到入口页', () => {
		expect(conf).toContain('try_files $uri $uri/ /index.html;');
	});
});
