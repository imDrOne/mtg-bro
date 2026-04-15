# React Login UI — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Заменить дефолтную страницу логина Spring Security на кастомное React-приложение, раздаваемое Caddy на том же домене, что и auth-service.

**Architecture:** React SPA собирается отдельно и деплоится как статические файлы в Docker-контейнер Caddy. Caddy раздаёт `/login` и связанные ассеты напрямую, всё остальное проксирует в auth-service. Spring Security перенаправляет неаутентифицированных пользователей на `/login` (React SPA), а React отправляет `<form>` submit на `POST /login` (обрабатывается Spring). OAuth2 flow работает прозрачно — тот же домен, та же сессия.

**Tech Stack:** React 19, TypeScript, Vite, Tailwind CSS 4, Caddy (static files), Spring Security (form login)

---

## Обзор архитектуры

```
Браузер
  │
  │ GET /oauth2/authorize
  ▼
┌─────────────────────────────────────────────────────────┐
│ Caddy                                                   │
│                                                         │
│  /login, /register, /assets/*  →  static files (React)  │
│  всё остальное                 →  reverse_proxy :8080    │
│                                                         │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────────┐
│ auth-service (Spring Boot)                              │
│                                                         │
│  POST /login              ← Spring обрабатывает form    │
│  POST /api/v1/users/register  ← регистрация             │
│  GET /oauth2/authorize    ← OAuth2 flow                 │
│  ...                                                    │
│                                                         │
│  SecurityConfig:                                        │
│    .loginPage("/login")   ← redirect сюда              │
│    .loginProcessingUrl("/login")  ← form action сюда   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Важный нюанс:** `GET /login` и `POST /login` — разные HTTP-методы на одном URL.
- `GET /login` → Caddy отдаёт React SPA (статический файл).
- `POST /login` → Caddy проксирует в Spring (static files не обрабатывают POST).

Caddy по умолчанию `file_server` отвечает только на GET/HEAD. POST `/login` не найдёт файл → Caddy прокинет его дальше через fallback reverse_proxy. Для этого используем `route` директиву с правильным приоритетом.

---

## File Structure

```
auth-ui/                            # Новый модуль — React SPA
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
├── src/
│   ├── main.tsx                    # Entry point
│   ├── App.tsx                     # Router
│   ├── pages/
│   │   ├── LoginPage.tsx           # Форма логина
│   │   └── RegisterPage.tsx        # Форма регистрации
│   ├── components/
│   │   └── AuthLayout.tsx          # Общий layout для auth-страниц
│   └── styles/
│       └── index.css               # Tailwind entry point
├── public/
│   └── favicon.ico

docker/caddy/Caddyfile              # Modify: добавить static file serving
docker/caddy/Dockerfile             # Modify: копировать React build

auth-service/
├── src/main/kotlin/.../security/
│   ├── SecurityConfig.kt          # Modify: loginPage("/login"), CSRF cookie, убрать failure handler
│   └── AuthorizationServerConfig.kt  # Без изменений (LoginUrlAuthenticationEntryPoint("/login") уже стоит)
```

---

### Task 1: Scaffold React-приложения (auth-ui)

**Files:**
- Create: `auth-ui/package.json`
- Create: `auth-ui/tsconfig.json`
- Create: `auth-ui/vite.config.ts`
- Create: `auth-ui/index.html`
- Create: `auth-ui/src/main.tsx`
- Create: `auth-ui/src/styles/index.css`

- [ ] **Step 1: Создать React-приложение через Vite**

```bash
cd /Users/a.tikholoz/IdeaProjects/mtg-bro
npm create vite@latest auth-ui -- --template react-ts
```

- [ ] **Step 2: Установить зависимости**

```bash
cd auth-ui
npm install
npm install -D tailwindcss @tailwindcss/vite
```

- [ ] **Step 3: Настроить Vite для Tailwind и base path**

Заменить содержимое `auth-ui/vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: 'dist',
  },
})
```

- [ ] **Step 4: Настроить Tailwind CSS**

Заменить содержимое `auth-ui/src/index.css`:

```css
@import "tailwindcss";
```

- [ ] **Step 5: Убедиться, что dev server запускается**

```bash
cd auth-ui
npm run dev
```

Expected: Vite dev server на `http://localhost:5173`, страница открывается в браузере.

- [ ] **Step 6: Commit**

```bash
git add auth-ui/
git commit -m "feat(auth-ui): scaffold React app with Vite + Tailwind"
```

---

### Task 2: Login Page

**Files:**
- Create: `auth-ui/src/pages/LoginPage.tsx`
- Create: `auth-ui/src/components/AuthLayout.tsx`
- Modify: `auth-ui/src/main.tsx`
- Modify: `auth-ui/src/App.tsx`

- [ ] **Step 1: Создать AuthLayout — общий layout**

Создать `auth-ui/src/components/AuthLayout.tsx`:

```tsx
import type { ReactNode } from "react";

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950 px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white tracking-tight">
            MTG Bro
          </h1>
          <p className="text-gray-400 mt-1 text-sm">
            Deck-building assistant
          </p>
        </div>
        {children}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Создать LoginPage**

Создать `auth-ui/src/pages/LoginPage.tsx`:

```tsx
import { useState } from "react";
import { AuthLayout } from "../components/AuthLayout";

export function LoginPage() {
  const [error, setError] = useState(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get("error") !== null;
  });

  return (
    <AuthLayout>
      <form
        method="post"
        action="/login"
        className="bg-gray-900 rounded-2xl border border-gray-800 p-6 shadow-xl space-y-5"
      >
        <h2 className="text-xl font-semibold text-white">Sign in</h2>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg px-4 py-3 text-sm text-red-400">
            Invalid email or password
          </div>
        )}

        <div className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-300 mb-1.5">
              Email
            </label>
            <input
              id="username"
              name="username"
              type="email"
              required
              autoComplete="email"
              autoFocus
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-white
                         placeholder-gray-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500
                         outline-none transition-colors"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-1.5">
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              required
              autoComplete="current-password"
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-white
                         placeholder-gray-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500
                         outline-none transition-colors"
              placeholder="••••••••"
            />
          </div>
        </div>

        <button
          type="submit"
          className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white
                     hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500
                     focus:ring-offset-2 focus:ring-offset-gray-900 transition-colors cursor-pointer"
        >
          Sign in
        </button>

        <p className="text-center text-sm text-gray-400">
          Don't have an account?{" "}
          <a href="/register" className="text-indigo-400 hover:text-indigo-300 transition-colors">
            Create one
          </a>
        </p>
      </form>
    </AuthLayout>
  );
}
```

> **Ключевой момент:** `<form method="post" action="/login">` — обычный form submit, не fetch.
> Браузер отправляет POST, Spring обрабатывает, делает 302 redirect.
> Поле `name="username"` — Spring convention, хотя туда вводится email.
> CSRF-токен не нужен в форме — мы отключим CSRF для POST /login (уже отключен в текущей конфигурации).

- [ ] **Step 3: Обновить App.tsx — routing**

Заменить содержимое `auth-ui/src/App.tsx`:

```tsx
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";

export default function App() {
  const path = window.location.pathname;

  if (path === "/register") {
    return <RegisterPage />;
  }

  return <LoginPage />;
}
```

> Используем простой pathname-based routing без react-router — всего 2 страницы,
> не нужна лишняя зависимость.

- [ ] **Step 4: Обновить main.tsx**

Заменить содержимое `auth-ui/src/main.tsx`:

```tsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
```

- [ ] **Step 5: Обновить index.html — title**

Обновить `<title>` в `auth-ui/index.html`:

```html
<title>MTG Bro — Sign in</title>
```

- [ ] **Step 6: Проверить визуально**

```bash
cd auth-ui && npm run dev
```

Открыть `http://localhost:5173` — должна отобразиться форма логина на тёмном фоне.
Открыть `http://localhost:5173/register` — должна отобразиться заглушка (создадим в Task 3).

- [ ] **Step 7: Commit**

```bash
git add auth-ui/src/
git commit -m "feat(auth-ui): add login page with dark theme"
```

---

### Task 3: Register Page

**Files:**
- Create: `auth-ui/src/pages/RegisterPage.tsx`

- [ ] **Step 1: Создать RegisterPage**

Создать `auth-ui/src/pages/RegisterPage.tsx`:

```tsx
import { useState, type FormEvent } from "react";
import { AuthLayout } from "../components/AuthLayout";

export function RegisterPage() {
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);

    const form = e.currentTarget;
    const data = {
      email: (form.elements.namedItem("email") as HTMLInputElement).value,
      username: (form.elements.namedItem("username") as HTMLInputElement).value,
      password: (form.elements.namedItem("password") as HTMLInputElement).value,
    };

    try {
      const res = await fetch("/api/v1/users/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });

      if (res.ok) {
        window.location.href = "/login?registered";
        return;
      }

      const body = await res.json().catch(() => null);
      setError(body?.message ?? `Registration failed (${res.status})`);
    } catch {
      setError("Network error. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <form
        onSubmit={handleSubmit}
        className="bg-gray-900 rounded-2xl border border-gray-800 p-6 shadow-xl space-y-5"
      >
        <h2 className="text-xl font-semibold text-white">Create account</h2>

        {error && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-lg px-4 py-3 text-sm text-red-400">
            {error}
          </div>
        )}

        <div className="space-y-4">
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-300 mb-1.5">
              Email
            </label>
            <input
              id="email"
              name="email"
              type="email"
              required
              autoComplete="email"
              autoFocus
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-white
                         placeholder-gray-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500
                         outline-none transition-colors"
              placeholder="you@example.com"
            />
          </div>

          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-300 mb-1.5">
              Username
            </label>
            <input
              id="username"
              name="username"
              type="text"
              required
              autoComplete="username"
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-white
                         placeholder-gray-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500
                         outline-none transition-colors"
              placeholder="wizard42"
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-300 mb-1.5">
              Password
            </label>
            <input
              id="password"
              name="password"
              type="password"
              required
              minLength={6}
              autoComplete="new-password"
              className="w-full rounded-lg border border-gray-700 bg-gray-800 px-3 py-2 text-white
                         placeholder-gray-500 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500
                         outline-none transition-colors"
              placeholder="••••••••"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white
                     hover:bg-indigo-500 focus:outline-none focus:ring-2 focus:ring-indigo-500
                     focus:ring-offset-2 focus:ring-offset-gray-900 transition-colors
                     disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
        >
          {loading ? "Creating..." : "Create account"}
        </button>

        <p className="text-center text-sm text-gray-400">
          Already have an account?{" "}
          <a href="/login" className="text-indigo-400 hover:text-indigo-300 transition-colors">
            Sign in
          </a>
        </p>
      </form>
    </AuthLayout>
  );
}
```

> Регистрация использует `fetch` (JSON API), а не form submit — потому что
> `POST /api/v1/users/register` возвращает JSON, а не redirect.
> После успеха — redirect на `/login?registered`.

- [ ] **Step 2: Добавить «registered» уведомление в LoginPage**

В `LoginPage.tsx` добавить сообщение об успешной регистрации. Под инициализацией `error` добавить:

```tsx
const [registered] = useState(() => {
  const params = new URLSearchParams(window.location.search);
  return params.get("registered") !== null;
});
```

И в JSX перед блоком с ошибкой добавить:

```tsx
{registered && (
  <div className="bg-green-500/10 border border-green-500/30 rounded-lg px-4 py-3 text-sm text-green-400">
    Account created! Sign in below.
  </div>
)}
```

- [ ] **Step 3: Проверить визуально**

```bash
cd auth-ui && npm run dev
```

Открыть `http://localhost:5173/register` — форма регистрации с 3 полями.
Открыть `http://localhost:5173/login?registered` — зелёное уведомление.

- [ ] **Step 4: Commit**

```bash
git add auth-ui/src/
git commit -m "feat(auth-ui): add registration page"
```

---

### Task 4: Spring Security — настройка loginPage

**Files:**
- Modify: `auth-service/src/main/kotlin/xyz/candycrawler/authservice/security/SecurityConfig.kt`

- [ ] **Step 1: Обновить SecurityConfig**

В `SecurityConfig.kt` изменить блок `formLogin` и `authorizeHttpRequests`:

```kotlin
@Bean
@Order(2)
fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/api/v1/users/register").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/connect/logout").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/register").permitAll()
                .anyRequest().authenticated()
        }
        .formLogin { form ->
            form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .failureUrl("/login?error")
                .permitAll()
        }
        .csrf { csrf ->
            csrf.ignoringRequestMatchers("/api/v1/users/register", "/login")
        }

    return http.build()
}
```

Изменения по сравнению с текущей конфигурацией:
1. Добавлен `.loginPage("/login")` — теперь Spring **не** генерирует дефолтную страницу, а делает redirect на `/login` (Caddy отдаст React).
2. Добавлен `.failureUrl("/login?error")` — при ошибке логина redirect на React-страницу с query-параметром `?error`.
3. Убран `.failureHandler(jsonAuthenticationFailureHandler())` — он конфликтует с `failureUrl` (redirect vs JSON response).
4. Добавлены `permitAll()` для `/login` и `/register` — чтобы Spring не перехватывал GET-запросы к React SPA.

- [ ] **Step 2: Удалить неиспользуемый failure handler**

Удалить метод `jsonAuthenticationFailureHandler()` и его импорты из `SecurityConfig.kt`, так как теперь используется `failureUrl` вместо JSON-ответа.

Итоговый файл:

```kotlin
package xyz.candycrawler.authservice.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    @Order(2)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/users/register").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/connect/logout").permitAll()
                    .requestMatchers("/login").permitAll()
                    .requestMatchers("/register").permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { form ->
                form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .failureUrl("/login?error")
                    .permitAll()
            }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/v1/users/register", "/login")
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
```

- [ ] **Step 3: Убедиться, что auth-service компилируется**

```bash
./gradlew :auth-service:compileKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Запустить тесты auth-service**

```bash
./gradlew :auth-service:test
```

Expected: все тесты проходят. Если есть тесты, завязанные на JSON failure handler — обновить их.

- [ ] **Step 5: Commit**

```bash
git add auth-service/
git commit -m "feat(auth-service): configure loginPage redirect to React SPA"
```

---

### Task 5: Caddy — раздача React-статики

**Files:**
- Modify: `docker/caddy/Caddyfile`
- Modify: `docker/caddy/Dockerfile`

- [ ] **Step 1: Обновить Caddyfile**

Заменить блок `{$AUTH_DOMAIN}` в `docker/caddy/Caddyfile`:

```caddyfile
{
    email {$CADDY_EMAIL}
}

{$MCP_DOMAIN} {
    tls {
        dns duckdns {$DUCKDNS_TOKEN}
    }
    reverse_proxy mcp-server:3000
}

{$AUTH_DOMAIN} {
    tls {
        dns duckdns {$DUCKDNS_TOKEN}
    }

    @static_pages {
        method GET HEAD
        path /login /register
    }

    @static_assets {
        method GET HEAD
        path /assets/*
    }

    handle @static_pages {
        root * /srv/auth-ui
        rewrite * /index.html
        file_server
    }

    handle @static_assets {
        root * /srv/auth-ui
        file_server
    }

    handle {
        reverse_proxy auth-service:8080
    }
}
```

Логика:
- `GET /login`, `GET /register` → Caddy отдаёт `/srv/auth-ui/index.html` (React SPA).
- `GET /assets/*` → Caddy отдаёт JS/CSS bundles из `/srv/auth-ui/assets/`.
- `POST /login` → **не** матчится `@static_pages` (method GET HEAD) → попадает в `handle` → `reverse_proxy` → Spring обрабатывает.
- Все остальные запросы → `reverse_proxy` → Spring.

- [ ] **Step 2: Обновить Dockerfile — копировать React build**

Заменить содержимое `docker/caddy/Dockerfile`:

```dockerfile
FROM node:22-alpine AS ui-builder
WORKDIR /app
COPY auth-ui/package.json auth-ui/package-lock.json ./
RUN npm ci
COPY auth-ui/ .
RUN npm run build

FROM caddy:builder AS caddy-builder
RUN xcaddy build --with github.com/caddy-dns/duckdns

FROM caddy:latest
COPY --from=caddy-builder /usr/bin/caddy /usr/bin/caddy
COPY --from=ui-builder /app/dist /srv/auth-ui
```

> React собирается в multi-stage Docker build. Итоговый image содержит Caddy + статику auth-ui.

- [ ] **Step 3: Обновить Docker build context**

Caddy Dockerfile теперь ссылается на `auth-ui/` — нужно обновить build context в `docker-compose.prod.yml`.

В `docker/docker-compose.prod.yml` изменить секцию `caddy`:

```yaml
  caddy:
    container_name: caddy
    build:
      context: ..
      dockerfile: docker/caddy/Dockerfile
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./caddy/Caddyfile:/etc/caddy/Caddyfile
      - caddy_data:/data
      - caddy_config:/config
    env_file: [.infra.env]
    depends_on:
      - mcp-server
      - auth-service
    networks:
      - mtg-bro-net
```

Изменение: `build: ./caddy` → `build: { context: .., dockerfile: docker/caddy/Dockerfile }`.
Context поднят на уровень корня репозитория, чтобы `COPY auth-ui/` работал.

- [ ] **Step 4: Проверить Docker build**

```bash
cd /Users/a.tikholoz/IdeaProjects/mtg-bro
docker build -f docker/caddy/Dockerfile -t caddy-test .
```

Expected: image собирается успешно.

```bash
docker run --rm caddy-test ls /srv/auth-ui
```

Expected: `index.html`, `assets/` с JS/CSS файлами.

```bash
docker rmi caddy-test
```

- [ ] **Step 5: Commit**

```bash
git add docker/caddy/ docker/docker-compose.prod.yml
git commit -m "feat(caddy): serve React login UI as static files"
```

---

### Task 6: Локальная разработка — proxy в Vite

**Files:**
- Modify: `auth-ui/vite.config.ts`

- [ ] **Step 1: Настроить Vite proxy для локальной разработки**

При локальной разработке React dev server работает на `:5173`, а auth-service на `:8083`.
Чтобы `POST /login` и `POST /api/v1/users/register` работали, настроим proxy в Vite.

Обновить `auth-ui/vite.config.ts`:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: 'dist',
  },
  server: {
    proxy: {
      '/login': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            if (proxyReq.method === 'GET') {
              proxyReq.destroy();
            }
          });
        },
      },
      '/api': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
})
```

> Proxy нужен только для POST/API запросов. GET /login обрабатывается Vite (React SPA).
> Упрощённый вариант: проксировать только `/api` и `/oauth2`, а `/login` POST пойдёт через
> обычный form submit (браузер сам отправит на тот же origin → Vite proxy перехватит).

Более простой вариант — проксировать всё, кроме Vite assets:

```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    outDir: 'dist',
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8083',
      '/oauth2': 'http://localhost:8083',
      '/.well-known': 'http://localhost:8083',
    },
  },
})
```

> В этом варианте `POST /login` из form submit пойдёт на Vite dev server → Vite
> не найдёт обработчик → вернёт ошибку. Для полного E2E тестирования OAuth flow
> используй Docker Compose. Vite dev server — для визуальной разработки UI.

- [ ] **Step 2: Проверить**

Запустить auth-service локально:
```bash
./gradlew :auth-service:bootRun
```

В другом терминале:
```bash
cd auth-ui && npm run dev
```

Открыть `http://localhost:5173/login` — должна отобразиться форма.
Отправить регистрацию → `POST /api/v1/users/register` должен проксироваться в auth-service.

- [ ] **Step 3: Commit**

```bash
git add auth-ui/vite.config.ts
git commit -m "feat(auth-ui): add Vite proxy for local development"
```

---

### Task 7: Обновить docker-compose.local.yml

**Files:**
- Modify: `docker/docker-compose.local.yml`

- [ ] **Step 1: Решение для локальной разработки**

Для локальной разработки auth-ui используется Vite dev server напрямую (Task 6).
Docker Compose не требует изменений для auth-ui — при необходимости полного E2E
тестирования, пересобрать image caddy:

```bash
docker compose -f docker/docker-compose.prod.yml build caddy
```

Никаких изменений в `docker-compose.local.yml` не требуется.

- [ ] **Step 2: Commit (если были изменения)**

Нет изменений — пропускаем.

---

### Task 8: CI/CD — deploy Caddy при изменениях auth-ui

**Files:**
- Modify: `.github/workflows/` (при необходимости)

- [ ] **Step 1: Оценить текущий CI pipeline**

Текущий deploy через git tags (`<module>/v<semver>`). Caddy собирается из `docker/caddy/Dockerfile`
при deploy на сервер (`docker compose -f docker-compose.prod.yml up -d caddy`).

Caddy **не** деплоится через JIB — он собирается прямо на сервере через `docker compose build`.
Если `auth-ui/` изменился, нужно пересобрать caddy image на сервере.

- [ ] **Step 2: Добавить пересборку caddy в deploy скрипт**

Вариант A — пересобирать caddy при каждом деплое auth-service:

В `scripts/deploy.py` или в GitHub Actions workflow добавить шаг:
```bash
cd /opt/mtg-bro/docker
docker compose -f docker-compose.prod.yml build caddy
docker compose -f docker-compose.prod.yml up -d --no-deps caddy
```

Вариант B — отдельный tag/workflow для caddy. Это более чистый подход,
но требует дополнительного workflow. Решить на этапе реализации.

> **Рекомендация:** начать с Варианта A (пересборка caddy при деплое auth-service).
> Оптимизировать позже, если сборка станет медленной.

- [ ] **Step 3: Обновить deploy для caddy**

Нужно отправить `auth-ui/` на сервер или клонировать repo, чтобы Docker build context был доступен.

Текущий подход: `docker compose pull + up` — работает для JIB images (push в GHCR → pull на сервере).
Caddy собирается локально на сервере. Значит `auth-ui/` должен быть на сервере.

Самый простой вариант: на сервере делать `git pull` в `/opt/mtg-bro/` и пересобирать caddy.

Альтернатива: собирать caddy image в CI (GitHub Actions) и пушить в GHCR, как другие модули.
Тогда на сервере будет `docker compose pull caddy && up -d caddy` — консистентно с остальными.

> **Рекомендация:** собирать caddy image в CI и пушить в GHCR.
> Для этого нужен отдельный workflow trigger (например, path filter на `auth-ui/**` и `docker/caddy/**`).
> Детали реализации зависят от предпочтений — проработать при выполнении.

- [ ] **Step 4: Commit**

```bash
git add .github/ scripts/
git commit -m "ci: add caddy rebuild on auth-ui changes"
```

---

### Task 9: Обновить документацию

**Files:**
- Modify: `auth-service/CLAUDE.md`

- [ ] **Step 1: Обновить раздел «Кастомизация UI» в auth-service/CLAUDE.md**

Заменить секцию «Кастомизация UI (Thymeleaf) — Phase 2» на:

```markdown
## Кастомный Login UI (React SPA) — Phase 2

Login и Register страницы реализованы как отдельное React-приложение (`auth-ui/`),
раздаваемое Caddy на том же домене, что и auth-service.

### Архитектура

- `GET /login`, `GET /register` → Caddy отдаёт React SPA (`/srv/auth-ui/index.html`).
- `POST /login` → Caddy проксирует в Spring (form login).
- `POST /api/v1/users/register` → Caddy проксирует в Spring (JSON API).
- OAuth2 flow работает прозрачно — тот же домен, та же сессия.

### Локальная разработка

```bash
# Терминал 1: auth-service
./gradlew :auth-service:bootRun

# Терминал 2: React dev server
cd auth-ui && npm run dev
```

Vite проксирует `/api` и `/oauth2` в auth-service на `:8083`.

### Сборка и деплой

React собирается в multi-stage Docker build вместе с Caddy (`docker/caddy/Dockerfile`).
Caddy image содержит статику auth-ui в `/srv/auth-ui/`.

### Файлы

| Файл | Назначение |
|------|------------|
| `auth-ui/` | React SPA (Vite + TypeScript + Tailwind) |
| `docker/caddy/Caddyfile` | Routing: static pages vs reverse proxy |
| `docker/caddy/Dockerfile` | Multi-stage: Node build → Caddy image |
```

- [ ] **Step 2: Commit**

```bash
git add auth-service/CLAUDE.md
git commit -m "docs: update auth-service CLAUDE.md with React login UI info"
```

---

## Чеклист перед мержем

- [ ] `npm run build` в `auth-ui/` — production build без ошибок
- [ ] `./gradlew :auth-service:test` — все тесты проходят
- [ ] Docker build caddy — image собирается
- [ ] E2E: OAuth flow через Docker Compose — логин → redirect → token
- [ ] Визуально: страница логина и регистрации выглядят корректно

---

## Решения, принятые в плане

| Решение | Обоснование |
|---------|-------------|
| React SPA на том же домене через Caddy | Нет проблем с CORS, session cookies, CSRF |
| Form submit (не fetch) для POST /login | Spring redirect после логина работает нативно для OAuth flow |
| Fetch для POST /register | API возвращает JSON, нужна обработка ошибок на клиенте |
| Простой pathname routing без react-router | 2 страницы, не нужна лишняя зависимость |
| Tailwind CSS | Быстрая стилизация, тёмная тема, минимальный CSS |
| Multi-stage Docker build | React собирается в CI вместе с Caddy, не нужен отдельный Node-контейнер |
| CSRF отключен для /login | Уже отключен в текущей конфигурации (`csrf.ignoringRequestMatchers("/login")`) |
