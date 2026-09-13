# Spring AI Alibaba RAG 运维手册

本文说明 `agent-service` 知识库 RAG 的权限边界、索引运维、灰度发布、质量门禁和回滚流程。默认配置保持 RAG 关闭，不需要 MySQL、Nacos 或 DashScope 凭据即可运行确定性测试。

## 权限与数据边界

- 检索请求必须提供当前用户、租户和知识库范围；服务只返回授权租户、知识库及已激活文档版本的 chunk。
- 文档版本不可变。新内容必须生成新版本并完成整批向量化后才能激活；失败批次不会覆盖当前有效版本。
- 引用只允许来自本次授权检索结果，使用不透明 citation key 映射标题、版本、chunk 和安全来源位置。
- 索引、重建和删除操作需要知识库管理权限；审计记录只保存主体、范围、版本、状态和失败分类，不保存密钥、原文或内部存储路径。

## 本地确定性验证

无需启动 MySQL/Nacos，也无需 `AI_DASHSCOPE_API_KEY`：

```powershell
mvn -pl services/agent-service -Dtest="*Knowledge*Test,RagRolloutDeciderTest" test
mvn -pl services/agent-service -Dtest="*Rag*Test" test
openspec validate --changes add-spring-ai-alibaba-rag --strict
```

默认内存 VectorStore 和 deterministic embedding double 用于测试。接入真实服务前，先保证上述测试通过。

## MySQL 索引操作

启用持久化前准备 MySQL 8 数据库并使用最小权限账号。启动时设置 `SPRING_PROFILES_ACTIVE=mysql`；Flyway 会执行 `V1__knowledge_base.sql`、`V2__agent_mysql.sql` 和 `V3__knowledge_rag_mysql.sql`。回滚脚本为 `db/migration/rollback/U3__knowledge_rag_mysql.sql`，执行前须确认已备份向量和版本元数据。

索引流程由 `KnowledgeIndexingJob` 执行：

1. 上传或变更文档后创建不可变版本和 fingerprint。
2. 按 batch 写入向量，失败批次按配置重试。
3. 全部 batch 成功后标记 generation ready 并激活。
4. 未变化版本重复执行是幂等的；变更版本不会删除当前有效版本，直到新版本激活。
5. 删除文档、chunk 或旧版本时写入 tombstone；删除处理可重试且幂等，检索立即排除失效 generation。

出现部分失败时，不要手工激活 generation；修复 embedding/vector store 后重跑该版本重建。删除误操作时先停止 tombstone 消费，再从备份恢复元数据并重新建立目标版本。

## 配置与灰度

核心 RAG 开关保持独立：`indexing-enabled`、`retrieval-enabled`、`reranking-enabled` 和 `citations-enabled`。灰度开关位于 `agent.knowledge.rag.rollout`：

- `AGENT_KNOWLEDGE_RAG_ROLLOUT_ENABLED=true` 才允许进入灰度。
- `AGENT_KNOWLEDGE_RAG_ROLLOUT_INTERNAL_USER_IDS=7,8` 让内部用户稳定进入。
- `AGENT_KNOWLEDGE_RAG_ROLLOUT_CANARY_PERCENTAGE=0..100` 按 `userId|cohortKey` 的 SHA-256 稳定分桶。
- `AGENT_KNOWLEDGE_RAG_ROLLOUT_ROLLBACK=true` 优先级最高，强制回退到非 RAG 路径。

配置变更通过部署平台或 Nacos 配置发布后重启 `agent-service`，确保回滚配置重新绑定；不依赖 JVM 内存状态。生产环境不得把 DashScope 密钥写入仓库或日志。

## 质量门禁与监控

发布前使用 `KnowledgeRetrievalQualityEvaluator` 检查 recall@k、首位排序准确率、引用覆盖率和无命中精度。默认阈值分别为 `0.80`、`0.80`、`0.80` 和 `0.90`，任何一项不达标都不扩大灰度。

观测重点包括检索 outcome、候选数、最终数、引用覆盖率、检索/模型延迟、token 使用量和估算成本，并按 tenant、knowledge base、cohort 和版本聚合。禁止记录原始查询、文档正文、密钥或授权头。通过 `/actuator/health` 查看 `rag` 指标：RAG 关闭时应为 `UP`；启用但 embedding/vector store 缺失时为 `DOWN`。

## 灰度与回滚

1. 仅开启 indexing，验证 generation ready、删除同步和审计脱敏。
2. 对内部用户开启 retrieval/citations，观察质量、越权告警、引用率、P95 延迟和成本。
3. 逐步提高 canary percentage；任一质量或安全门禁失败立即设置 `AGENT_KNOWLEDGE_RAG_ROLLOUT_ROLLBACK=true` 并重启。
4. 若 provider 故障，同时关闭 `AGENT_KNOWLEDGE_RAG_RETRIEVAL_ENABLED`、`AGENT_KNOWLEDGE_RAG_CITATIONS_ENABLED` 和模型开关；保留索引数据用于诊断。
5. 修复并重新通过本地确定性测试和质量门禁后，再关闭 rollback 并从 0% 灰度恢复。

回退始终保留原有非 RAG ChatClient/tool flow，不能改变用户身份、工具白名单、订单确认和幂等策略。