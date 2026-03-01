# Homework Frontend

## Run

```bash
npm install
npm run dev
```

Default dev url: `http://localhost:5173`

Backend API base is configured to `http://localhost:8081/api/v1`.
Production builds can override this via `VITE_API_BASE_URL`.

## Test

```bash
npm test
```

The frontend test command compiles shared TypeScript route/api helpers into `.test-dist`
and runs zero-dependency assertion scripts against the generated JavaScript.
