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
