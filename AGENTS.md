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

常改：站点配置读写 `pig-upms/pig-upms-biz/.../service/impl/SysSiteConfigServiceImpl.java`；运行参数 `pig-boot/src/main/resources/application{,-dev}.yml`；种子与 `SITE_*` 参数 `db/pig.sql`；登录/验证码 `pig-auth/.../support/`。

## 前端 `pig-ui/`

```bash
cd pig-ui
npm install      # 用 npm；pnpm-lock 未入库
npm run dev      # :8888，/api 代理到 127.0.0.1:9999
npm run build    # → dist/
npm run test     # vitest，现有 siteConfig.spec.ts / login.spec.ts
```

常改：登录页 `src/theme/login.scss`；主题覆盖 `src/theme/element.scss`；站点配置页 `src/views/admin/siteconfig/index.vue`；store 与兜底值 `src/stores/siteConfig.ts`；接口 `src/api/admin/system.ts`；请求拦截 `src/utils/request.ts`；路由 `src/router/backEnd.ts`。

## 部署 `server-deploy/`

`docker-compose.yml`（mysql + redis + pig-boot + nginx，对外只开 85）、`pig-ui.conf`（静态站 + `/api/` 反代，actuator/druid/swagger 一律 403）、`env.server-actual`（服务器上 `.env` 实际值）。

## 约定与坑

1. **`VITE_IS_MICRO` 必须 `false`**。仓库里 `.env` 是上游原值 `true`，服务器是 `false`。`true` 会请求 `/auth/code/image`，单体版是 `/admin/code/image` —— 验证码必坏、登录必失败。重建前端别覆盖回去。
2. **覆盖 Element Plus 样式要 `!important`**。`element.scss` 的 `.el-button--primary` 规则特异度 0,5,0，普通声明压不过。
3. **`title: null` 要回落**。`SITE_TITLE` 参数行缺失时后端下发 null，`setSiteConfig` 的兜底别删。
4. **后端测试默认被跳过**：`pom.xml` 顶层 `<skipTests>true</skipTests>`，必须 `-DskipTests=false`。
5. **路由由后端菜单驱动**。新增页面除了加 `.vue`，还得在 `sys_menu` 加记录，路径形如 `/admin/system/user/index`。
6. **`.gitignore` 别裸写 `upload`**。Windows 大小写不敏感，会误伤 `pig-ui/src/components/Upload/`，根规则用的是 `/pig/upload`。
7. **Windows 检出的 mode 变化是噪音**（100755→100644，diff 为 0 行）。用 `git config core.fileMode false` 关掉。
8. **提交风格** `type(scope): summary`；Java 格式 `mvn spring-javaformat:apply`。
9. **别碰宿主机 `adp_*` 容器**（另一套业务，占 81/82/18090/18091）。pig 边界：85 端口、`pig-net`、`pig-*` 容器名、`/opt/pig*`。

## 本机环境

JDK 11（编译不了）、Node 24、**无 Maven / pnpm / Docker**。本机只能改代码和跑前端 vitest，后端构建与部署在服务器上做。
