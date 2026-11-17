import { describe, expect, it } from 'vitest';

import { fileURLToPath } from 'node:url';

import { readFileSync } from 'node:fs';



const backEnd = readFileSync(fileURLToPath(new URL('./backEnd.ts', import.meta.url)), 'utf8');

const route = readFileSync(fileURLToPath(new URL('./route.ts', import.meta.url)), 'utf8');

const routerIndex = readFileSync(fileURLToPath(new URL('./index.ts', import.meta.url)), 'utf8');



describe('后端控制路由 · 零菜单账号也能进首页（AC1/AC3）', () => {

	it('菜单为空时不再提前返回，静态路由照常注册', () => {

		// 这个提前 return 会让 /home（在 dynamicRoutes 里）永远挂不上，登录后白屏

		expect(backEnd).not.toContain('if ((res.data || []).length <= 0) return');

		expect(backEnd).not.toContain('Promise.resolve(true)');

	});



	it('先取 menus，再把 dynamicRoutes 和菜单一起挂到 baseRoutes 第一个子节点', () => {

		const menusAt = backEnd.indexOf('const menus = res?.data || [];');

		const childrenAt = backEnd.indexOf('baseRoutes[0].children = [...dynamicRoutes,');

		expect(menusAt).toBeGreaterThan(-1);

		expect(childrenAt).toBeGreaterThan(menusAt);

		// backEndComponent(undefined) 返回 undefined（开头 if (!routes) return），所以必须兜底成空数组

		expect(backEnd).toContain('...((await backEndComponent(menus)) || [])');

	});



	it('菜单为空时后续三步仍然执行：加路由、写 routesList、缓存 tagsView', () => {

		const childrenAt = backEnd.indexOf('baseRoutes[0].children = [...dynamicRoutes,');

		const addRouteAt = backEnd.indexOf('await setAddRoute();', childrenAt);

		const listAt = backEnd.indexOf('await setFilterMenuAndCacheTagsViewRoutes();', childrenAt);

		expect(addRouteAt).toBeGreaterThan(childrenAt);

		expect(listAt).toBeGreaterThan(addRouteAt);

	});



	it('只注册首页与默认路由，不给零菜单账号兜底塞业务菜单', () => {

		// 挂进去的只有 dynamicRoutes（首页 + 隐藏的个人中心）加上后端原样返回的菜单

		expect(backEnd).toContain('useRequestOldRoutes().setRequestOldRoutes(JSON.parse(JSON.stringify(menus)));');

		expect(route).toContain(`path: '/home'`);

		expect(route).toContain(`path: '/personal'`);

		// 首页在 dynamicRoutes 里，不依赖后端菜单

		const dynamicAt = route.indexOf('export const dynamicRoutes');

		expect(route.indexOf(`path: '/home'`)).toBeGreaterThan(dynamicAt);

	});



	it('路由守卫仍按 routesList 长度判断是否初始化，不会反复重定向', () => {

		expect(routerIndex).toContain('if (routesList.value.length === 0) {');

		expect(routerIndex).toContain('await initBackEndControlRoutes();');

	});

});

