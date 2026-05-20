# ManyFoot Web UI

ManyFoot Web UI 是 ManyFoot Supervisor 多智能体系统的前端聊天界面，基于 React + Vite 构建。它负责会话管理、文件上传、SSE 流式响应展示，以及后端 API 地址配置。

## 技术栈

- React 19
- TypeScript
- Vite
- Tailwind CSS CDN（主题配置保留在 `index.html`）
- react-markdown + remark-gfm
- lucide-react

## 项目结构

```text
foot-ui/
├── index.html              # Vite HTML 入口，包含 Tailwind CDN 主题配置
├── package.json            # npm 脚本与依赖
├── package-lock.json       # npm 锁文件
├── vite.config.ts          # 开发服务器与 /api 代理
├── tsconfig.json           # TypeScript 配置
└── src/
    ├── main.tsx            # React 挂载入口
    ├── App.tsx             # 会话状态、流式响应聚合与主布局
    ├── types.ts            # 前端 API 与 UI 类型
    ├── components/         # 展示组件
    └── services/
        └── manyFootApi.ts  # ManyFoot 后端 API 客户端
```

## 开发命令

```bash
npm install
npm run dev
npm run typecheck
npm run build
npm run preview
```

开发服务器默认运行在 `http://localhost:3000`。

## 后端集成

开发环境中，Vite 会把 `/api` 代理到 ManyFoot 后端：

```text
/api -> http://127.0.0.1:8100
```

当前前端使用的接口：

- `POST /api/chat/stream`：SSE 流式对话响应
- `POST /api/chat/upload`：上传会话附件

设置弹窗中的 `baseUrl` 留空时使用 Vite 代理；填写后会直接请求该基础地址。

## 迁移清理说明

该目录已从整包粘贴状态整理为子项目结构：

- 删除本地生成物：`node_modules/`、`dist/`、`.idea/`、`.playwright-mcp/`、`.DS_Store`
- 删除嵌套 `.git/`，避免主仓库出现 embedded repo/submodule 混乱
- 统一使用 npm，保留 `package-lock.json`，删除 `bun.lock`
- 删除 AI Studio/Gemini 模板残留：`metadata.json`、importmap、未用 Gemini 依赖
- 源码迁移到标准 `src/` 目录

## 注意事项

- 不要提交 `node_modules/` 和 `dist/`。
- 样式仍主要通过 Tailwind CDN 类名表达；全局主题配置在 `index.html`。
- 流式响应合并逻辑位于 `src/App.tsx`，用于避免后端最终稿重放导致回答重复。
