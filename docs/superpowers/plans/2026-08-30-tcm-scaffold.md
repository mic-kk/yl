# TCM 平台脚手架 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建中医药电商交易平台（TCM）的后端 Maven 多模块脚手架（父 POM + tcm-common 公共层 + tcm-trade-platform 单体主工程）与前端双基座（tcm-pc-base / tcm-h5-base），跑通达梦 DM8 数据源、MyBatis-Plus、Sa-Token 登录鉴权 demo。

**Architecture:** 模块化单体。仓库根为父 POM（统一版本管理）；`tcm-common` 提供 Base 基类、统一返回、企业级异常体系、审计、分布式锁、MQ 工具等横向公共能力（不依赖任何业务域、不依赖 Sa-Token）；`tcm-trade-platform` 按领域分包承载全部业务域与适配层；前端为两个独立 Vite 工程，通过 `/api` 代理对接后端。

**Tech Stack:** JDK 17 / Spring Boot 3.2.12 / MyBatis-Plus 3.5.7 / Sa-Token 1.37.0 / 达梦 DmJdbcDriver18 / Hutool 5.8.32 / Redisson 3.27.2 / RocketMQ 2.3.0 / springdoc 2.5.0；Vue 3.4 / Vite 5 / Element Plus / Vant 4 / Pinia / Axios。

**Spec:** [docs/superpowers/specs/2026-08-30-tcm-scaffold-design.md](../specs/2026-08-30-tcm-scaffold-design.md)

## Global Constraints

- JDK 17；Spring Boot 3.2.12；MyBatis-Plus `mybatis-plus-spring-boot3-starter` 3.5.7；Sa-Token `sa-token-spring-boot3-starter` 1.37.0；达梦 `com.dameng:DmJdbcDriver18:8.1.2.192`（Maven Central 直拉）；Hutool 5.8.32；`org.redisson:redisson` 3.27.2；`rocketmq-spring-boot-starter` 2.3.0；MinIO SDK 8.5.10；xxl-job-core 2.4.0；springdoc-openapi-starter-webmvc-ui 2.5.0。所有版本只在父 POM `dependencyManagement` 声明，子模块禁止私自指定版本。
- 包根 `com.tcm`。`com.tcm.common` 禁止依赖任何业务域包、禁止依赖 Sa-Token；业务域之间只允许调 Service 接口；外部系统调用必须走 `com.tcm.integration`。
- 所有业务表：雪花主键（`IdType.ASSIGN_ID`）+ `create_time/update_time/create_by/is_deleted`；禁止物理删除；逻辑删除字段 `is_deleted`。
- 禁止达梦独有语法/函数/存储过程；分页统一 MyBatis-Plus 分页插件（`DbType.DM`）；字段类型对齐 MySQL 标准（VARCHAR/DECIMAL/INT/TINYINT/TEXT/TIMESTAMP）。
- Lombok 实体用 `@Getter/@Setter`（禁止 `@Data` 于实体）；日期用 `LocalDateTime`；线程池手动创建、命名前缀；`@Transactional(rollbackFor = Exception.class)`。
- 所有接口统一返回 `Result<T>`；业务异常只抛 `BusinessException`；魔法值必须枚举/常量化。
- 中间件（Redis/MQ/MinIO/XXL-Job）全部通过 `tcm.infrastructure.*.enabled` 条件装配，**默认关闭，仅依赖 DM8 即可启动**。
- 审计：核心操作加 `@AuditLog`，异步落库 `tcm_audit_log`，写库失败仅记日志不阻断业务。
- MQ 消费者必须幂等（Redis 去重）、失败重试、超限进死信。
- 前端：Vue3 组合式 API + `<script setup>`；所有请求走 `utils/request.js`；菜单/按钮权限后端返回；敏感信息禁止本地存储与日志打印。
- 提交粒度：每个 Task 独立 commit，消息 `feat: ...` / `docs: ...` 前缀。

---

### Task 1: 父 POM 与 Maven 模块骨架

**Files:**
- Create: `pom.xml`（tcm-platform-parent）
- Create: `tcm-common/pom.xml`
- Create: `tcm-trade-platform/pom.xml`
- Create: `.gitignore`

**Interfaces:**
- Consumes: 无
- Produces: `com.tcm` groupId、模块坐标 `tcm-common` / `tcm-trade-platform`、全部依赖版本锁定（后续所有 Task 直接引用，不再声明版本）

- [ ] **Step 1: 写根 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.tcm</groupId>
    <artifactId>tcm-platform-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>tcm-platform-parent</name>
    <description>TCM 中医药电商交易平台 - 父工程（统一版本管理）</description>

    <modules>
        <module>tcm-common</module>
        <module>tcm-trade-platform</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.2.12</spring-boot.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <sa-token.version>1.37.0</sa-token.version>
        <dameng-driver.version>8.1.2.192</dameng-driver.version>
        <hutool.version>5.8.32</hutool.version>
        <redisson.version>3.27.2</redisson.version>
        <rocketmq-spring.version>2.3.0</rocketmq-spring.version>
        <minio.version>8.5.10</minio.version>
        <xxl-job.version>2.4.0</xxl-job.version>
        <springdoc.version>2.5.0</springdoc.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- MyBatis-Plus (Boot3 专用 starter) -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <!-- Sa-Token -->
            <dependency>
                <groupId>cn.dev33</groupId>
                <artifactId>sa-token-spring-boot3-starter</artifactId>
                <version>${sa-token.version}</version>
            </dependency>
            <!-- 达梦驱动（Maven Central） -->
            <dependency>
                <groupId>com.dameng</groupId>
                <artifactId>DmJdbcDriver18</artifactId>
                <version>${dameng-driver.version}</version>
            </dependency>
            <!-- Hutool 工具集 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <!-- Redisson（仅核心包，自动装配由 tcm-common 条件控制） -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson</artifactId>
                <version>${redisson.version}</version>
            </dependency>
            <!-- RocketMQ Spring Boot starter -->
            <dependency>
                <groupId>org.apache.rocketmq</groupId>
                <artifactId>rocketmq-spring-boot-starter</artifactId>
                <version>${rocketmq-spring.version}</version>
            </dependency>
            <!-- MinIO SDK -->
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
            </dependency>
            <!-- XXL-Job -->
            <dependency>
                <groupId>com.xuxueli</groupId>
                <artifactId>xxl-job-core</artifactId>
                <version>${xxl-job.version}</version>
            </dependency>
            <!-- springdoc API 文档 -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
            <!-- 模块间依赖 -->
            <dependency>
                <groupId>com.tcm</groupId>
                <artifactId>tcm-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>1.18.32</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 写 tcm-common/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.tcm</groupId>
        <artifactId>tcm-platform-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>tcm-common</artifactId>
    <name>tcm-common</name>
    <description>TCM 公共基础层：base/exception/utils/audit/lock/mq/constant</description>

    <dependencies>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: 写 tcm-trade-platform/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.tcm</groupId>
        <artifactId>tcm-platform-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>tcm-trade-platform</artifactId>
    <name>tcm-trade-platform</name>
    <description>TCM 单体主工程：领域分包承载全部业务域与适配层</description>

    <dependencies>
        <dependency>
            <groupId>com.tcm</groupId>
            <artifactId>tcm-common</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.dev33</groupId>
            <artifactId>sa-token-spring-boot3-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.dameng</groupId>
            <artifactId>DmJdbcDriver18</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>
        <!-- 以下依赖仅随 classpath 携带，Bean 装配由 tcm.infrastructure.*.enabled 控制 -->
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
        </dependency>
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: 写 .gitignore**

```
target/
node_modules/
dist/
.idea/
*.iml
.vscode/
*.log
.env.local
```

- [ ] **Step 5: 验证并提交**

Run: `mvn -q validate`（工作目录 `f:/MyProject/ylxm`）
Expected: BUILD SUCCESS（无代码也可 validate，此时不触发依赖下载）

```bash
git add -A && git commit -m "feat: 父POM与Maven模块骨架（版本矩阵锁定）"
```

---

### Task 2: tcm-common — base 基础类（BaseEntity / Result / PageResult）

**Files:**
- Create: `tcm-common/src/main/java/com/tcm/common/constant/CommonConstant.java`
- Create: `tcm-common/src/main/java/com/tcm/common/base/BaseEntity.java`
- Create: `tcm-common/src/main/java/com/tcm/common/base/Result.java`
- Create: `tcm-common/src/main/java/com/tcm/common/base/PageResult.java`
- Test: `tcm-common/src/test/java/com/tcm/common/base/ResultTest.java`
- Test: `tcm-common/src/test/java/com/tcm/common/base/PageResultTest.java`

**Interfaces:**
- Consumes: 无（CommonConstant 先行）
- Produces: `Result<T>`（getCode/getMsg/getData + ok()/ok(data)/fail(ErrorCode)/fail(code,msg)）、`PageResult<T>`（of(IPage)）、`BaseEntity`（继承即可获得 id/createTime/updateTime/createBy/isDeleted）

- [ ] **Step 1: 写 CommonConstant**

```java
package com.tcm.common.constant;

/** 全平台通用常量。业务常量放各自业务域 constant 包。 */
public final class CommonConstant {

    private CommonConstant() {
    }

    /** 统一返回：成功码 */
    public static final int SUCCESS_CODE = 200;
    /** 逻辑删除：未删除 */
    public static final int NOT_DELETED = 0;
    /** 逻辑删除：已删除 */
    public static final int DELETED = 1;
    /** 状态：启用 */
    public static final int STATUS_ENABLED = 1;
    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;
    /** 分页默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;
    /** 分页默认页大小 */
    public static final long DEFAULT_PAGE_SIZE = 10L;
}
```

- [ ] **Step 2: 写 BaseEntity**

```java
package com.tcm.common.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库实体基类。
 * 强制约定：所有业务表必须包含 id(雪花)、create_time、update_time、create_by、is_deleted。
 */
@Getter
@Setter
public abstract class BaseEntity implements Serializable {

    /** 雪花主键，应用层生成，禁止数据库自增 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 逻辑删除：0 未删 / 1 已删，禁止物理删除 */
    @TableLogic
    private Integer isDeleted;
}
```

- [ ] **Step 3: 写 Result**

```java
package com.tcm.common.base;

import com.tcm.common.constant.CommonConstant;
import com.tcm.common.exception.ErrorCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 全局统一返回结构。所有 Controller 方法必须返回 Result，禁止直接返回裸对象。
 */
@Getter
public class Result<T> implements Serializable {

    private final int code;
    private final String msg;
    private final T data;
    private final long timestamp;

    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> ok() {
        return new Result<>(CommonConstant.SUCCESS_CODE, "success", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CommonConstant.SUCCESS_CODE, "success", data);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return this.code == CommonConstant.SUCCESS_CODE;
    }
}
```

- [ ] **Step 4: 写 PageResult**

```java
package com.tcm.common.base;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回结构。
 */
@Getter
public class PageResult<T> implements Serializable {

    private final List<T> records;
    private final long total;
    private final long current;
    private final long size;

    private PageResult(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records == null ? Collections.emptyList() : records, total, current, size);
    }
}
```

- [ ] **Step 5: 写测试 ResultTest / PageResultTest**

`ResultTest`：ok() 返回 code=200、data=null；ok(data) 回传数据；fail(ErrorCode) 携带枚举 code/message；isSuccess() 判定正确。

```java
package com.tcm.common.base;

import com.tcm.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void ok_shouldReturnSuccess() {
        Result<Void> result = Result.ok();
        assertTrue(result.isSuccess());
        assertEquals(200, result.getCode());
        assertNull(result.getData());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    void okWithData_shouldCarryData() {
        Result<String> result = Result.ok("hello");
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getData());
    }

    @Test
    void failWithErrorCode_shouldCarryCodeAndMessage() {
        Result<Void> result = Result.fail(ErrorCode.NOT_LOGIN);
        assertFalse(result.isSuccess());
        assertEquals(2001, result.getCode());
        assertEquals(ErrorCode.NOT_LOGIN.getMessage(), result.getMsg());
    }

    @Test
    void failWithCustomMessage_shouldUseCustomMessage() {
        Result<Void> result = Result.fail(ErrorCode.LOGIN_ERROR, "账号已被锁定");
        assertEquals(2003, result.getCode());
        assertEquals("账号已被锁定", result.getMsg());
    }
}
```

`PageResultTest`：of(records,total,current,size) 字段正确；records 为 null 时返回空列表不抛 NPE。

```java
package com.tcm.common.base;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void of_shouldFillFields() {
        List<String> records = Arrays.asList("a", "b");
        PageResult<String> page = PageResult.of(records, 100, 1, 10);
        assertEquals(2, page.getRecords().size());
        assertEquals(100, page.getTotal());
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
    }

    @Test
    void ofWithNullRecords_shouldNotThrow() {
        PageResult<String> page = PageResult.of(null, 0, 1, 10);
        assertNotNull(page.getRecords());
        assertTrue(page.getRecords().isEmpty());
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `mvn -q -pl tcm-common test`
Expected: BUILD SUCCESS（4 个测试全过；首次运行会下载依赖，耗时较长属正常）

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "feat(common): base 基础类 BaseEntity/Result/PageResult 与常量"
```

---

### Task 3: tcm-common — 企业级异常体系（ErrorCode / BusinessException / GlobalExceptionHandler）

**Files:**
- Create: `tcm-common/src/main/java/com/tcm/common/exception/ErrorCode.java`
- Create: `tcm-common/src/main/java/com/tcm/common/exception/BusinessException.java`
- Create: `tcm-common/src/main/java/com/tcm/common/exception/GlobalExceptionHandler.java`
- Test: `tcm-common/src/test/java/com/tcm/common/exception/ErrorCodeTest.java`
- Test: `tcm-common/src/test/java/com/tcm/common/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `Result`、`CommonConstant`（Task 2）
- Produces: `ErrorCode` 枚举（SUCCESS=200 / SYSTEM_ERROR=1000 / NOT_LOGIN=2001 / NO_PERMISSION=2002 / LOGIN_ERROR=2003 / PARAM_ERROR=3001 / USER_DISABLED=4001）；`BusinessException(ErrorCode)` 与 `BusinessException(ErrorCode, String)`；`GlobalExceptionHandler`（@RestControllerAdvice，返回统一 Result JSON）。Sa-Token 相关异常不在 common 处理（由主工程 Task 9 的 SaTokenExceptionHandler 负责，异常类型更具体，不会冲突）。

- [ ] **Step 1: 写 ErrorCode**

```java
package com.tcm.common.exception;

import lombok.Getter;

/**
 * 全局错误码枚举（分段约定）：
 * 200 成功；1xxx 通用；2xxx 鉴权；3xxx 参数校验；4xxx 业务（各业务域在后续迭代各自扩充分段）。
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),

    /** 1xxx 通用 */
    SYSTEM_ERROR(1000, "系统繁忙，请稍后重试"),

    /** 2xxx 鉴权 */
    NOT_LOGIN(2001, "未登录或登录已过期"),
    NO_PERMISSION(2002, "无权限访问"),
    LOGIN_ERROR(2003, "用户名或密码错误"),

    /** 3xxx 参数校验 */
    PARAM_ERROR(3001, "参数校验失败"),

    /** 4xxx 业务 */
    USER_DISABLED(4001, "账号已被禁用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 2: 写 BusinessException**

```java
package com.tcm.common.exception;

import lombok.Getter;

/**
 * 业务异常：业务层唯一允许抛出的异常，必须携带错误码。
 * 禁止在业务层抛出裸 RuntimeException。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 3: 写 GlobalExceptionHandler**

```java
package com.tcm.common.exception;

import com.tcm.common.base.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器：所有异常统一出口，保证响应结构永远是 Result JSON。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数校验失败" : fieldError.getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR, message);
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数校验失败" : fieldError.getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR, message);
    }

    /** 方法参数（@RequestParam/@PathVariable）校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR, e.getMessage());
    }

    /** 请求体不可读（JSON 格式错误等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_ERROR, "请求体格式错误");
    }

    /** 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return Result.fail(404, "资源不存在");
    }

    /** 兜底：任何未预期异常，必须记录完整堆栈 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
```

- [ ] **Step 4: 写 ErrorCodeTest**

```java
package com.tcm.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorCodeTest {

    @Test
    void errorCodes_shouldBeUniqueAndMessageNotEmpty() {
        long distinctCodes = Arrays.stream(ErrorCode.values()).map(ErrorCode::getCode).distinct().count();
        assertEquals(ErrorCode.values().length, distinctCodes, "错误码必须全局唯一");
        for (ErrorCode errorCode : ErrorCode.values()) {
            assertEquals(false, errorCode.getMessage() == null || errorCode.getMessage().isBlank(), "错误码信息不能为空");
        }
    }
}
```

- [ ] **Step 5: 写 GlobalExceptionHandlerTest（standalone MockMvc，无需 Spring 容器）**

```java
package com.tcm.common.exception;

import com.tcm.common.base.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/test")
    static class TestController {
        @GetMapping("/business")
        public Result<String> business() {
            throw new BusinessException(ErrorCode.LOGIN_ERROR, "密码错误");
        }

        @GetMapping("/system")
        public Result<String> system() {
            throw new IllegalStateException("boom");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void businessException_shouldReturnUnifiedResult() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.msg").value("密码错误"));
    }

    @Test
    void unknownException_shouldReturnSystemError() throws Exception {
        mockMvc.perform(get("/test/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }

    @Test
    void badJsonBody_shouldReturnParamError() throws Exception {
        mockMvc.perform(get("/test/business").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(jsonPath("$.code").value(3001));
    }
}
```

- [ ] **Step 6: 运行测试**

Run: `mvn -q -pl tcm-common test`
Expected: BUILD SUCCESS（ErrorCodeTest + GlobalExceptionHandlerTest 全过）

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "feat(common): 企业级异常体系（错误码分段/业务异常/全局处理器）"
```

---

### Task 4: tcm-common — SnowflakeIdUtil 与敏感信息脱敏工具

**Files:**
- Create: `tcm-common/src/main/java/com/tcm/common/utils/SnowflakeIdUtil.java`
- Create: `tcm-common/src/main/java/com/tcm/common/utils/SensitiveDataUtil.java`
- Test: `tcm-common/src/test/java/com/tcm/common/utils/SnowflakeIdUtilTest.java`
- Test: `tcm-common/src/test/java/com/tcm/common/utils/SensitiveDataUtilTest.java`

**Interfaces:**
- Consumes: 无
- Produces: `SnowflakeIdUtil.nextId(): long`（线程安全，供手动生成主键）；`SensitiveDataUtil.maskJson(String json): String`（递归脱敏 password/idCard/phone 等键）、`SensitiveDataUtil.maskValue(String key, Object value): String`

- [ ] **Step 1: 写 SnowflakeIdUtil**

```java
package com.tcm.common.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

/**
 * 雪花 ID 生成器：全局唯一主键，应用层生成（workerId=1, datacenterId=1）。
 * 达梦/MySQL 双兼容；后期分库分表无主键冲突。线程安全。
 */
public final class SnowflakeIdUtil {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private SnowflakeIdUtil() {
    }

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }
}
```

- [ ] **Step 2: 写 SensitiveDataUtil**

```java
package com.tcm.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 敏感信息脱敏工具：审计日志、日志打印前必须脱敏。
 * 键名命中 SENSITIVE_KEYS 的值直接替换为掩码；嵌套 JSON 递归处理。
 */
public final class SensitiveDataUtil {

    private static final Set<String> SENSITIVE_KEYS = new HashSet<>() {{
        add("password");
        add("oldPassword");
        add("newPassword");
        add("idCard");
        add("phone");
        add("mobile");
        add("token");
        add("bankCard");
        add("certNo");
    }};

    private static final String MASK = "******";

    private SensitiveDataUtil() {
    }

    /** 对 JSON 字符串递归脱敏；非 JSON 输入原样返回。 */
    public static String maskJson(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        try {
            Object parsed = JSONUtil.parse(json);
            return maskValue("", parsed);
        } catch (Exception e) {
            return json;
        }
    }

    /** 递归脱敏：命中敏感键名返回掩码；对象/数组递归处理；普通值原样返回。 */
    public static String maskValue(String key, Object value) {
        if (SENSITIVE_KEYS.contains(key)) {
            return MASK;
        }
        if (value instanceof JSONObject obj) {
            JSONObject masked = new JSONObject();
            for (String k : obj.keySet()) {
                masked.set(k, maskValue(k, obj.get(k)));
            }
            return masked.toString();
        }
        if (value instanceof JSONArray arr) {
            JSONArray masked = new JSONArray();
            arr.forEach(item -> masked.add(maskValue("", item)));
            return masked.toString();
        }
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
```

- [ ] **Step 3: 写 SnowflakeIdUtilTest**

```java
package com.tcm.common.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeIdUtilTest {

    @Test
    void nextId_shouldBeUniqueAndPositive() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            long id = SnowflakeIdUtil.nextId();
            assertTrue(id > 0, "雪花 ID 必须为正数");
            ids.add(id);
        }
        assertTrue(ids.size() == 100_000, "10 万个 ID 必须无重复");
    }
}
```

- [ ] **Step 4: 写 SensitiveDataUtilTest**

```java
package com.tcm.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataUtilTest {

    @Test
    void maskJson_shouldMaskSensitiveKeysRecursively() {
        String json = "{\"username\":\"admin\",\"password\":\"123456\",\"profile\":{\"phone\":\"13800138000\"},\"list\":[{\"token\":\"abc\"}]}";
        String masked = SensitiveDataUtil.maskJson(json);
        assertFalse(masked.contains("123456"), "password 必须被脱敏");
        assertFalse(masked.contains("13800138000"), "嵌套 phone 必须被脱敏");
        assertFalse(masked.contains("\"abc\""), "嵌套 token 必须被脱敏");
        assertTrue(masked.contains("\"admin\""), "非敏感字段保留");
        assertTrue(masked.contains("******"));
    }

    @Test
    void maskJson_shouldReturnInputWhenNotJson() {
        assertEquals("plain text", SensitiveDataUtil.maskJson("plain text"));
        assertNull(SensitiveDataUtil.maskJson(null));
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `mvn -q -pl tcm-common test`
Expected: BUILD SUCCESS

- [ ] **Step 6: 提交**

```bash
git add -A && git commit -m "feat(common): 雪花ID生成器与敏感信息脱敏工具"
```

---

### Task 5: tcm-common — 审计日志组件（@AuditLog + 切面 + 异步落库）

**Files:**
- Create: `tcm-common/src/main/java/com/tcm/common/audit/AuditLog.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/AuditLogRecord.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/AuditLogRecordFactory.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/AuditLogAspect.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/AuditLogWriter.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/OperatorProvider.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/entity/AuditLogEntity.java`
- Create: `tcm-common/src/main/java/com/tcm/common/audit/mapper/AuditLogMapper.java`
- Test: `tcm-common/src/test/java/com/tcm/common/audit/AuditLogRecordFactoryTest.java`

**Interfaces:**
- Consumes: `BaseEntity`、`SensitiveDataUtil`（Task 2/4）
- Produces: `@AuditLog(module, action, description, logResponse)`；`OperatorProvider` 接口（主工程注入实现，从 Sa-Token 取操作人）；`AuditLogWriter`（@Async 异步写库，失败仅记日志）；`AuditLogMapper`（插入 tcm_audit_log）

**设计要点：**
- common 不依赖 Sa-Token：通过 `OperatorProvider` 接口解耦，主工程提供实现。
- 修改前后快照：方法第一个参数实现 `AuditLogRecord.SnapshotProvider`（提供 before/after 摘要）时记录；否则仅记录请求参数（脱敏）与响应摘要。
- 参数脱敏：`SensitiveDataUtil.maskJson`；跳过 ServletRequest/Response/MultipartFile/HttpSession/BindingResult。
- 写库失败不阻断业务：`AuditLogWriter` 内部 try/catch + log.error。

- [ ] **Step 1: 写 @AuditLog 注解**

```java
package com.tcm.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解：标注在需要审计的 Controller/Service 方法上。
 * 核心操作（报价、订单、资质修改、库存调整、审核等）必须标注。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 业务模块，如 user / order / inventory */
    String module();

    /** 动作标识，如 createOrder / auditQualification */
    String action();

    /** 描述，如 "创建订单" */
    String description() default "";

    /** 是否记录响应摘要，默认记录 */
    boolean logResponse() default true;
}
```

- [ ] **Step 2: 写 AuditLogRecord（快照提供者接口 + 记录体）**

```java
package com.tcm.common.audit;

import lombok.Builder;
import lombok.Getter;

/**
 * 审计记录体：由切面构建，异步写入 tcm_audit_log。
 */
@Getter
@Builder
public class AuditLogRecord {

    private String module;
    private String action;
    private String description;
    private Long operatorId;
    private String operatorName;
    private String requestIp;
    private String paramsSnapshot;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String responseSnapshot;
    private long costTime;
    private boolean success;

    /**
     * 快照提供者：方法首个参数实现此接口时，切面记录修改前后快照。
     * 如 DTO 携带旧值/新值，实现两个方法返回 JSON 摘要即可。
     */
    public interface SnapshotProvider {
        /** 执行前的状态摘要 */
        String provideBeforeSnapshot();

        /** 执行后的状态摘要 */
        String provideAfterSnapshot();
    }
}
```

- [ ] **Step 3: 写 AuditLogRecordFactory（纯逻辑，可单测）**

```java
package com.tcm.common.audit;

import cn.hutool.json.JSONUtil;
import com.tcm.common.utils.SensitiveDataUtil;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 审计记录构建工厂：纯函数式，不依赖 Spring，便于单元测试。
 */
public final class AuditLogRecordFactory {

    private static final int MAX_SNAPSHOT_LENGTH = 2048;

    private AuditLogRecordFactory() {
    }

    public static AuditLogRecord build(AuditLog annotation, Object[] args, Long operatorId,
                                       String operatorName, String requestIp,
                                       Object result, long costTime, boolean success) {
        String params = serializeArgs(args);
        String before = null;
        String after = null;
        if (args != null && args.length > 0 && args[0] instanceof AuditLogRecord.SnapshotProvider provider) {
            before = truncate(provider.provideBeforeSnapshot());
            after = truncate(provider.provideAfterSnapshot());
        }
        String response = annotation.logResponse() ? truncate(toJson(result)) : null;
        return AuditLogRecord.builder()
                .module(annotation.module())
                .action(annotation.action())
                .description(annotation.description())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .requestIp(requestIp)
                .paramsSnapshot(truncate(params))
                .beforeSnapshot(before)
                .afterSnapshot(after)
                .responseSnapshot(response)
                .costTime(costTime)
                .success(success)
                .build();
    }

    /** 序列化方法参数：跳过 Servlet/文件等不可序列化对象，脱敏敏感字段。 */
    private static String serializeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> list = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof MultipartFile || arg instanceof jakarta.servlet.ServletRequest
                    || arg instanceof jakarta.servlet.ServletResponse
                    || arg instanceof jakarta.servlet.http.HttpSession) {
                continue;
            }
            list.add(arg);
        }
        if (list.isEmpty()) {
            return null;
        }
        return SensitiveDataUtil.maskJson(toJson(list));
    }

    private static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_SNAPSHOT_LENGTH ? text : text.substring(0, MAX_SNAPSHOT_LENGTH);
    }
}
```

- [ ] **Step 4: 写 OperatorProvider 接口**

```java
package com.tcm.common.audit;

/**
 * 当前操作人提供者：由主工程实现（基于 Sa-Token），common 不直接依赖鉴权框架。
 * 未登录场景返回 null。
 */
public interface OperatorProvider {

    Long currentOperatorId();

    String currentOperatorName();
}
```

- [ ] **Step 5: 写 AuditLogAspect**

```java
package com.tcm.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 审计切面：@AuditLog 方法环绕执行，构建记录后异步落库。
 * 审计失败仅记日志，绝不阻断业务。
 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    private final AuditLogWriter auditLogWriter;
    private final List<OperatorProvider> operatorProviders;

    public AuditLogAspect(AuditLogWriter auditLogWriter,
                          @Autowired(required = false) List<OperatorProvider> operatorProviders) {
        this.auditLogWriter = auditLogWriter;
        this.operatorProviders = operatorProviders;
    }

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            throw t;
        } finally {
            try {
                long cost = System.currentTimeMillis() - start;
                AuditLogRecord record = AuditLogRecordFactory.build(auditLog, joinPoint.getArgs(),
                        resolveOperatorId(), resolveOperatorName(), resolveRequestIp(),
                        result, cost, success);
                auditLogWriter.write(record);
            } catch (Exception e) {
                log.error("审计日志写入失败（不阻断业务）: module={}, action={}", auditLog.module(), auditLog.action(), e);
            }
        }
    }

    private Long resolveOperatorId() {
        if (operatorProviders != null && !operatorProviders.isEmpty()) {
            return operatorProviders.get(0).currentOperatorId();
        }
        return null;
    }

    private String resolveOperatorName() {
        if (operatorProviders != null && !operatorProviders.isEmpty()) {
            return operatorProviders.get(0).currentOperatorName();
        }
        return null;
    }

    private String resolveRequestIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 6: 写 AuditLogWriter（异步落库）**

```java
package com.tcm.common.audit;

import com.tcm.common.audit.entity.AuditLogEntity;
import com.tcm.common.audit.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步写入器：独立线程池执行，失败仅记日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogWriter {

    private final AuditLogMapper auditLogMapper;

    @Async("auditExecutor")
    public void write(AuditLogRecord record) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setModule(record.getModule());
            entity.setAction(record.getAction());
            entity.setDescription(record.getDescription());
            entity.setOperatorId(record.getOperatorId());
            entity.setOperatorName(record.getOperatorName());
            entity.setRequestIp(record.getRequestIp());
            entity.setParamsSnapshot(record.getParamsSnapshot());
            entity.setBeforeSnapshot(record.getBeforeSnapshot());
            entity.setAfterSnapshot(record.getAfterSnapshot());
            entity.setResponseSnapshot(record.getResponseSnapshot());
            entity.setCostTime(record.getCostTime());
            entity.setSuccess(record.isSuccess());
            auditLogMapper.insert(entity);
        } catch (Exception e) {
            log.error("审计日志落库失败（不阻断业务）: {}", record, e);
        }
    }
}
```

- [ ] **Step 7: 写 AuditLogEntity 与 AuditLogMapper**

```java
package com.tcm.common.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tcm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 审计日志表：全平台核心操作留痕，禁止物理删除。
 */
@Getter
@Setter
@TableName("tcm_audit_log")
public class AuditLogEntity extends BaseEntity {

    private String module;
    private String action;
    private String description;
    private Long operatorId;
    private String operatorName;
    private String requestIp;
    private String paramsSnapshot;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String responseSnapshot;
    private Long costTime;
    private Boolean success;
}
```

```java
package com.tcm.common.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.common.audit.entity.AuditLogEntity;

public interface AuditLogMapper extends BaseMapper<AuditLogEntity> {
}
```

- [ ] **Step 8: 写 AuditLogRecordFactoryTest**

```java
package com.tcm.common.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogRecordFactoryTest {

    @Test
    void build_shouldMaskPasswordAndFillFields() {
        Object[] args = new Object[]{"{\"username\":\"admin\",\"password\":\"secret123\"}"};
        AuditLogRecord record = AuditLogRecordFactory.build(
                new TestAuditLog(), args, 1001L, "admin", "127.0.0.1",
                "{\"code\":200}", 15L, true);

        assertEquals("user", record.getModule());
        assertEquals("login", record.getAction());
        assertEquals(1001L, record.getOperatorId());
        assertEquals("admin", record.getOperatorName());
        assertEquals("127.0.0.1", record.getRequestIp());
        assertFalse(record.getParamsSnapshot().contains("secret123"), "参数必须脱敏");
        assertEquals(15L, record.getCostTime());
        assertTrue(record.isSuccess());
    }

    @Test
    void build_shouldCaptureSnapshotWhenFirstArgImplementsProvider() {
        SnapshotDto dto = new SnapshotDto();
        dto.setOldStatus("PENDING");
        dto.setNewStatus("APPROVED");
        AuditLogRecord record = AuditLogRecordFactory.build(
                new TestAuditLog(), new Object[]{dto}, 1L, "u", null, null, 1L, true);

        assertEquals("PENDING", record.getBeforeSnapshot());
        assertEquals("APPROVED", record.getAfterSnapshot());
    }

    static class TestAuditLog implements AuditLog {
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return AuditLog.class;
        }

        @Override
        public String module() {
            return "user";
        }

        @Override
        public String action() {
            return "login";
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public boolean logResponse() {
            return true;
        }
    }

    static class SnapshotDto implements AuditLogRecord.SnapshotProvider {
        private String oldStatus;
        private String newStatus;

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        @Override
        public String provideBeforeSnapshot() {
            return oldStatus;
        }

        @Override
        public String provideAfterSnapshot() {
            return newStatus;
        }
    }
}
```

- [ ] **Step 9: 运行测试**

Run: `mvn -q -pl tcm-common test`
Expected: BUILD SUCCESS

- [ ] **Step 10: 提交**

```bash
git add -A && git commit -m "feat(common): 审计日志组件（注解/切面/异步落库/脱敏）"
```

---

### Task 6: tcm-common — RedisLockUtil 分布式锁（条件装配）

**Files:**
- Create: `tcm-common/src/main/java/com/tcm/common/lock/RedisLockUtil.java`
- Create: `tcm-common/src/main/java/com/tcm/common/lock/RedisLockConfig.java`

**Interfaces:**
- Consumes: `BusinessException`、`ErrorCode`（Task 3）
- Produces: `RedisLockUtil`（tryLock/unlock/executeWithLock 三组 API）；`RedisLockConfig`（`tcm.infrastructure.redis.enabled=true` 时装配 RedissonClient + RedisLockUtil，默认不装配）

- [ ] **Step 1: 写 RedisLockUtil**

```java
package com.tcm.common.lock;

import com.tcm.common.exception.BusinessException;
import com.tcm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redisson 的分布式锁工具。
 * 约定：锁粒度必须小，锁业务唯一标识（如订单号/批次号），禁止锁表锁模块。
 */
@Slf4j
public class RedisLockUtil {

    private static final String LOCK_KEY_PREFIX = "tcm:lock:";

    private final RedissonClient redissonClient;

    public RedisLockUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private RLock getLock(String key) {
        return redissonClient.getLock(LOCK_KEY_PREFIX + key);
    }

    /** 尝试加锁，waitMillis 内拿不到返回 false。 */
    public boolean tryLock(String key, long waitMillis, long leaseMillis) {
        RLock lock = getLock(key);
        try {
            return lock.tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 解锁；非持锁线程调用仅记日志不抛异常。 */
    public void unlock(String key) {
        RLock lock = getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        } else {
            log.warn("释放锁失败：当前线程未持有该锁 key={}", key);
        }
    }

    /** 回调式加锁执行：加锁失败抛业务异常。 */
    public <T> T executeWithLock(String key, long waitMillis, long leaseMillis, Supplier<T> action) {
        if (!tryLock(key, waitMillis, leaseMillis)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取分布式锁失败: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(key);
        }
    }

    /** 回调式加锁执行（无返回值）。 */
    public void executeWithLock(String key, long waitMillis, long leaseMillis, Runnable action) {
        executeWithLock(key, waitMillis, leaseMillis, () -> {
            action.run();
            return null;
        });
    }
}
```

- [ ] **Step 2: 写 RedisLockConfig（条件装配）**

```java
package com.tcm.common.lock;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redis 分布式锁配置：tcm.infrastructure.redis.enabled=true 时启用。
 * 默认关闭，避免本地未装 Redis 时启动失败。手动创建 RedissonClient，不依赖 starter 自动装配。
 */
@Configuration
@ConditionalOnProperty(prefix = "tcm.infrastructure", name = "redis.enabled", havingValue = "true")
public class RedisLockConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16);
        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    public RedisLockUtil redisLockUtil(RedissonClient redissonClient) {
        return new RedisLockUtil(redissonClient);
    }
}
```

- [ ] **Step 3: 编译验证并提交**

Run: `mvn -q -pl tcm-common compile`
Expected: BUILD SUCCESS

```bash
git add -A && git commit -m "feat(common): 分布式锁 RedisLockUtil（条件装配，默认关闭）"
```

---

### Task 7: tcm-common — MQ 工具（MqProducer / MqConsumer 基类）

**Files:**
- Create: `tcm-common/src/main/java/com/tcm/common/mq/MqProducer.java`
- Create: `tcm-common/src/main/java/com/tcm/common/mq/AbstractMqConsumer.java`

**Interfaces:**
- Consumes: `BusinessException`、`ErrorCode`（Task 3）
- Produces: `MqProducer.send(topic, tag, payload)`（RocketMQTemplate 未装配时降级告警日志）；`AbstractMqConsumer` 抽象基类（子类实现 `dedupKey(payload)` 与 `doConsume(payload)`；`tryConsume(payload, dedupSeconds)` 内建 Redis 幂等，业务异常则删除幂等标记交由 MQ 重试）

- [ ] **Step 1: 写 MqProducer**

```java
package com.tcm.common.mq;

import com.tcm.common.exception.BusinessException;
import com.tcm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 统一消息生产者。
 * 约定：所有异步解耦、跨系统通知、耗时操作必须走 MQ，禁止同步调用嵌套。
 * RocketMQTemplate 未配置（rocketmq.name-server 缺失）时降级为告警日志。
 */
@Slf4j
@Component
public class MqProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public MqProducer(@Autowired(required = false) RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /** 发送消息：topic:tag 形式，payload 会被 JSON 序列化。 */
    public void send(String topic, String tag, Object payload) {
        if (rocketMQTemplate == null) {
            log.warn("RocketMQ 未启用（未配置 rocketmq.name-server），消息丢弃: topic={}, tag={}", topic, tag);
            return;
        }
        try {
            rocketMQTemplate.convertAndSend(topic + ":" + tag, payload);
            log.info("MQ 发送成功: topic={}, tag={}", topic, tag);
        } catch (Exception e) {
            log.error("MQ 发送失败: topic={}, tag={}", topic, tag, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息发送失败: " + topic + ":" + tag);
        }
    }
}
```

- [ ] **Step 2: 写 AbstractMqConsumer**

```java
package com.tcm.common.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * MQ 消费者基类。
 * 子类实现 dedupKey（业务唯一 ID）与 doConsume（抛异常表示消费失败）。
 * 幂等：Redis SETNX 去重；业务失败时删除幂等标记，交给 MQ 重试；超过重试上限自动进入 %DLQ% 死信队列。
 */
@Slf4j
public abstract class AbstractMqConsumer {

    private static final String DEDUP_KEY_PREFIX = "tcm:mq:dedup:";

    private final StringRedisTemplate stringRedisTemplate;

    protected AbstractMqConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 业务唯一 ID（如订单号、工单号），同一消息幂等去重依据。 */
    protected abstract String dedupKey(Object payload);

    /** 消费逻辑，抛异常表示消费失败（触发 MQ 重试）。 */
    protected abstract void doConsume(Object payload);

    /**
     * 消费入口：幂等拦截 + 业务处理。
     *
     * @return true=消费成功或重复消息；false=业务失败（等待 MQ 重试）
     */
    protected boolean tryConsume(Object payload, long dedupSeconds) {
        String dedupKey = DEDUP_KEY_PREFIX + dedupKey(payload);
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofSeconds(dedupSeconds));
        if (Boolean.FALSE.equals(first)) {
            log.info("幂等拦截重复消息: {}", dedupKey);
            return true;
        }
        try {
            doConsume(payload);
            return true;
        } catch (Exception e) {
            log.error("消息消费失败，删除幂等标记等待重试: {}", dedupKey, e);
            stringRedisTemplate.delete(dedupKey);
            return false;
        }
    }
}
```

- [ ] **Step 3: 编译验证并提交**

Run: `mvn -q -pl tcm-common compile`
Expected: BUILD SUCCESS

```bash
git add -A && git commit -m "feat(common): MQ 统一生产者与消费者基类（幂等/重试约定）"
```

---

### Task 8: 主工程 — 启动类、DM8 数据源、MyBatis-Plus、全局配置

**Files:**
- Create: `tcm-trade-platform/src/main/java/com/tcm/TradePlatformApplication.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/MybatisPlusConfig.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/MyMetaObjectHandler.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/AsyncConfig.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/CorsConfig.java`
- Create: `tcm-trade-platform/src/main/resources/application.yml`
- Create: `tcm-trade-platform/src/main/resources/application-dev.yml`
- Create: `tcm-trade-platform/src/main/resources/application-prod.yml`

**Interfaces:**
- Consumes: tcm-common（BaseEntity 自动填充依赖 MetaObjectHandler）
- Produces: 可启动的 Spring Boot 应用（`mvn spring-boot:run`）；`MybatisPlusInterceptor`（分页 `DbType.DM`）；`auditExecutor` 线程池（审计异步用）；CORS 放行 dev 前端

- [ ] **Step 1: 写启动类**

```java
package com.tcm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TCM 平台单体主工程启动类。
 * 领域分包：com.tcm.common 公共层 / com.tcm.integration 适配层 / com.tcm.{user,goods,...} 业务域。
 */
@SpringBootApplication
public class TradePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradePlatformApplication.class, args);
    }
}
```

- [ ] **Step 2: 写 MybatisPlusConfig（分页 + Mapper 扫描）**

```java
package com.tcm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页插件统一适配达梦 DM 方言。
 * 禁止手写原生分页 SQL；禁止使用达梦 rownum 分页。
 */
@Configuration
@MapperScan("com.tcm")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.DM));
        return interceptor;
    }
}
```

- [ ] **Step 3: 写 MyMetaObjectHandler（自动填充）**

```java
package com.tcm.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：create_time/update_time/create_by。
 * 未登录场景 create_by 填充 -1。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, currentOperatorId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    private Long currentOperatorId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return -1L;
        }
    }
}
```

- [ ] **Step 4: 写 AsyncConfig（审计异步线程池）**

```java
package com.tcm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置：审计日志使用独立线程池（手动创建，禁止 Executors）。
 * 队列满时丢弃并记录错误日志——审计失败绝不阻断业务。
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("audit-thread-");
        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor pool) ->
                log.error("审计线程池已满，丢弃审计任务"));
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 5: 写 CorsConfig**

```java
package com.tcm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置：放行开发环境前端（PC 基座 5173、H5 基座 5174）。
 * 生产环境建议同域部署或经网关收敛。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:5174")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

- [ ] **Step 6: 写 application.yml / dev / prod**

`application.yml`：

```yaml
spring:
  application:
    name: tcm-trade-platform
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

server:
  port: 8080

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  global-config:
    banner: false
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true

sa-token:
  token-name: Authorization
  timeout: 86400
  active-timeout: -1
  is-concurrent: true
  is-share: false
  token-style: uuid
  is-log: false

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

# 中间件开关：默认全部关闭，仅依赖达梦即可启动；启用后按需填写下方配置
tcm:
  infrastructure:
    redis:
      enabled: false
    mq:
      enabled: false
    minio:
      enabled: false
    job:
      enabled: false

logging:
  level:
    com.tcm: debug
```

`application-dev.yml`：

```yaml
spring:
  datasource:
    driver-class-name: dm.jdbc.driver.DmDriver
    url: jdbc:dm://localhost:5236
    username: SYSDBA
    password: SYSDBA
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

# 达梦连接池
spring.datasource.hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 30000
```

`application-prod.yml`（占位 + 说明）：

```yaml
# 生产环境配置模板：
# 1. 数据库、Redis、MQ、MinIO 等凭据一律走环境变量/配置中心，禁止提交真实密码
# 2. 达梦 URL 示例：jdbc:dm://<host>:<port>
# 3. 启用中间件时置 tcm.infrastructure.*.enabled=true
spring:
  datasource:
    driver-class-name: dm.jdbc.driver.DmDriver
    url: ${TCM_DB_URL}
    username: ${TCM_DB_USERNAME}
    password: ${TCM_DB_PASSWORD}
```

- [ ] **Step 7: 编译验证**

Run: `mvn -q -pl tcm-trade-platform -am compile`
Expected: BUILD SUCCESS

- [ ] **Step 8: 提交**

```bash
git add -A && git commit -m "feat(platform): 启动类/数据源/MyBatis-Plus/异步与跨域配置"
```

---

### Task 9: 主工程 — Sa-Token 登录鉴权 demo（user 域）

**Files:**
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/SaTokenConfig.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/SaTokenExceptionHandler.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/SaOperatorProvider.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/entity/SysUser.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/mapper/SysUserMapper.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/service/SysUserService.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/service/impl/SysUserServiceImpl.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/controller/AuthController.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/dto/req/LoginReq.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/dto/resp/LoginResp.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/dto/resp/UserInfoResp.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/user/enums/UserStatusEnum.java`
- Test: `tcm-trade-platform/src/test/java/com/tcm/user/PasswordHashGenTest.java`（临时，输出 BCrypt 哈希后删除）

**Interfaces:**
- Consumes: `Result`、`BusinessException`、`ErrorCode`、`BaseEntity`、`AuditLog`、`OperatorProvider`（common）
- Produces: 登录链路 `POST /api/auth/login` → `{token, tokenName, userId, username, realName}`；`GET /api/auth/info`（@SaCheckLogin）；`GET /api/auth/need-permission`（@SaCheckPermission("demo:view")）；`OperatorProvider` 实现（审计取操作人）；Sa-Token 异常统一返回 2001/2002

- [ ] **Step 1: 写 SaTokenConfig（拦截 + 白名单）**

```java
package com.tcm.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器：/api/** 全部要求登录，白名单仅限登录接口与文档。
 * 新增白名单接口必须评审备案。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}
```

- [ ] **Step 2: 写 SaTokenExceptionHandler**

```java
package com.tcm.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.tcm.common.base.Result;
import com.tcm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 鉴权异常处理：未登录 2001、无权限 2002。
 * 异常类型比 common 的兜底处理器更具体，Spring 自动路由到此处。
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLogin(NotLoginException e) {
        log.warn("未登录访问: type={}", e.getType());
        return Result.fail(ErrorCode.NOT_LOGIN);
    }

    @ExceptionHandler({NotPermissionException.class, NotRoleException.class})
    public Result<Void> handleNoPermission(Exception e) {
        log.warn("无权限访问: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_PERMISSION);
    }
}
```

- [ ] **Step 3: 写 SaOperatorProvider（审计取操作人）**

```java
package com.tcm.config;

import cn.dev33.satoken.stp.StpUtil;
import com.tcm.common.audit.OperatorProvider;
import org.springframework.stereotype.Component;

/**
 * 审计操作人提供者：基于 Sa-Token 会话。
 */
@Component
public class SaOperatorProvider implements OperatorProvider {

    @Override
    public Long currentOperatorId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String currentOperatorName() {
        try {
            return String.valueOf(StpUtil.getLoginIdDefaultNull());
        } catch (Exception e) {
            return null;
        }
    }
}
```

- [ ] **Step 4: 写 SysUser 实体与 Mapper**

```java
package com.tcm.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.tcm.common.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 平台账号（6 端共用，通过角色区分终端权限）。
 */
@Getter
@Setter
@TableName("tcm_user")
public class SysUser extends BaseEntity {

    /** 登录账号 */
    private String username;

    /** BCrypt 哈希，禁止明文 */
    private String password;

    /** 真实姓名/昵称 */
    private String realName;

    /** 角色编码：admin/seller/supplier/buyer_b/buyer_c/pharmacist */
    private String roleCode;

    /** 状态：1 启用 / 0 禁用 */
    private Integer status;
}
```

```java
package com.tcm.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tcm.user.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

- [ ] **Step 5: 写 UserStatusEnum 与 Service**

```java
package com.tcm.user.enums;

import lombok.Getter;

/**
 * 账号状态。
 */
@Getter
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final int code;
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

```java
package com.tcm.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tcm.user.dto.req.LoginReq;
import com.tcm.user.dto.resp.LoginResp;
import com.tcm.user.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    /** 登录：校验账号密码，签发 Sa-Token，返回 token 信息 */
    LoginResp login(LoginReq req);

    /** 按用户名查询（未删除） */
    SysUser getByUsername(String username);
}
```

```java
package com.tcm.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tcm.common.exception.BusinessException;
import com.tcm.common.exception.ErrorCode;
import com.tcm.user.dto.req.LoginReq;
import com.tcm.user.dto.resp.LoginResp;
import com.tcm.user.entity.SysUser;
import com.tcm.user.enums.UserStatusEnum;
import com.tcm.user.mapper.SysUserMapper;
import com.tcm.user.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResp login(LoginReq req) {
        SysUser user = getByUsername(req.getUsername());
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            // 统一提示，不泄露账号是否存在
            throw new BusinessException(ErrorCode.LOGIN_ERROR);
        }
        if (UserStatusEnum.DISABLED.getCode() == user.getStatus()) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        StpUtil.login(user.getId());
        LoginResp resp = new LoginResp();
        resp.setToken(StpUtil.getTokenValue());
        resp.setTokenName(StpUtil.getTokenName());
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        return resp;
    }

    @Override
    public SysUser getByUsername(String username) {
        return getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    }
}
```

- [ ] **Step 6: 写 DTO 与 AuthController**

```java
package com.tcm.user.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginReq {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    private String password;
}
```

```java
package com.tcm.user.dto.resp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResp {

    private String token;
    private String tokenName;
    private Long userId;
    private String username;
    private String realName;
}
```

```java
package com.tcm.user.dto.resp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoResp {

    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}
```

```java
package com.tcm.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.tcm.common.audit.AuditLog;
import com.tcm.common.base.Result;
import com.tcm.user.dto.req.LoginReq;
import com.tcm.user.dto.resp.LoginResp;
import com.tcm.user.dto.resp.UserInfoResp;
import com.tcm.user.entity.SysUser;
import com.tcm.user.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService sysUserService;

    public AuthController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Operation(summary = "登录")
    @AuditLog(module = "user", action = "login", description = "账号登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(sysUserService.login(req));
    }

    @Operation(summary = "退出登录")
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }

    @Operation(summary = "当前用户信息")
    @SaCheckLogin
    @GetMapping("/info")
    public Result<UserInfoResp> info() {
        SysUser user = sysUserService.getById(StpUtil.getLoginIdAsLong());
        UserInfoResp resp = new UserInfoResp();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRoleCode(user.getRoleCode());
        return Result.ok(resp);
    }

    @Operation(summary = "权限注解 demo（需 demo:view 权限）")
    @SaCheckPermission("demo:view")
    @GetMapping("/need-permission")
    public Result<String> needPermission() {
        return Result.ok("你有 demo:view 权限");
    }
}
```

- [ ] **Step 7: 写 StpInterfaceImpl（角色 → 权限）与 PasswordEncoder Bean**

```java
package com.tcm.config;

import cn.dev33.satoken.stp.StpInterface;
import com.tcm.user.entity.SysUser;
import com.tcm.user.service.SysUserService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 权限数据源：从账号角色映射权限码。
 * 说明：脚手架为最小演示实现；后续 M1 按 6 端角色权限表完善。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private final SysUserService sysUserService;

    public StpInterfaceImpl(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SysUser user = sysUserService.getById(Long.valueOf(loginId.toString()));
        if (user == null) {
            return Collections.emptyList();
        }
        if ("admin".equals(user.getRoleCode())) {
            return List.of("*");
        }
        if ("seller".equals(user.getRoleCode())) {
            return List.of("demo:view");
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = sysUserService.getById(Long.valueOf(loginId.toString()));
        return user == null ? Collections.emptyList() : List.of(user.getRoleCode());
    }
}
```

`SecurityConfig`（新增文件 `tcm-trade-platform/src/main/java/com/tcm/config/SecurityConfig.java`）：

```java
package com.tcm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密：仅使用 spring-security-crypto 的 BCrypt，不引入完整 Spring Security。
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 8: 生成种子密码 BCrypt 哈希（临时测试，输出后删除）**

写 `PasswordHashGenTest`：

```java
package com.tcm.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordHashGenTest {

    @Test
    void printHash() {
        System.out.println("ADMIN_HASH=" + new BCryptPasswordEncoder().encode("123456"));
        System.out.println("SELLER_HASH=" + new BCryptPasswordEncoder().encode("123456"));
    }
}
```

Run: `mvn -q -pl tcm-trade-platform -am test -Dtest=PasswordHashGenTest`
Expected: 控制台输出两行 `ADMIN_HASH=$2a$10$...`、`SELLER_HASH=$2a$10$...`（BCrypt 每次盐不同，两个哈希值不同，均可用于登录）。**复制两行哈希值供 Task 11 使用，然后删除本测试文件。**

- [ ] **Step 9: 全量编译验证**

Run: `mvn -q -pl tcm-trade-platform -am test`
Expected: BUILD SUCCESS

- [ ] **Step 10: 提交**

```bash
git add -A && git commit -m "feat(platform): Sa-Token 登录鉴权 demo（user 域）"
```

---

### Task 10: 业务域骨架 + integration 适配层 + 中间件配置占位

**Files:**
- Create: `tcm-trade-platform/src/main/java/com/tcm/integration/wms/WmsAdapter.java` + `WmsAdapterMockImpl.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/integration/tms/TmsAdapter.java` + `TmsAdapterMockImpl.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/integration/trace/TraceAdapter.java` + `TraceAdapterMockImpl.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/integration/erp/ErpAdapter.java` + `ErpAdapterMockImpl.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/integration/pay/PayAdapter.java` + `PayAdapterMockImpl.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/integration/regulatory/RegulatoryAdapter.java` + `RegulatoryAdapterMockImpl.java`
- Create: 9 个业务域骨架 `com/tcm/{goods,trade,order,inventory,prescription,settle,trace,regulatory,mall}/README.md`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/MinioConfig.java`
- Create: `tcm-trade-platform/src/main/java/com/tcm/config/XxlJobConfig.java`

**Interfaces:**
- Consumes: 无新依赖
- Produces: 6 个适配器接口（业务层只依赖接口；更换厂商只改 Impl）；中间件条件配置（`tcm.infrastructure.minio.enabled` / `tcm.infrastructure.job.enabled`）

- [ ] **Step 1: 写 WMS 适配器（模板，其余 5 个照此模式）**

```java
package com.tcm.integration.wms;

import java.util.Map;

/**
 * WMS 仓储适配器：订单出库指令下发、出库结果回调。
 * 约束：只做报文转换/协议适配，不写业务逻辑；外部回调必须幂等。
 */
public interface WmsAdapter {

    /** 下发订单出库指令 */
    void pushOutboundOrder(Long orderId, Map<String, Object> outboundInfo);

    /** 接收 WMS 出库回调（实现内必须做幂等校验） */
    void handleOutboundCallback(Map<String, Object> callbackBody);
}
```

```java
package com.tcm.integration.wms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * WMS 适配器 Mock 实现：脚手架阶段仅记录日志，替换厂商时新增实现类即可，业务层零改动。
 */
@Slf4j
@Component
public class WmsAdapterMockImpl implements WmsAdapter {

    @Override
    public void pushOutboundOrder(Long orderId, Map<String, Object> outboundInfo) {
        log.info("[WMS-Mock] 下发出库指令: orderId={}, info={}", orderId, outboundInfo);
    }

    @Override
    public void handleOutboundCallback(Map<String, Object> callbackBody) {
        log.info("[WMS-Mock] 接收出库回调: {}", callbackBody);
        // TODO 实际实现：按回调幂等键（如出库单号）校验后更新订单状态
    }
}
```

**其余 5 个适配器接口签名（实现类同 WmsAdapterMockImpl 模式，日志前缀对应系统名）：**

| 包 | 接口方法 |
|---|---|
| `tms` | `void pushShipment(Long orderId, Map<String,Object> shipmentInfo)`；`void handleTraceCallback(Map<String,Object> callbackBody)` |
| `trace` | `void reportTraceNode(Map<String,Object> nodeData)`；`Map<String,Object> queryTrace(String traceCode)` |
| `erp` | `void syncProduct(Map<String,Object> productData)`；`void syncInventory(Map<String,Object> inventoryData)`；`void pushOrderToErp(Long orderId, Map<String,Object> orderData)` |
| `pay` | `String createPayment(Map<String,Object> paymentReq)`（返回支付单号）；`void handlePayCallback(Map<String,Object> callbackBody)`；`void refund(String paymentNo, Long amount)` |
| `regulatory` | `void reportTaxData(Map<String,Object> taxData)`；`void reportSupervisionData(Map<String,Object> data)` |

- [ ] **Step 2: 写 9 个业务域骨架 README（统一模板，如 goods/README.md）**

```markdown
# goods 业务域 — 商品中心（M2）

## 职责
商品档案统一管理：药材基础库、商品上架审核、批次属性、溯源码绑定。

## 约束
- 对外只暴露 Service 接口给其他业务域，禁止跨域访问 Mapper/Entity
- 所有外部系统调用走 com.tcm.integration 适配层

## 状态
脚手架阶段：空骨架，业务实现见后续迭代计划。
```

（其余 8 个域的 README 按 SPEC §1 中各模块职责描述填写"职责"段，其余相同。）

- [ ] **Step 3: 写 MinioConfig 与 XxlJobConfig（条件装配）**

```java
package com.tcm.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端：tcm.infrastructure.minio.enabled=true 时启用。
 * 约束：敏感文件服务端加密，禁止公网直链访问。
 */
@Configuration
@ConditionalOnProperty(prefix = "tcm.infrastructure", name = "minio.enabled", havingValue = "true")
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:}")
    private String accessKey;

    @Value("${minio.secret-key:}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

```java
package com.tcm.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器：tcm.infrastructure.job.enabled=true 时启用。
 * 约束：禁止使用 Spring Schedule 本地定时任务；所有定时任务统一走 XXL-Job。
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "tcm.infrastructure", name = "job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses:http://localhost:8080/xxl-job-admin}")
    private String adminAddresses;

    @Value("${xxl.job.access-token:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname:tcm-trade-platform}")
    private String appName;

    @Value("${xxl.job.executor.port:9999}")
    private int executorPort;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info("XXL-Job 执行器初始化: admin={}, appname={}", adminAddresses, appName);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appName);
        executor.setAddress("");
        executor.setIp("");
        executor.setPort(executorPort);
        executor.setAccessToken(accessToken);
        executor.setLogPath("logs/xxl-job");
        executor.setLogRetentionDays(30);
        return executor;
    }
}
```

- [ ] **Step 4: 编译验证并提交**

Run: `mvn -q -pl tcm-trade-platform -am compile`
Expected: BUILD SUCCESS

```bash
git add -A && git commit -m "feat(platform): 适配层接口骨架、业务域占位、MinIO/XXL-Job 条件配置"
```

---

### Task 11: 数据库脚本 sql/init.sql

**Files:**
- Create: `sql/init.sql`

**Interfaces:**
- Consumes: Task 9 Step 8 输出的 `ADMIN_HASH` / `SELLER_HASH`
- Produces: 达梦/MySQL 双兼容建表脚本 + 种子账号

- [ ] **Step 1: 写 sql/init.sql（将 Task 9 Step 8 的哈希值填入占位）**

```sql
-- ============================================================
-- TCM 平台初始化脚本（达梦 DM8 / MySQL 双兼容）
-- 执行方式：
--   达梦: disql tcm/<密码>@localhost:5236 < init.sql
--   MySQL: mysql -u tcm -p tcm < init.sql
-- 约束：禁止达梦独有语法/函数；分页走 MyBatis-Plus 插件。
-- ============================================================

-- 账号表（6 端共用，角色区分终端）
CREATE TABLE tcm_user (
    id          BIGINT       NOT NULL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 哈希',
    real_name   VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    role_code   VARCHAR(32)  NOT NULL DEFAULT 'buyer_c' COMMENT '角色: admin/seller/supplier/buyer_b/buyer_c/pharmacist',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    create_time TIMESTAMP    DEFAULT NULL,
    update_time TIMESTAMP    DEFAULT NULL,
    create_by   BIGINT       DEFAULT NULL,
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除'
);
CREATE UNIQUE INDEX uk_username ON tcm_user (username);
CREATE INDEX idx_user_role ON tcm_user (role_code);

-- 审计日志表（禁止物理删除，保留修改前后快照）
CREATE TABLE tcm_audit_log (
    id                BIGINT       NOT NULL PRIMARY KEY,
    module            VARCHAR(64)  NOT NULL COMMENT '业务模块',
    action            VARCHAR(64)  NOT NULL COMMENT '动作标识',
    description       VARCHAR(255) DEFAULT NULL COMMENT '描述',
    operator_id       BIGINT       DEFAULT NULL COMMENT '操作人ID',
    operator_name     VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    request_ip        VARCHAR(64)  DEFAULT NULL COMMENT '请求IP',
    params_snapshot   TEXT         DEFAULT NULL COMMENT '请求参数快照(脱敏)',
    before_snapshot   TEXT         DEFAULT NULL COMMENT '修改前快照',
    after_snapshot    TEXT         DEFAULT NULL COMMENT '修改后快照',
    response_snapshot TEXT         DEFAULT NULL COMMENT '响应摘要',
    cost_time         BIGINT       DEFAULT NULL COMMENT '耗时ms',
    success           TINYINT      DEFAULT 1 COMMENT '是否成功',
    create_time       TIMESTAMP    DEFAULT NULL,
    update_time       TIMESTAMP    DEFAULT NULL,
    create_by         BIGINT       DEFAULT NULL,
    is_deleted        TINYINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_audit_module_time ON tcm_audit_log (module, create_time);

-- 种子账号：admin(平台管理员) / seller01(示例卖家)，密码均为 123456（BCrypt）
INSERT INTO tcm_user (id, username, password, real_name, role_code, status, create_time, update_time, create_by, is_deleted) VALUES
(1830000000000000001, 'admin',    '<ADMIN_HASH>', '平台管理员', 'admin',  1, NOW(), NOW(), 0, 0),
(1830000000000000002, 'seller01', '<SELLER_HASH>', '示例卖家',   'seller', 1, NOW(), NOW(), 0, 0);
```

- [ ] **Step 2: 提交**

```bash
git add -A && git commit -m "feat: 初始化数据库脚本（tcm_user/tcm_audit_log + 种子账号）"
```

---

### Task 12: 前端基座 tcm-pc-base

**Files:**
- Create: `tcm-pc-base/package.json`
- Create: `tcm-pc-base/vite.config.js`
- Create: `tcm-pc-base/index.html`
- Create: `tcm-pc-base/.env.development`、`.env.production`
- Create: `tcm-pc-base/.eslintrc.cjs`、`.prettierrc`、`.gitignore`
- Create: `tcm-pc-base/jsconfig.json`
- Create: `tcm-pc-base/src/main.js`、`src/App.vue`
- Create: `tcm-pc-base/src/router/index.js`
- Create: `tcm-pc-base/src/stores/user.js`
- Create: `tcm-pc-base/src/utils/request.js`
- Create: `tcm-pc-base/src/layout/index.vue`
- Create: `tcm-pc-base/src/views/login/index.vue`
- Create: `tcm-pc-base/src/views/dashboard/index.vue`

**Interfaces:**
- Consumes: 后端 `POST /api/auth/login`、`POST /api/auth/logout`、`GET /api/auth/info`（Task 9）
- Produces: 可 npm run dev 启动的 PC 管理基座（5173），登录页对接后端 Sa-Token

- [ ] **Step 1: 写 package.json / vite.config.js / index.html / 环境变量**

```json
{
  "name": "tcm-pc-base",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.7.7",
    "element-plus": "^2.8.4",
    "pinia": "^2.2.4",
    "vue": "^3.4.38",
    "vue-router": "^4.4.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.4",
    "eslint": "^8.57.1",
    "eslint-plugin-vue": "^9.28.0",
    "prettier": "^3.3.3",
    "vite": "^5.4.8"
  }
}
```

```js
// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

```html
<!-- index.html -->
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TCM 运营平台基座</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

`.env.development`：`VITE_API_BASE=/api`
`.env.production`：`VITE_API_BASE=/api`

- [ ] **Step 2: 写 utils/request.js（统一请求封装）**

```js
// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = userStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 2001) {
      // 未登录/过期：登出并回登录页
      useUserStore().resetAuth()
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const res = error.response?.data
    if (error.response?.status === 401 || res?.code === 2001) {
      useUserStore().resetAuth()
      router.push('/login')
    } else {
      ElMessage.error(res?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
```

- [ ] **Step 3: 写 stores/user.js 与 router/index.js**

```js
// src/stores/user.js
import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('tcm_token') || '',
    userInfo: JSON.parse(localStorage.getItem('tcm_user_info') || 'null'),
  }),
  actions: {
    async login(form) {
      const res = await request.post('/auth/login', form)
      this.token = res.data.token
      this.userInfo = res.data
      localStorage.setItem('tcm_token', this.token)
      localStorage.setItem('tcm_user_info', JSON.stringify(this.userInfo))
    },
    async fetchInfo() {
      const res = await request.get('/auth/info')
      this.userInfo = res.data
      localStorage.setItem('tcm_user_info', JSON.stringify(this.userInfo))
    },
    logout() {
      try {
        request.post('/auth/logout')
      } catch (e) {
        // 忽略登出接口异常
      }
      this.resetAuth()
    },
    resetAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('tcm_token')
      localStorage.removeItem('tcm_user_info')
    },
  },
})
```

```js
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫：未登录跳登录页；已登录访问登录页跳首页
router.beforeEach((to) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  if (to.path !== '/login' && !isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && isLoggedIn) {
    return '/'
  }
})

export default router
```

- [ ] **Step 4: 写 main.js / App.vue / layout / 登录页 / 首页**

```js
// src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

```vue
<!-- src/App.vue -->
<template>
  <router-view />
</template>
```

```vue
<!-- src/layout/index.vue：侧边菜单 + 顶栏，菜单数据由后端返回（动态渲染预留） -->
<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="layout-logo">TCM 平台</div>
      <el-menu :default-active="$route.path" router background-color="#001529" text-color="#fff" active-text-color="#409eff">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <el-dropdown @command="handleCommand">
          <span class="layout-user">
            {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// TODO: 后续由后端返回菜单/按钮权限，前端动态渲染，禁止硬编码权限
const menus = computed(() => [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
])

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.layout-aside {
  background-color: #001529;
}
.layout-logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.layout-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-bottom: 1px solid #eee;
}
.layout-user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
```

```vue
<!-- src/views/login/index.vue -->
<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">TCM 平台基座</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">种子账号：admin / 123456</p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3a5f 0%, #2d5f8a 100%);
}
.login-card {
  width: 380px;
  padding: 12px 8px;
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #303133;
}
.login-btn {
  width: 100%;
}
.login-tip {
  text-align: center;
  color: #909399;
  font-size: 12px;
}
</style>
```

```vue
<!-- src/views/dashboard/index.vue -->
<template>
  <el-card>
    <h3>欢迎使用 TCM 平台</h3>
    <p>当前用户：{{ userStore.userInfo?.realName }}（{{ userStore.userInfo?.username }}）</p>
    <p>脚手架已完成：Sa-Token 登录鉴权、统一返回、全局异常、审计日志链路已打通。</p>
  </el-card>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>
```

- [ ] **Step 5: 写配套配置文件**

`jsconfig.json`：

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "exclude": ["node_modules", "dist"]
}
```

`.eslintrc.cjs`：

```js
module.exports = {
  root: true,
  env: { browser: true, es2022: true, node: true },
  extends: ['eslint:recommended', 'plugin:vue/vue3-recommended', 'prettier'],
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module' },
  rules: {
    'vue/multi-word-component-names': 'off',
  },
}
```

`.prettierrc`：

```json
{
  "semi": false,
  "singleQuote": true,
  "printWidth": 100,
  "trailingComma": "all"
}
```

`.gitignore`：`node_modules/`、`dist/`、`*.local`、`.DS_Store`

- [ ] **Step 6: 安装依赖并构建验证**

Run（工作目录 `f:/MyProject/ylxm/tcm-pc-base`）：
`npm install`
`npm run build`
Expected: 构建成功，输出 `dist/` 目录（首次 install 需联网，耗时 1-3 分钟）

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "feat(pc-base): Vue3+Element Plus 前端基座（登录/布局/请求封装/路由守卫）"
```

---

### Task 13: 前端基座 tcm-h5-base

**Files:**
- Create: `tcm-h5-base/package.json`
- Create: `tcm-h5-base/vite.config.js`
- Create: `tcm-h5-base/postcss.config.js`
- Create: `tcm-h5-base/index.html`
- Create: `tcm-h5-base/.env.development`、`.env.production`
- Create: `tcm-h5-base/.eslintrc.cjs`、`.prettierrc`、`.gitignore`
- Create: `tcm-h5-base/src/main.js`、`src/App.vue`
- Create: `tcm-h5-base/src/router/index.js`
- Create: `tcm-h5-base/src/stores/user.js`
- Create: `tcm-h5-base/src/utils/request.js`
- Create: `tcm-h5-base/src/views/login/index.vue`
- Create: `tcm-h5-base/src/views/home/index.vue`

**Interfaces:**
- Consumes: 后端登录接口（同 Task 12）
- Produces: 可 npm run dev 启动的 H5 基座（5174），供 C 端商城、执业药师端复用

- [ ] **Step 1: 写 package.json / vite.config.js / postcss.config.js / index.html / 环境变量**

```json
{
  "name": "tcm-h5-base",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "axios": "^1.7.7",
    "pinia": "^2.2.4",
    "vant": "^4.9.8",
    "vue": "^3.4.38",
    "vue-router": "^4.4.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.4",
    "postcss-px-to-viewport": "^1.1.1",
    "vite": "^5.4.8"
  }
}
```

```js
// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

```js
// postcss.config.js — 375 设计稿 px → vw 移动端适配
export default {
  plugins: {
    'postcss-px-to-viewport': {
      viewportWidth: 375,
      unitPrecision: 5,
      viewportUnit: 'vw',
      selectorBlackList: [],
      minPixelValue: 1,
      mediaQuery: false,
    },
  },
}
```

`.env.development` / `.env.production`：`VITE_API_BASE=/api`
`index.html`：同 tcm-pc-base，标题改为「TCM H5 基座」，引入 `src/main.js`。

- [ ] **Step 2: 写 request.js 与 stores/user.js（与 tcm-pc-base 相同逻辑，提示组件换成 vant 的 showToast/showDialog）**

```js
// src/utils/request.js
import axios from 'axios'
import { showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = userStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 2001) {
      useUserStore().resetAuth()
      router.push('/login')
      return Promise.reject(new Error(res.msg))
    }
    showToast(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const res = error.response?.data
    if (error.response?.status === 401 || res?.code === 2001) {
      useUserStore().resetAuth()
      router.push('/login')
    } else {
      showToast(res?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
```

`src/stores/user.js`：与 tcm-pc-base 完全相同（login/logout/fetchInfo/resetAuth）。

- [ ] **Step 3: 写 router / main.js / App.vue**

```js
// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { title: '登录' } },
  { path: '/', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const userStore = useUserStore()
  const isLoggedIn = !!userStore.token
  if (to.path !== '/login' && !isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && isLoggedIn) {
    return '/'
  }
})

export default router
```

```js
// src/main.js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Vant from 'vant'
import 'vant/lib/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Vant)
app.mount('#app')
```

```vue
<!-- src/App.vue -->
<template>
  <router-view />
</template>
```

- [ ] **Step 4: 写登录页与首页（Vant 组件）**

```vue
<!-- src/views/login/index.vue -->
<template>
  <div class="login-page">
    <div class="login-title">TCM H5 基座</div>
    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field v-model="form.username" name="username" label="用户名" placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]" />
        <van-field v-model="form.password" type="password" name="password" label="密码" placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]" />
      </van-cell-group>
      <div style="margin: 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">登 录</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(form)
    showSuccessToast('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1f3a5f 0%, #2d5f8a 100%);
  padding-top: 20vh;
}
.login-title {
  text-align: center;
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 24px;
}
</style>
```

```vue
<!-- src/views/home/index.vue -->
<template>
  <div class="home-page">
    <van-nav-bar title="首页" />
    <van-cell-group inset>
      <van-cell title="当前用户" :value="userStore.userInfo?.realName || '-'" />
      <van-cell title="登录账号" :value="userStore.userInfo?.username || '-'" />
    </van-cell-group>
    <div class="home-tip">H5 基座脚手架完成：登录鉴权、请求封装、移动端适配已打通。</div>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.home-tip {
  margin: 16px;
  color: #969799;
  font-size: 13px;
}
</style>
```

- [ ] **Step 5: 写配套配置文件**

`.eslintrc.cjs`、`.prettierrc`、`.gitignore`、`jsconfig.json`：与 tcm-pc-base 相同。

- [ ] **Step 6: 安装依赖并构建验证**

Run（工作目录 `f:/MyProject/ylxm/tcm-h5-base`）：
`npm install`
`npm run build`
Expected: 构建成功，输出 `dist/`

- [ ] **Step 7: 提交**

```bash
git add -A && git commit -m "feat(h5-base): Vant H5 移动端基座（登录/首页/请求封装/px转vw）"
```

---

### Task 14: README 与验收清单核对

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: 全部 Task 产物
- Produces: 架构约定 + 启动手册（交付物）

- [ ] **Step 1: 写 README.md**

```markdown
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
- 登录接口：POST http://localhost:8080/api/auth/login（admin / 123456，Authorization 头携带返回 token）

### 3. 前端

```bash
# PC 基座（5173）
cd tcm-pc-base && npm install && npm run dev
# H5 基座（5174）
cd tcm-h5-base && npm install && npm run dev
```

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
```

- [ ] **Step 2: 最终全量验证**

Run: `mvn -q clean verify`（工作目录 `f:/MyProject/ylxm`，跳过需达梦的运行验证）
Expected: BUILD SUCCESS，全部单元测试通过

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "docs: README 架构约定与启动手册"
```

---

## 自评检查

- **Spec 覆盖**：版本矩阵（Task 1）✓ 包结构（Task 1/8/10）✓ base 基类（Task 2）✓ 异常体系（Task 3）✓ 工具类（Task 4）✓ 审计（Task 5）✓ 锁（Task 6）✓ MQ（Task 7）✓ DM8+MP 跑通（Task 8）✓ Sa-Token demo（Task 9）✓ 适配层+域骨架（Task 10）✓ SQL 脚本（Task 11）✓ PC 基座（Task 12）✓ H5 基座（Task 13）✓ README+验收（Task 14）✓
- **占位符扫描**：唯一 TODO 是 WMS 适配器 Mock 内的"实际实现"提示（脚手架阶段有意为之，注明后续迭代）；其余全部为可执行内容。
- **类型一致性**：`OperatorProvider`（Task 5 接口 / Task 9 SaOperatorProvider）、`RedisLockUtil` 三组 API（Task 6 / 后续业务引用）、`AuditLog` 注解属性（Task 5 / Task 9 登录接口）、Result 工厂方法（Task 2 / Task 3/9）签名一致。
