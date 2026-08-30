package com.tcm.common.audit;

/**
 * 当前操作人提供者：由主工程实现（基于 Sa-Token），common 不直接依赖鉴权框架。
 * 未登录场景返回 null。
 */
public interface OperatorProvider {

    Long currentOperatorId();

    String currentOperatorName();
}
