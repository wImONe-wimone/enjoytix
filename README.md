# EnjoyTix - 文娱票务微服务系统

> 推荐 GitHub 仓库标题：**EnjoyTix - 文娱票务微服务系统**

EnjoyTix 是一个面向同城文娱场景的在线票务系统，覆盖用户登录、观演人管理、文娱项目浏览、演出时间选择、票档库存、选座锁座、下单、模拟支付、出票和订单超时释放等核心链路。

项目采用 Java 17、Spring Boot 3、Spring Cloud Alibaba 和 Maven 多模块架构。默认模式使用内存仓储，便于本地快速验证；`mysql` profile 支持 MyBatis-Plus、ShardingSphere JDBC 和 MySQL 持久化，适合作为微服务票务系统的学习、演示和二次开发基础。

## 核心功能

- 用户账号：注册、登录、登出、当前用户资料、收货地址管理。
- 观演人管理：新增、编辑、删除、设为默认、证件信息维护。
- 文娱票务项目：项目列表、详情、类型筛选、日期筛选、艺人/团队搜索。
- 票务库存：票档余票、座位状态、库存锁定、释放和出票。
- 订单链路：创建订单、取消订单、订单详情、订单列表、支付成功确认。
- 支付链路：创建支付单、模拟支付成功、退款申请基础能力。
- 超时关闭：订单超时自动关闭，并释放锁定库存和座位。
- 网关能力：统一路由、鉴权过滤、OpenAPI 聚合入口。
- 前端页面：项目浏览、项目详情、选座下单、订单、票夹、账户中心。

## 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言与构建 | Java 17, Maven |
| 后端框架 | Spring Boot 3.0.7, Spring Cloud 2022, Spring Cloud Alibaba |
| 网关 | Spring Cloud Gateway |
| 数据访问 | MyBatis-Plus, ShardingSphere JDBC |
| 数据库 | MySQL 8 |
| 服务治理 | Nacos，可按环境关闭或替换 |
| 可观测性 | Spring Boot Actuator, Micrometer, Prometheus Registry |
| API 文档 | Springdoc OpenAPI / Swagger UI |
| 通用能力 | 统一响应、统一异常、参数校验、分布式 ID、缓存封装、幂等、操作日志 |
| 前端部署 | 原生 HTML/CSS/JavaScript, Nginx 静态部署 |

## 目录结构

```text
enjoytix
├─ dependencies/                  # 统一依赖版本管理
├─ frameworks/                    # 公共框架模块
│  ├─ base/                       # 基础常量与通用能力
│  ├─ convention/                 # 统一响应、异常、分页对象
│  ├─ common/                     # 通用扩展
│  ├─ web/                        # Web 异常与响应处理
│  ├─ database/                   # BaseDO、MyBatis-Plus 基础配置
│  ├─ cache/                      # 缓存、锁模板
│  ├─ distributed-id/             # 分布式 ID
│  ├─ idempotent/                 # 幂等注解与切面
│  └─ log/                        # 操作日志注解与切面
├─ services/
│  ├─ gateway-service/            # API 网关与静态前端副本
│  ├─ user-service/               # 用户、观演人、地址、用户订单视图
│  ├─ performance-service/        # 项目、艺人、场馆、演出时间、票档、座位图
│  ├─ ticket-service/             # 余票、座位库存、锁座、释放、出票
│  ├─ order-service/              # 下单、取消、支付确认、超时关闭
│  ├─ pay-service/                # 支付单、模拟支付、退款
│  ├─ marketing-service/          # 营销扩展模块
│  └─ aggregation-service/        # 聚合扩展模块
├─ tests/
│  └─ mvp-flow-test/              # 端到端购票主链路测试
├─ scripts/mysql/                 # MySQL 建库、建表、种子和校验脚本
├─ deploy/enjoytix-nginx-1.25.4/  # Windows Nginx 前端部署包
├─ docs/                          # 项目演进文档
└─ pom.xml                        # 根 Maven 聚合工程
```

## 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8，可选，仅 `mysql` profile 需要
- Nacos，可选，使用网关 `lb://` 路由进行多服务联调时需要
- Windows Nginx，可选，使用仓库内前端部署包时需要

## 服务端口

| 服务 | 端口 |
| --- | --- |
| gateway-service | 9000 |
| user-service | 9010 |
| performance-service | 9020 |
| ticket-service | 9030 |
| order-service | 9040 |
| pay-service | 9050 |

Swagger UI：

```text
http://localhost:9000/swagger-ui.html
```

## 配置说明

- 默认 profile：使用内存仓储，适合快速测试和本地开发。
- `mysql` profile：启用 MyBatis-Plus 和 ShardingSphere JDBC，连接各服务独立 schema。
- MySQL 连接配置位于各服务的 `src/main/resources/shardingsphere-*.yaml`。
- 网关路由位于 `services/gateway-service/src/main/resources/application.yaml`。
- 前端 API 基础路径可在 `config.js` 中调整。
- 访问受保护接口时需要携带 `Authorization: Bearer <access-token>`，不要提交真实令牌到仓库。

## 主链路说明

1. 用户登录并选择文娱项目。
2. 用户选择演出时间、票档和座位。
3. 订单服务向票务服务确认库存。
4. 票务服务锁定票档库存和座位。
5. 订单服务生成待支付订单。
6. 支付服务创建支付单并模拟支付成功。
7. 订单服务确认支付结果。
8. 票务服务出票，库存和座位变为已售。
9. 超时未支付订单会关闭，并释放锁定库存和座位。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。
