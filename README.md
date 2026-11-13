# Base Framework

基于 Java 21 + Spring Boot 3 + MyBatis-Flex + Vue3 的轻量级后端管理脚手架。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.5 |
| ORM | MyBatis-Flex |
| 数据库 | MySQL 8.x（Flyway 管理表结构） |
| 缓存 | Redis (Lettuce) |
| 安全 | Spring Security 6 + JWT |
| 接口文档 | SpringDoc OpenAPI 3 (Swagger) |
| 构建 | Maven |
| 前端 | Vue 3 + Ant Design Vue |

## 项目结构

```
framework/
├── backend/                        # 后端
│   └── src/main/java/com/zc/base/
│       ├── common/                 # 通用层（配置、基础类、异常、工具）
│       ├── security/               # 认证鉴权（JWT、Spring Security）
│       ├── system/                 # 系统管理（用户、角色、菜单）
│       ├── monitor/                # 系统监控（服务器、在线用户）
│       └── log/                    # 操作日志（注解 + AOP）、登录日志
├── fronted-admin/                  # 前端管理端
├── docker-compose.yml              # 本地开发用的 MySQL 和 Redis
├── init-project.sh                 # 项目模板化脚本
└── README.md
```

## 快速开始

### 环境要求

- JDK 21（后端用 JDK 21 构建和运行）
- Maven 3.9+
- Docker（用来起 MySQL 和 Redis；自己装了 MySQL 8 和 Redis 6+ 也可以）
- Node.js 18+

### 后端启动

```bash
# 1. 启动 MySQL 8（root/root，自动建库 base_dev）和 Redis（无密码）
docker compose up -d

# 2. 启动后端。首次启动时 Flyway 自动执行 db/migration 下的脚本建表并写入初始数据
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS；Linux 自行指向 JDK 21
mvn spring-boot:run
```

不用 docker 时，先自己建一个空库（库名、账号、Redis 地址见 `application-dev.yml`），不要手工导 SQL。

### 前端启动

```bash
cd fronted-admin
npm install
npm run dev
```

### 访问地址

- 前端: http://localhost:3000
- 接口文档: http://localhost:8080/swagger-ui.html
- 默认账号: admin / admin123

## 基于脚手架创建新项目

```bash
./init-project.sh <项目名> <包名> <目标路径>

# 示例
./init-project.sh my-app com.zc.myapp ~/IdeaProjects/my-app
```

脚本自动完成包名替换、类名重命名、Maven/前端项目名替换、应用名和数据库名替换、Git 初始化。

## 功能模块

| 模块 | 说明 |
|---|---|
| 认证管理 | 登录/登出/刷新Token，JWT + Spring Security |
| 登录防护 | 图形验证码、连续输错密码锁定、密码强度校验（至少 8 位，同时含字母和数字） |
| 个人中心 | 当前用户查看/修改自己的资料、修改密码（改完所有已签发的 token 失效，需重新登录） |
| 权限控制 | RBAC：用户 - 角色 - 菜单，接口用权限标识控制，前端侧边栏和按钮按权限显示 |
| 用户管理 | 用户 CRUD、角色分配、密码重置（新建用户必须填密码，重置后该用户的 token 全部失效） |
| 角色管理 | 角色 CRUD、菜单（含按钮）权限分配 |
| 菜单管理 | 目录 / 菜单 / 按钮的树形维护，按钮上的权限标识就是接口权限 |
| 操作日志 | 在 Controller 方法上加 @OperationLog 即自动记录 |
| 登录日志 | 登录成功、失败都记录用户名、IP、浏览器、操作系统 |
| 系统监控 | CPU/内存/磁盘/JVM 信息、在线用户管理 |
| 接口文档 | SpringDoc 自动生成，支持在线调试 |

## 配置说明

### 权限说明

只有 RBAC 一种模式。

- **权限标识**格式为 `模块:资源:操作`，如 `system:user:list`、`system:user:add`、`system:user:resetPwd`。标识写在菜单表（`sys_menu.permission`）里，菜单和按钮都可以带。
- **超级管理员**：角色标识为 `ADMIN` 的用户拥有全部菜单的权限标识，接口上不需要再写 `or hasRole('ADMIN')`。
- **普通角色**只拿到角色分配的菜单上的权限标识。
- **权限缓存**：每次请求鉴权用到的用户信息（角色、权限标识）缓存在 Redis，缓存时间见下文 `user-cache-minutes`。通过本系统的接口改用户、角色、菜单会立即让相关缓存失效，权限马上生效；直接改数据库（包括 Flyway 脚本改菜单）不会触发失效，最长 `user-cache-minutes` 分钟后生效，要立即生效就清掉 Redis 里的 `auth:user` 这个 key。
- **前端**：侧边栏由 `GET /api/auth/menus` 返回的菜单树渲染；路由守卫要求目标页面在该菜单树里，否则跳 `/403`；只要登录就能访问、不在菜单树里的页面（如个人中心），在路由 meta 里写 `skipMenuCheck: true`；按钮用 `v-permission="'system:user:add'"` 控制显示。

新增一个带权限的业务接口：

1. 在菜单管理页（或 Flyway 新脚本）里加菜单：页面建一个 `M` 类型菜单，页面上的每个操作建一个 `B` 类型按钮，填好权限标识，如 `order:order:list`、`order:order:add`。
2. 在后端 Controller 方法上写 `@PreAuthorize("hasAuthority('order:order:add')")`，增删改再加 `@OperationLog`。
3. 在角色管理页给需要的角色勾选这些菜单。`ADMIN` 自动拥有。通过页面或接口改菜单、角色后权限立即生效；用 Flyway 脚本直接改菜单表时，缓存不会失效，需要在脚本发布后清掉 Redis 里的 `auth:user`，或等缓存过期。
4. 新页面需要在 `fronted-admin/src/router/index.ts` 加静态路由，路径要和菜单的路由地址一致；页面里的按钮加 `v-permission`。

### 登录防护与缓存配置

```yaml
# application.yml
base:
  security:
    captcha:
      enabled: true          # 登录图形验证码，false 时登录不需要验证码，前端不显示
    login:
      max-fail-count: 5      # 同一用户名连续密码错误几次后锁定（用户名不存在也计数）
      lock-minutes: 15       # 锁定多少分钟；失败计数在最后一次失败后同样保留这么久
    user-cache-minutes: 30   # 鉴权用户信息的 Redis 缓存时间
    ignore-urls:
      - /api/auth/captcha    # 获取验证码，免认证；自己覆盖 ignore-urls 时要保留
```

- 验证码答案存 Redis，2 分钟过期，校验一次就删除，不区分大小写。
- 锁定期间直接拒绝登录，不校验密码，提示剩余等待时间。要提前解锁，删掉 Redis 里的 `login:lock:<小写用户名>`。
- 新增 Redis key：`captcha:*`、`login:fail:*`、`login:lock:*`、`auth:user`（Hash）、`auth:user:epoch`。

### 数据库迁移

表结构和初始数据由 Flyway 管理，脚本放在 `backend/src/main/resources/db/migration`，文件名 `V<版本号>__<说明>.sql`。应用启动时自动执行未执行过的脚本。已执行的脚本不要再改，表结构变更新增一个版本。数据库本身需要提前存在。

### 日志配置

```yaml
# application.yml
logging:
  file:
    path: ./logs                              # 日志文件路径
  logback:
    rollingpolicy:
      max-history: 30                         # 日志保留天数
```

每日生成两份日志文件：全量日志 + 错误日志（仅 ERROR）。

---

## 开发规范

### 包结构规范

新增业务模块时，在 `com.zc.base` 下创建独立包，内部按 controller / service / mapper / entity / dto 分层：

```
com.zc.base.{模块名}/
├── controller/     # 接口层，只做参数校验和调用 Service
├── service/        # 业务逻辑层
├── mapper/         # 数据访问层（继承 BaseMapper）
├── entity/         # 实体类（对应数据库表，继承 BaseEntity）
└── dto/            # 数据传输对象（请求/响应参数）
```

### 编码规范

1. **空值处理**：使用 `Optional` 或专属判空工具类，禁止裸 `null` 判断
2. **实体类**：所有业务实体继承 `BaseEntity`，主键由雪花算法自动生成，禁止手动赋值
3. **响应格式**：Controller 统一返回 `R<T>`，使用 `R.success()` / `R.error()`
4. **异常处理**：业务异常抛出 `BusinessException`，由全局异常处理器统一捕获，禁止 Controller 中 try-catch 后手动构建错误响应
5. **参数校验**：使用 `@Valid` + JSR 380 注解（`@NotBlank`、`@Size` 等），校验逻辑不要写在 Service 中
6. **接口路径**：RESTful 风格，统一前缀 `/api/{模块}/{资源}`
7. **权限注解**：Controller 方法上标注 `@PreAuthorize("hasAuthority('模块:资源:操作')")`，不要用 `hasRole()`
8. **操作日志**：所有增删改接口加 `@OperationLog(module = "xxx", description = "xxx")`；请求里带密码的接口必须加 `saveParams = false`
9. **接口文档**：Controller 类加 `@Tag(name = "xxx")`，方法加 `@Operation(summary = "xxx")`
10. **Service 写法**：Service 直接写成类，不拆接口和 `Impl`。只有存在多个实现（如本地存储与对象存储），或需要隔离包之间的依赖时才抽接口

### 数据库规范

1. 表名统一小写，下划线分隔，业务表带模块前缀（如 `order_item`），系统表用 `sys_` 前缀
2. 主键字段名统一为 `id`，类型 `BIGINT`
3. 必须包含 `create_time`、`update_time` 字段
4. 逻辑删除字段为 `deleted`（0 未删除 / 1 已删除）
5. 字段注释必须填写

### Git 规范

提交信息格式：`<type>: <description>`

```
feat: 新增用户导出功能
fix: 修复登录日志IP获取错误
refactor: 重构角色权限查询逻辑
docs: 补充接口文档注解
```
