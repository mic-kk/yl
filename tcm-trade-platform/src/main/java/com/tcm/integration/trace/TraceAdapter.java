package com.tcm.integration.trace;

import java.util.Map;

/**
 * 溯源系统适配器：药材流转节点上报、溯源码查询。
 * 约束：只做报文转换/协议适配，不写业务逻辑。
 */
public interface TraceAdapter {

    /** 上报流转节点（入库、出库、签收等） */
    void reportTraceNode(Map<String, Object> nodeData);

    /** 查询溯源码全链路信息 */
    Map<String, Object> queryTrace(String traceCode);
}
