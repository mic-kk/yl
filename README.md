# TCM 中医药电商交易平台

信创合规中医药电商平台脚手架：Spring Boot 3.2（单体模块化）+ Vue3 双基座，达梦 DM8 主库，MyBatis-Plus + Sa-Token。

## 目录结构

| 路径 | 说明 |
|---|---|
| `pom.xml` | tcm-platform-parent 父工程（统一版本管理） |
| `tcm-common/` | 公共基础层：base/exception/utils/audit/lock/mq（禁止依赖业务域） |
| `tcm-trade-platform/` | 单体主工程：领域分包承载全部业务域 + integration 适配层 |
| `tcm-pc-base/` | PC 前端基座（Element Plus）→ 运营端/供应商端/卖家端/B端买家端 |
| `tcm-h5-base/` | H5 移动基座（Vant）→ C端商城/执业药师端 |
| `sql/init.sql` | 建表与种子数据（达梦/MySQL 双兼容） |
| `docs/superpowers/specs/` | SPEC 与实施计划 |

## 架构约定（开发必读，强制）

1. **依赖方向**：业务域 → common；业务域之间只允许调 Service 接口，禁止跨域访问 Mapper/Entity；禁止循环依赖。
2. **适配层**：所有外部系统调用必须走 `com.tcm.integration`，业务代码禁止出现第三方 SDK/HTTP 调用；外部回调必须幂等。
3. **表结构**：雪花主键（IdType.ASSIGN_ID）+ create_time/update_time/create_by/is_deleted；禁止物理删除；达梦/MySQL 双兼容，禁止达梦独有语法。
4. **异常**：业务层只抛 `BusinessException`（携带 ErrorCode 分段错误码）；全局统一返回 `Result<T>`。
5. **审计**：核心操作加 `@AuditLog(module, action, description)`，异步落 tcm_audit_log，禁止删表数据。
6. **并发**：库存扣减、竞拍报价等加 `RedisLockUtil` 分布式锁（锁粒度=业务唯一标识）。
7. **MQ**：异步解耦必须走 RocketMQ；消费者继承 `AbstractMqConsumer` 实现幂等，失败重试，超限进 %DLQ%。
8. **定时任务**：统一 XXL-Job，禁止 Spring Schedule。
9. **规范**：Lombok 实体用 @Getter/@Setter；LocalDateTime；线程池手动创建；魔法值常量/枚举化；@Transactional(rollbackFor = Exception.class)。

## 启动步骤

### 1. 数据库（达梦 DM8）

1. 达梦服务启动（默认端口 5236），创建业务用户 `tcm`（或直接用 SYSDBA，开发环境）；
2. 执行初始化脚本：`disql tcm/<密码>@localhost:5236 < sql/init.sql`；
3. 修改 `tcm-trade-platform/src/main/resources/application-dev.yml` 中的数据源账号密码。

### 2. 后端

```bash
mvn -pl tcm-trade-platform -am spring-boot:run
```

启动后：
- 接口文档：http://localhost:8080/swagger-ui.html
- 登录接口：POST http://localhost:8080/api/auth/login（admin / 123456，响应 Authorization 头携带返回 token）
- 权限 demo：GET http://localhost:8080/api/auth/need-permission（admin 可通过，seller 需 demo:view 权限）

### 3. 前端

```bash
# PC 基座（5173）
cd tcm-pc-base && npm install && npm run dev
# H5 基座（5174）
cd tcm-h5-base && npm install && npm run dev
```

> 注意：`tcm-h5-base` 锁定 `vant@4.9.15`，勿升级至 4.10.x（其发布包错误包含 `workspace:` 协议依赖，npm/pnpm 均无法安装）。

## 中间件启用（可选）

| 组件 | 开关 | 配置位置 |
|---|---|---|
| Redis 分布式锁 | `tcm.infrastructure.redis.enabled=true` | spring.data.redis.* |
| RocketMQ | 配置 `rocketmq.name-server` | 见 rocketmq-spring-boot-starter |
| MinIO | `tcm.infrastructure.minio.enabled=true` | minio.* |
| XXL-Job | `tcm.infrastructure.job.enabled=true` | xxl.job.* |

默认全部关闭，仅依赖达梦即可启动。

## 验收清单（脚手架）

- [x] 包结构合规：common 不依赖业务域；跨域仅 Service 接口
- [x] 全表雪花主键 + 逻辑删除 + 公共字段
- [x] 分页统一 MP 插件（DbType.DM），无达梦独有语法
- [x] 外部对接全部走 integration 适配层（6 个 Mock 实现）
- [x] @AuditLog 审计异步落库（登录接口已挂）
- [x] MQ 消费者幂等/重试/死信约定
- [x] 接口权限：/api/** 全部 Sa-Token 拦截，白名单仅 /api/auth/login
- [x] 全局统一返回 Result + 错误码分段
- [x] 敏感信息脱敏工具（审计日志场景）
