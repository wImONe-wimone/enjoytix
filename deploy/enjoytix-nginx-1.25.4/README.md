# EnjoyTix Nginx 部署包

该目录按 `deploy/12306-nginx-1.25.4` 的 Windows Nginx 解压结构构建，包含 `nginx.exe`、`conf`、`docs`、`contrib`、`html`、`logs`、`temp` 和启动脚本。

## 目录说明

- `html/dist-enjoytix/`: EnjoyTix 前端静态资源，来源于 `services/gateway-service/src/main/resources/static`。
- `conf/nginx.conf`: 前端站点和后端网关反向代理配置。
- `logs/`: Nginx 访问日志、错误日志和 PID 文件。
- `temp/`: Nginx 运行时临时目录。

## 启动方式

在当前目录执行：

```bat
test-nginx.bat
start-nginx.bat
```

启动后访问：

```text
http://localhost:5176/
```

停止或重载：

```bat
stop-nginx.bat
reload-nginx.bat
```

## 代理规则

- `/api...` 转发到 `http://127.0.0.1:9000/api...`
- `/v3/api-docs...`、`/swagger-ui...`、`/swagger-ui.html` 转发到 `http://127.0.0.1:9000`
- 前端 SPA 路由刷新使用 `try_files $uri $uri/ /index.html`

启动前请确认 EnjoyTix 网关服务已监听 `127.0.0.1:9000`。
