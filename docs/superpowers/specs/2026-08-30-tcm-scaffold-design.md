# TCM 中医药电商交易平台 — 脚手架搭建 SPEC

- 日期：2026-08-30
- 状态：已评审通过，进入开发计划阶段
- 目标读者：开发团队

## 1. 背景与目标

中医药电商交易平台（后称 TCM 平台）为信创合规业务：一套后端服务对外提供 6 个独立前端终端（平台运营端、供应商端、卖家端、B 端买家端、C 端商城、执业药师 H5 端），承载大宗/集采/竞拍三类 B 端交易与 C 端购药（含处方药审方），并对接 WMS/TMS/溯源/ERP/银联支付/税务药监等外部系统。

**本期范围：脚手架搭建**（非业务实现）。目标：

1. 建立标准 Maven 多模块工程骨架（父 POM + 公共模块 + 单体主工程），版本统一管理；
2. 完成公共基础层 tcm-common：Base 基类、企业级统一返回、企业级异常体系、审计日志、分布式锁、MQ 工具、雪花 ID；
3. 主工程跑通：达梦 DM8 数据源 + MyBatis-Plus + Sa-Token 登录鉴权 demo；
4. 前端双基座：tcm-pc-base（Element Plus）、tcm-h5-base（Vant）；
5. 沉淀《架构约定》于 README，作为后续所有业务模块开发的强制规范。

## 2. 非目标（本期明确不做）

- 11 个业务模块（M1–M11）的业务逻辑；仅建空骨架包
- 外部系统真实对接（WMS/TMS/溯源/ERP/银联/监管）；仅建 Adapter 接口 + Mock 实现
- 微服务拆分、Spring Cloud
- 监控告警（Prometheus，二期）
- 代码生成器落地（MyBatis-Plus Generator 模板化，后续迭代）

## 3. 技术选型矩阵（父 POM 统一锁定，禁止子模块私自指定版本）

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | LTS，本机 17.0.16 |
| Spring Boot | 3.2.12 | 3.2.x 末版 |
| MyBatis-Plus | 3.5.7 | mybatis-plus-spring-boot3-starter，分页插件内置 |
| Sa-Token | 1.37.0 | sa-token-spring-boot3-starter |
| 达梦驱动 | DmJdbcDriver18 8.1.2.192 | Maven Central 直拉；版本不匹配时 README 提供本地安装方案 |
| Hutool | 5.8.32 | 工具集 |
| Redisson | 3.27.2 | 分布式锁 |
| RocketMQ | rocketmq-spring-boot-starter 2.3.0 | Boot 3 兼容 |
| MinIO SDK | 8.5.10 | 文件存储客户端 |
| XXL-Job | 2.4.0 | 任务调度客户端 |
| springdoc-openapi | 2.5.0 | Swagger UI，企业级接口文档 |
| spring-security-crypto | 随 Boot 管理 | 仅 BCrypt，不引入完整 Security |
| Lombok | 随 Boot 管理 | 用 @Getter/@Setter，慎用 @Data |

前端：Vue 3.4+ / Vite 5 / Element Plus / Vant 4 / Pinia / Vue Router / Axios / ESLint+Prettier。

## 4. 工程结构

```
ylxm/
├── pom.xml                    # tcm-platform-parent（packaging=pom）
├── tcm-common/                # 公共基础层 jar，禁止依赖业务域
├── tcm-trade-platform/        # 单体主工程 jar，依赖 tcm-common
├── sql/init.sql               # 达梦建库建表 + 种子数据
├── tcm-pc-base/               # PC 前端基座
├── tcm-h5-base/               # H5 前端基座
└── README.md                  # 架构约定 + 启动手册
```

## 5. tcm-common 设计（核心交付物）

包结构 `com.tcm.common`：

### 5.1 base — 基础类
- `BaseEntity`：`id`（雪花 Long）、`createTime`、`updateTime`、`createBy`、`isDeleted`；`@TableLogic` 逻辑删除；`@TableField(fill=...)` 自动填充。
- `Result<T>`：`code/msg/data/timestamp`，静态工厂 `ok()`、`ok(data)`、`fail(code,msg)`。全局统一返回结构。
- `PageResult<T>`：`records/total/current/size`，静态工厂 `of(IPage)`。

### 5.2 exception — 企业级异常体系
- `ErrorCode` 枚举，分段错误码：
  - `1xxx` 通用（1000 系统繁忙、1001 参数错误占位）
  - `2xxx` 鉴权（2001 未登录、2002 无权限、2003 登录过期）
  - `3xxx` 参数校验（3001 参数非法，由校验异常映射）
  - `4xxx` 业务错误（各业务域预留段位）
  - 每项含 code、message，支持自定义 message。
- `BusinessException`：携带 ErrorCode + 可选 message，业务层唯一允许抛出的受检风格异常（运行时）。
- `GlobalExceptionHandler`（@RestControllerAdvice）统一处理：BusinessException、MethodArgumentNotValidException、BindException、ConstraintViolationException、HttpMessageNotReadableException、Sa-Token 鉴权异常（SaTokenException/NotLoginException 等）、兜底 Exception（记 error 日志）。返回统一 Result 结构。

### 5.3 utils — 工具类
- `SnowflakeIdUtil`：Hutool Snowflake 封装，线程安全，全局唯一主键生成（与达梦/MySQL 双兼容，后期分库分表无冲突）。

### 5.4 audit — 审计日志组件
- `@AuditLog(module, action, description)` 方法注解。
- `AuditLogAspect`：环绕切面。取操作人（Sa-Token LoginId → createBy），记录：模块、动作、描述、请求参数（脱敏敏感字段如 password）、响应摘要、执行时长、修改前后快照（对象字段 diff 摘要）、操作人、IP、时间。
- 异步落库 `tcm_audit_log`（独立线程池，队列有界，拒绝策略丢弃并告警日志）；**写库失败仅记日志，不阻断业务**。
- 禁止物理删除审计表数据（验收项）。

### 5.5 lock — 分布式锁
- `RedisLockUtil`：Redisson 封装。API：`tryLock(key, wait, lease)`、`unlock(key)`、回调式 `executeWithLock(key, supplier)`。锁粒度约定：锁定业务唯一标识。

### 5.6 mq — 消息队列工具
- `MqProducer`：统一发送（RocketMQ 封装），消息体携带 `traceId`。
- `MqConsumer` 抽象基类：内置幂等（Redis setnx 业务唯一 ID）、失败重试（次数上限）、超限进死信队列约定。

### 5.7 constant
- `CommonConstant`：通用常量。
- 不引入业务域类（强制约束）。
- 说明：MyBatis-Plus 分页插件等数据源相关配置放主工程 config 包（见 6.1），common 仅承载与数据库无关的公共能力。

## 6. tcm-trade-platform 设计

### 6.1 包结构（com.tcm 根包，领域分包）

```
com.tcm
├── TradePlatformApplication
├── common            # 通过启动类 @ComponentScan 扫描
├── integration       # 适配层：wms/tms/trace/erp/pay/regulatory
│                      # 每个 = XxxAdapter 接口 + MockImpl（占位打日志）
├── user              # M1 账号&资质：登录 demo
├── goods             # M2 商品中心（空骨架）
├── trade             # M3 交易中心（空骨架）
├── order             # M4 订单履约（空骨架）
├── inventory         # M5 平台托管库存（空骨架）
├── prescription      # M6 处方&审方（空骨架）
├── settle            # M7 财务结算（空骨架）
├── trace             # M8 溯源监控（空骨架）
├── regulatory        # M9 监管上报（空骨架）
├── mall              # M10 商城运营&售后（空骨架）
└── config            # SaTokenConfig、MybatisPlusConfig、AsyncConfig 等
```

强制约束（写进 README，验收项）：
- 业务域之间仅允许调用 Service 接口；禁止跨域访问 Mapper/Entity
- 所有外部调用必须走 integration 适配层
- Controller 不写业务逻辑、不直接调 Mapper；Service 禁止拼接 SQL
- Entity 禁止直接返回前端；VO 只进不出

### 6.2 Sa-Token 登录 demo（M1 user 域）

- `AuthController`：`POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/info`
- `sys_user` 表（种子数据 admin/123456，BCrypt 存储）
- `SysUser` Entity/Mapper/Service
- 权限注解 demo：`@SaCheckLogin`、`@SaCheckPermission` 示例接口
- SaTokenConfig：拦截器注册、白名单（/api/auth/login、/doc.html、swagger 资源、actuator/health）

### 6.3 配置

- `application.yml`（公共）、`application-dev.yml`（达梦本机 5236）、`application-prod.yml`（占位）
- 基础设施**可选启动**：Redis（锁）、RocketMQ、MinIO、XXL-Job 均 `@ConditionalOnProperty` 控制，默认仅依赖 DM8 即可启动

### 6.4 API 文档

- springdoc-openapi 接入，`/doc.html` 或 `/swagger-ui.html` 可访问

## 7. 数据库脚本 sql/init.sql

- 建库 `tcm`（若达梦需实例级操作则提供说明）
- `tcm_user`：雪花主键、账号唯一索引、密码 BCrypt、状态、时间字段、逻辑删除
- `tcm_audit_log`：雪花主键、module/action/description、操作人、参数快照、前后快照、IP、耗时、时间；**禁止物理删除**
- 索引命名：`idx_`前缀；唯一索引 `uk_`前缀

## 8. 前端基座设计

### 8.1 tcm-pc-base（Vue3 + Vite5 + Element Plus + Pinia + Vue Router + Axios）

- 登录页（表单校验 → 调 /api/auth/login → 存 token）
- Layout：侧边菜单（动态渲染预留，菜单数据由后端返回）+ 顶栏（用户名/退出）
- 路由守卫：无 token 跳登录；401 统一登出处理
- `utils/request.ts`：axios 实例，token 头、业务错误码统一 ElMessage、401 处理
- stores：user（token/userInfo）、menu（预留）
- `.env.development`：VITE_API_BASE=/api，vite proxy → http://localhost:8080

### 8.2 tcm-h5-base（Vue3 + Vite5 + Vant4 + Pinia）

- 登录页 + 首页 + request 封装 + 路由守卫 + 移动端适配（postcss-px-to-viewport 或 rem）

## 9. 里程碑

| # | 内容 | 验收 |
|---|---|---|
| M1 | 父POM + 三模块骨架 + 版本矩阵 | `mvn compile` 通过 |
| M2 | tcm-common 全部组件 | 编译通过 + 单元自测（异常处理/雪花ID） |
| M3 | 主工程 + DM8 数据源 + MP + 启动类 | 本机 DM8 连接启动成功 |
| M4 | Sa-Token 登录 demo + init.sql | 登录接口可调通 |
| M5 | tcm-pc-base | npm run build 通过，登录页联调 |
| M6 | tcm-h5-base | npm run build 通过 |
| M7 | README + 验收清单核对 | 全部验收项通过 |

## 10. 强制验收清单（本次脚手架必过）

1. 包结构合规：无跨层、跨模块违规调用；common 不依赖业务域
2. 所有表雪花主键 + 逻辑删除 + 时间/操作人字段
3. 未使用达梦独有语法/函数/存储过程，分页走 MP 插件
4. 外部系统对接全部经 integration 适配层
5. 核心操作（demo 接口）有审计日志且异步落库
6. MQ 消费者有幂等/重试/死信约定
7. 接口权限：登录才可访问 demo 接口，白名单仅审批项
8. 无硬编码魔法值（常量/枚举化）
9. 遵循阿里巴巴开发手册（Lombok 不用 @Data 于实体、线程池手动创建、LocalDateTime、事务 rollbackFor）
10. 错误码分段清晰，异常统一出口
