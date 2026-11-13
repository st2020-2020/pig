import { defineStore } from 'pinia';

/** 验证码类型枚举 */
export enum CaptchaType {
	ClickWord = 'clickWord',
	BlockPuzzle = 'blockPuzzle',
	Math = 'math',
	None = 'none',
}

/** 密码规则预设 */
export enum PasswordRule {
	LetterNumber = 'letterNumber',
	LetterNumberSymbol = 'letterNumberSymbol',
	UpperLowerNumberSymbol = 'upperLowerNumberSymbol',
}

/**
 * 网站配置类型定义
 */
export interface SiteConfig {
	clarityId: string;
	captchaType: CaptchaType | string;
	passwordRule: PasswordRule | string;
	forceResetPwd: boolean;
	forceLogout: boolean;
	smsLoginEnable: boolean;
	socialLoginEnable: boolean;
	registerEnable: boolean;
	resetPassword: boolean;
	i18nEnable: boolean;
	darkModeEnable: boolean;
	antiDebugEnable: boolean;
	antiDebugKey: string;
	title: string;
	footer: string;
	privacyTip: string;
	logo: string;
}

/** 默认配置（兜底值） */
const DEFAULT_SITE_CONFIG: SiteConfig = {
	clarityId: '',
	captchaType: CaptchaType.ClickWord,
	passwordRule: PasswordRule.LetterNumber,
	forceResetPwd: false,
	forceLogout: false,
	smsLoginEnable: true,
	socialLoginEnable: true,
	registerEnable: true,
	resetPassword: true,
	i18nEnable: true,
	darkModeEnable: true,
	antiDebugEnable: false,
	antiDebugKey: 'pig',
	title: '自定义云平台',
	footer: 'pig4cloud.com',
	privacyTip: '',
	logo: '',
};

export const useSiteConfig = defineStore('siteConfig', {
	state: (): { siteConfig: SiteConfig } => ({
		siteConfig: { ...DEFAULT_SITE_CONFIG },
	}),
	actions: {
		setSiteConfig(config: SiteConfig) {
			// 约定 C1 错误分支：SITE_TITLE 参数行缺失时后端会下发 title: null，
			// 直接展开会把兜底值覆盖成 null，页面主标题变空，所以这里显式回落默认标题。
			this.siteConfig = {
				...DEFAULT_SITE_CONFIG,
				...config,
				title: config?.title || DEFAULT_SITE_CONFIG.title,
				passwordRule: config?.passwordRule || DEFAULT_SITE_CONFIG.passwordRule,
			};
		},
	},
});
