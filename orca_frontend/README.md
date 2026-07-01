# Orca Frontend

This module contains Orca's reference frontend delivery slices.

## Requirements

- Node.js 20
- npm 10 or later

## Local Startup

```bash
npm install
npm run dev
```

Open `http://localhost:5173`.

The Vite development server proxies `/api` requests to
`http://localhost:8080`. The login shell can be viewed without the backend, but
a real login result requires the Orca backend and its local database state.

## Verification

```bash
npm test
npm run build
npm audit
```

Frontend 01 does not inspect session state after login and does not provide a
protected route or organization command UI.
