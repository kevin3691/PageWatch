# 页哨 PageWatch

一个 Vue 前端 + 一个 Java 后端的个人网页变化雷达。添加公开网页，预览关注区域，自动检查正文变化，逐行对比历史版本。

## 已实现

- **关注列表**：名称 / 网址搜索，运行中、未读变化、暂停、异常筛选，统计与批量检查。
- **添加关注**：真实 HTTP 抓取，正文预览，自动提取 `main → article → body`，支持关注与忽略 CSS 选择器。
- **变化详情**：版本时间线、左右 / 统一差异视图、完整正文、文本导出、最近 30 次检查记录。
- **任务管理**：手动检查、自动调度、暂停 / 恢复、修改名称与检查间隔、标为已读、删除及关联记录清理。
- **实时状态**：SSE 推送检查开始 / 完成 / 数据变化；断线重连，离线时轮询补偿。
- **真实持久化**：PostgreSQL + Flyway；首次保存基准，之后只有正文变化时保存版本，未变化只写检查记录。
- **内置演示**：明确标记的演示网页，可主动发布更新。关注、快照、差异和检查记录仍走同一套后端与数据库流程。

## 技术栈

| 层 | 版本 / 选择 |
| --- | --- |
| Java | 编译目标 Java 25；开发验证运行于本机 JDK 26.0.2.1 |
| 后端 | Spring Boot 4.1.2、Spring MVC、JdbcClient、Bean Validation |
| 抓取 / 差异 | JDK HttpClient、jsoup 1.24.1、java-diff-utils 4.18 |
| 数据库 | PostgreSQL 18、Flyway |
| 前端 | Vue 3.5.45、Vue Router 5.4.0、Vite 8.3.7 |
| 类型 / 图标 | TypeScript 6.0.3、vue-tsc 3.3.14、@lucide/vue 1.71.0 |

TypeScript 版本选用与当前 Vue 类型检查依赖兼容的 6.x。后端按抓取、存储、调度、事件与接口划分，保持一个应用。前端跨页面共享数据使用 Vue 响应式状态。

## 快速启动

需要 **JDK 25+、Maven 3.9+、Node.js 24 LTS、PostgreSQL 18**。首次启动会下载 Maven / npm 依赖。

在 IDEA 中分别运行 `PageWatchApplication.main()` 和 `frontend/package.json` 的 `dev` 脚本；启动方法和数据库准备见下方「分别启动 / IDEA 开发」。也可在终端运行 `./scripts/dev.sh` 一并启动。本机数据库默认存放在用户目录 `~/.local/share/pagewatch/pgdata`：

- 前端：<http://127.0.0.1:5178>
- 后端健康检查：<http://127.0.0.1:8088/api/health>
- 数据库：`127.0.0.1:55432/pagewatch`，用户 `pagewatch`

IDEA 中用运行窗口的停止按钮停止前后端（终端脚本用 `Ctrl+C`）。数据库与数据会保留，需要时执行：

```bash
./scripts/db.sh status
./scripts/db.sh stop
```

原生数据库脚本使用仅限本机的 trust 认证，适合个人开发；Docker 方案使用密码认证。所有默认监听地址均为 `127.0.0.1`。

### 已有数据库或使用 Docker

Docker 仅用于数据库，前后端仍是两个普通项目：

```bash
docker compose up -d
export DATABASE_URL=jdbc:postgresql://127.0.0.1:55432/pagewatch
export DATABASE_USER=pagewatch
export DATABASE_PASSWORD=pagewatch
./scripts/dev.sh
```

使用其他 PostgreSQL 实例时，先创建独立的 `pagewatch` 数据库，再修改以上环境变量。Flyway 会按 `sql/` 中的版本脚本自动建表和迁移，无需手动执行 SQL。`DATABASE_URL` 已设置时，启动脚本会跳过本地数据库初始化。

### 分别启动 / IDEA 开发

在 IDEA 中打开项目根目录，重新加载根目录 `pom.xml`（`backend` 和 `backend-boot` 两个 Maven 模块），将项目 SDK 和 Maven 导入 JDK 设为 **JDK 25+**。先启动 PostgreSQL：`./scripts/db.sh start`（或使用已有数据库并设置 `DATABASE_URL` 等环境变量）。

- 后端：打开 `backend-boot/src/main/java/dev/pagewatch/PageWatchApplication.java`，直接点击 `main` 方法旁的运行按钮。`backend-boot` 依赖 `backend`，无需运行 Maven 启动脚本。
- 前端：首次在 `frontend` 执行 `npm ci`，然后在 IDEA 的 `frontend/package.json` 中点击 `dev` 脚本旁的运行按钮（`npm run dev`）。

前端地址为 <http://127.0.0.1:5178>。停止前后端后，如需停止数据库，执行 `./scripts/db.sh stop`。

Maven Central 访问不畅时可使用项目自带的镜像示例，不需要改全局 Maven 配置：

```bash
export PAGEWATCH_MAVEN_SETTINGS="$PWD/scripts/maven-settings.example.xml"
./scripts/dev.sh
```

其他可选环境变量见 `.env.example`；它是配置示例，脚本不会自动加载 `.env`。`PORT` 改变时，启动脚本会同步设置前端代理；单独启动前端时需设置 `API_PROXY`。

数据库数据默认位于用户目录 `~/.local/share/pagewatch/pgdata`，Maven / npm 使用各自默认缓存目录；项目目录不存放运行数据。可用 `PAGEWATCH_RUNTIME` 或 `PAGEWATCH_MAVEN_REPO` 覆盖。端口被其他应用占用时，脚本会报错，不会终止其他应用。

## 先体验一次完整流程

1. 首页点击「先体验演示」，进入已填好示例地址的添加页。
2. 点击「预览正文」，确认内容，然后「创建关注」。
3. 详情页显示「首次快照已保存」。
4. 点击「发布演示更新」，自动检查并显示新增与删除的行。
5. 查看「完整正文」「检查记录」，切换时间线，或「标为已读」。

`https://demo.pagewatch.test/changelog` 是后端内置的演示来源，不会向这个域名发请求。演示版本计数在后端重启时从 1 开始；数据库中的历史快照保留。

## 验证

前端类型检查与生产构建：

```bash
cd frontend
npm ci
npm run build
```

后端测试包含 8 项正文提取 / 差异 / 地址限制 / 大小限制测试，以及 4 项真实 PostgreSQL 集成测试。集成测试使用单独的 `pagewatch_test` 数据库，并在事务后回滚。

```bash
./scripts/db.sh test-db
mvn test
mvn package
```

使用自定义测试库时设置 `TEST_DATABASE_URL`、`TEST_DATABASE_USER`、`TEST_DATABASE_PASSWORD`，数据库名必须以 `_test` 结尾。自定义 Maven 镜像和仓库可通过 Maven 的 `-s` 和 `-Dmaven.repo.local` 参数指定。

实际 HTTP 接口冒烟验证，需要先启动后端；只创建并清理自己的测试关注：

```bash
python3 scripts/smoke.py
# 同时等待一次真实的分钟级自动调度（约 60–90 秒）
python3 scripts/smoke.py --scheduler
```

如后端端口不同，设置 `PAGEWATCH_API=http://127.0.0.1:8090/api`。

## 行为与边界

- 首版面向**本机单人使用**，没有账号系统；后端默认只接受本机 Host 和前端端口的来源。
- 抓取公开的静态 HTML / 文本网页，不执行目标网页 JavaScript，不携带登录 Cookie。需要登录、验证码或浏览器渲染的页面留待后续扩展。
- 只允许 HTTP / HTTPS 的 80 / 443 端口；校验 DNS 解析出的地址，并逐次校验重定向，拒绝本机、内网和保留地址。该校验不替代面向公网部署时的网络出口隔离。
- 单页响应最多 2 MB，正文最多 10 万字符 / 3000 行，最多 5 次重定向。连接超时 8 秒，单次请求超时 15 秒。
- 固定 4 个并发抓取槽位，虚拟线程执行；同一关注不会重复并发检查。调度器每 15 秒扫描到期任务，所以检查时间可能比设定晚最多一轮扫描和排队时间。
- 检查失败会记录错误并保留上次成功快照，按原间隔重试。暂停停止后续自动调度，已开始的检查会完成；暂停后仍可手动检查。
- 未读标记需主动清除。检查没有新变化时，已有未读不会被覆盖。
- 保留全部变化快照和最近 100 条检查日志；详情展示最近 30 条。删除关注会删除关联快照和日志。
- 时间以 UTC 存储，前端按浏览器本地时区展示。网页内容只作为纯文本显示，不渲染抓取的 HTML。
- 后端运行时，关闭前端页面不影响定时检查；后端进程停止时调度也会停止，重新启动后检查到期任务。

## 项目结构

参考 B037-HACS 的业务模块 / 启动模块分离和前端目录分工，保留本项目的技术栈及现有 API：

```text
pagewatch/
├── pom.xml                           # Maven 聚合器
├── backend/                          # 业务模块 (JAR)
│   └── src/main/java/dev/pagewatch/
│       ├── monitor/                  # controller → service → component / repository
│       ├── crawl/component/          # 抓取与网址规则
│       └── common/                   # SSE、异常与本地访问控制
├── backend-boot/                     # Spring Boot 启动模块、配置与集成测试
├── sql/                              # PostgreSQL 初始化及后续 Flyway 迁移 SQL
├── frontend/src/
│   ├── api/                          # 请求封装与接口入口
│   ├── router/                       # 路由
│   ├── stores/                       # 关注列表、SSE 与通知状态
│   ├── utils/                        # 日期、网址格式化
│   ├── views/                        # 列表、添加、详情三个页面
│   └── components/                   # 通用组件
├── scripts/                          # 启动、数据库与接口验证
└── compose.yml                       # 可选的 PostgreSQL 容器
```

本项目只有一个关注领域，未引入不必要的 TComponent/ORM 层，也不新增 B037-HACS 的安全、消息队列等业务依赖。

## 后续扩展方向

可视化点选关注区域、Playwright 动态网页抓取、关键词触发、通知渠道、AI 差异摘要，以及公开部署前的账号与权限。这些不在当前版本中。
