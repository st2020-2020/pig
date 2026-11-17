# AGENTS.md · 项目地图

pig 私有化部署：单体版后端 + Vue3 前端 + 容器化部署，跑在腾讯云 CVM。

- 线上地址/账号/服务器命令见 `HANDOVER.md`（本地文件，不入库）
- 上游 [pig-mesh/pig](https://github.com/pig-mesh/pig) / [pig-mesh/pig-ui](https://github.com/pig-mesh/pig-ui)，Apache-2.0
- 本仓库为重建的单一初始提交，不含上游历史

| 块 | 技术栈 |
|---|---|
| `pig/` | Spring Boot 4.1.2 + Spring Cloud 2025.1.4，Maven 多模块，JDK 17 |
| `pig-ui/` | Vue 3.5 + Vite 8 + Element Plus，Node ≥ 22.12 |
| `server-deploy/` | compose + nginx，服务器上实际运行的那份 |

## 后端 `pig/`

单体版 = `pig-boot` 把 auth + upms-biz + codegen + quartz 打进一个 jar，**网关和 Nacos 不启动**。
其余模块：`pig-auth` 认证 3000、`pig-upms` 用户权限 4000、`pig-visual` 监控/代码生成/定时任务、`pig-common` 12 个公共库。`pig-gateway` 与 `pig-register` 仅微服务模式用。

```bash
cd pig
mvn -Pboot clean package -DskipTests -Ddocker.skip=true   # 产物 pig-boot/target/pig-boot.jar
mvn clean install -T 4 -Pcloud                            # 微服务全量
mvn -Pboot test -DskipTests=false                         # 测试（不加这个参数什么都不跑，见下）
mvn -pl pig-upms/pig-upms-biz -am test -DskipTests=false  # 单模块，-am 不能省
java -jar pig-boot/target/pig-boot.jar --file.local.base-path=/app/upload
```

常改：站点配置 `pig/pig-upms/pig-upms-biz/src/main/java/com/pig4cloud/pig/admin/service/impl/`（SysSiteConfigServiceImpl）；运行参数 `pig/pig-boot/src/main/resources/application.yml`；种子与 `SITE_*` 参数 `pig/db/pig.sql`；登录与验证码 `pig/pig-auth/src/main/java/com/pig4cloud/pig/auth/support`。

## 前端 `pig-ui/`

```bash
cd pig-ui
npm install      # 用 npm；pnpm-lock 未入库
npm run dev      # :8888，/api 代理到 127.0.0.1:9999
npm run build    # → dist/
npm run test     # vitest 全量，现有 8 个 *.spec.ts；本机直接跑失败，见「本机环境」
```

常改：登录页 `pig-ui/src/views/login/index.vue` 与 `pig-ui/src/theme/login.scss`；主题覆盖 `pig-ui/src/theme/element.scss`；站点配置页 `pig-ui/src/views/admin/siteconfig/index.vue`；store 与兜底值 `pig-ui/src/stores/siteConfig.ts`。
常改续：接口封装 `pig-ui/src/api/admin/`；请求拦截 `pig-ui/src/utils/request.ts`；菜单转路由 `pig-ui/src/router/backEnd.ts`。

## 部署 `server-deploy/`

`server-deploy/docker-compose.yml`（mysql + redis + pig-boot + nginx，对外只开 85）、`server-deploy/pig-ui.conf`（静态站，接口前缀反代到 pig-boot，actuator/druid/swagger 一律 403）、`server-deploy/env.server-actual`（服务器上 .env 实际值）。

## 约定与坑

1. **`VITE_IS_MICRO` 必须 `false`**。仓库里 .env 是上游原值 `true`，服务器是 `false`。`true` 时验证码走 auth 前缀，单体版只有 admin 前缀 —— 验证码必坏、登录必失败。重建前端别覆盖回去。
2. **覆盖 Element Plus 样式要 `!important`**。`element.scss` 的 `.el-button--primary` 规则特异度 0,5,0，普通声明压不过。
3. **`title: null` 要回落**。`SITE_TITLE` 参数行缺失时后端下发 null，`setSiteConfig` 的兜底别删。
4. **后端测试默认被跳过**：`pom.xml` 顶层 `<skipTests>true</skipTests>`，必须 `-DskipTests=false`。
5. **路由由后端菜单驱动**。新增页面除了加 .vue，还得在 `sys_menu` 加记录，`path` 与该页在 `pig-ui/src/views/` 下的目录对应，种子见 `pig/db/pig.sql`。
6. **`.gitignore` 别裸写 upload**。Windows 大小写不敏感，会误伤 `pig-ui/src/components/Upload/`，根规则给运行期上传目录加了 pig/ 前缀限定。
7. **Windows 检出的 mode 变化是噪音**（100755→100644，diff 为 0 行）。用 `git config core.fileMode false` 关掉。
8. **提交风格** `type(scope): summary`；Java 格式 `mvn spring-javaformat:apply`。
9. **别碰宿主机 `adp_*` 容器**（另一套业务，占 81/82/18090/18091）。pig 边界：85 端口、`pig-net`、`pig-*` 容器名、`/opt/pig*`。
10. **直插 SQL 种菜单要清缓存**。菜单/角色/用户走 Redis（键 gl:menu_details、role_details、gl:user_details），直接 INSERT `sys_menu`/`sys_role_menu` 后不清，登录会 403 或看不到菜单。
11. **新增表要给两份脚本**。`pig/db/pig.sql` 是重建用（含 DROP），线上另给可重复执行的升级脚本（见 `pig/db/sys_device_upgrade.sql`、`pig/db/sys_notice_upgrade.sql`：CREATE TABLE IF NOT EXISTS + ON DUPLICATE KEY UPDATE），别拿 `pig/db/pig.sql` 刷线上库。
12. **GET 参数会被请求加密层逐个 toString()**。数组会变成 a,b 绑不上后端类型，日期范围这类要在 api 封装层拆成两个标量参数。
13. **没有可访问菜单的角色登录后首页白屏**。`initBackEndControlRoutes` 在菜单接口返回空数组时直接 return（`pig-ui/src/router/backEnd.ts:46`），`/home` 在 `dynamicRoutes` 里也跟着没注册，首页渲染不出来。给普通角色备验证账号时至少授一个菜单。

## 本机环境

JDK 11（编译不了）、Node 24、**无 Maven / pnpm / Docker**。本机可改代码、跑 eslint 与 vitest；`npm run build` 与 `npm run test` 直接跑都会失败：esbuild 和 vitest 默认的 forks 池在沙箱下起不了子进程（spawn EPERM），属沙箱限制不是代码问题。后端构建与部署在服务器上做。
跑 vitest 要先在 `pig-ui/` 里 `pnpm install --node-linker=hoisted`（默认 junction 布局下 `@vitest/utils` 解析失败），再加 `--pool=threads --configLoader native` 配一份临时精简 config（`include: ['src/**/*.spec.ts']`、`environment: 'node'`；默认加载器 bundle `vite.config.mts` 时也会 spawn EPERM），跑完删掉别提交。
那份临时 config 存成 `.mts` 时没有 `__dirname`，别名要用 `new URL('./src/', import.meta.url).pathname` 并去掉 Windows 前导斜杠。
本机无 Maven 与 `.m2`，改 Java 只能用 `javac -proc:none` 看语法，且必须加 `-encoding UTF-8`——javac 默认按 GBK 读源文件，带中文注释会报一堆「不可映射字符」假错误；真实编译与测试换环境做。
写含中文或非 ASCII 的源文件别用 PowerShell `Get-Content`/`Set-Content`，会破编码；用 node 读写 utf8。
`pig-ui/src/auto-imports.d.ts` 每次跑 vite/vitest 都会被重写成不同的相对路径，属噪音，提交前 `git checkout` 还原。prettier 只对新增文件跑 `--write`，整体跑会顺带改无关文件。

## 功能索引

每个功能一行，细节在各档案里；档案目录 `docs/agent-context/features/`。改这块之前先读档案的「已知限制」。
- 登录页背景与布局 — 一张 backgroud.png、桌面端卡片右移避插画与二维码 — `docs/agent-context/features/登录页背景与布局.md`
- 设备管理 — 系统管理下的设备增删改查页与 `/device` 接口 — `docs/agent-context/features/设备管理.md`
- 系统公告 — 公告管理页、首页公告条与历史公告抽屉、`/notice` 接口 — `docs/agent-context/features/系统公告.md`
- 我的登录记录 — 个人中心第 5 个标签与 `/login-log/page` 只读接口 — `docs/agent-context/features/我的登录记录.md`
