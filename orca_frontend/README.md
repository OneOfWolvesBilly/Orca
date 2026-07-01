# Orca Frontend

This directory contains Orca's independently buildable frontend framework
applications.

## Framework Targets

| Framework | Location | Status |
| --- | --- | --- |
| React | `react/` | Reference implementation |
| Vue | `vue/` | Planned |
| Angular | `angular/` | Planned |

React is implemented first. Vue and Angular will implement the same
authoritative frontend behavior in their own framework applications. Framework
applications do not import UI components from one another.

## Requirements

- Node.js 20
- npm 10 or later

## Local Startup

```bash
cd react
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite development server proxies `/api` requests to
`http://localhost:8080`. The login shell can be viewed without the backend, but
a real login result requires the Orca backend and its local database state.

## Verification

```bash
cd react
npm test
npm run build
npm audit
```

The React reference for Frontend 01 does not inspect session state after login
and does not provide a protected route or organization command UI.
