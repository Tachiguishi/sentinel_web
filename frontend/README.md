# Sentinel 站点列表

Vue 和 Tailwind CSS 页面，通过 Spring Boot API 读取 MySQL 中的站点记录。

## 本地启动

在仓库根目录启动后端，并将已配置的 MySQL 密码传入环境变量。

```sh
SENTINEL_DB_PASSWORD=你的数据库密码 mvn spring-boot:run
```

默认连接 `localhost:3306/ora9a`，用户名为 `rs5000`。如需调整，可设置 `SENTINEL_DB_URL` 和 `SENTINEL_DB_USER`。

站点查询使用 Spring Data JPA，分页语句由 Hibernate 根据数据库方言生成。迁移数据库时，需在目标库创建或导入 `sentinel_station` 表，添加对应的 JDBC 驱动，并更新连接配置。若 Hibernate 无法自动识别目标数据库，还需配置兼容的方言。

如果已有 MySQL 表缺少 `create_at`，执行下面的迁移即可保留已有记录。

```sql
ALTER TABLE sentinel_station
  ADD COLUMN create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
```

在另一个终端进入 `frontend`，运行下面的命令，再打开 [站点列表](http://localhost:5173)。Vite 会把 `/api` 请求转发到 `127.0.0.1:31313`。

```sh
npm ci
npm run dev
```

## 更新站点 SCD

从站点列表进入「详情」，点击「更新SCD」并选择不超过 50 MB 的 `.scd` 文件。导入结果保存在后端工作目录的 `data/scd/` 中，可用 `SENTINEL_SCD_DIRECTORY` 更改路径。每个 SQLite 文件用站点名的 UTF-8 SHA-256 命名。首次导入创建数据库，之后在原库中以事务更新 SCD 数据，失败时回滚。

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```

### Run End-to-End Tests with [Playwright](https://playwright.dev)

```sh
# Install browsers for the first run
npx playwright install

# When testing on CI, must build the project first
npm run build

# Runs the end-to-end tests
npm run test:e2e
# Runs the tests only on Chromium
npm run test:e2e -- --project=chromium
# Runs the tests of a specific file
npm run test:e2e -- tests/example.spec.ts
# Runs the tests in debug mode
npm run test:e2e -- --debug
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```
