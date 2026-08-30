# trace 业务域 — 溯源监控（M8）

## 职责
溯源数据上报、溯源码全链路查询、溯源断链/缺失异常监控。

## 约束
- 溯源系统交互走 integration 适配层（TraceAdapter）
- 对外只暴露 Service 接口给其他业务域，禁止跨域访问 Mapper/Entity

## 状态
脚手架阶段：空骨架，业务实现见后续迭代计划。
