package com.tcm.integration.regulatory;

import java.util.Map;

/**
 * 监管上报适配器：税务、药监数据上报。
 * 约束：只做报文转换/协议适配，不写业务逻辑；失败重试、异常告警由调用方负责。
 */
public interface RegulatoryAdapter {

    /** 上报税务数据（实时/定时） */
    void reportTaxData(Map<String, Object> taxData);

    /** 上报药监协同数据（资质、交易、溯源） */
    void reportSupervisionData(Map<String, Object> data);
}
