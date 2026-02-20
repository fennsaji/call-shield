# CallGuard

**You decide what gets through. We never see who's calling.**

CallGuard is a privacy-first spam and scam call protection app for Android, built for the Indian market. Unlike Truecaller, it works without uploading your contacts, storing your call logs, or showing you ads.

## How it works

- Incoming calls are checked against a local seed database, your personal rules, and an anonymous reputation system — in that order.
- Phone numbers are hashed on your device before anything leaves it. The backend never sees a raw number.
- Blocking decisions happen on-device. The cloud is only consulted for reputation signals, with a hard 1500ms timeout and full offline fallback.

## Key features

- **Call screening** — block, silence, or allow calls before they ring
- **Prefix blocking** — block entire number series (e.g., promotional ranges)
- **Anonymous spam reporting** — reports are hash-keyed; no identity attached
- **Missed call risk indicator** — warns before you call back an unknown number
- **No contact upload. No ads. No surveillance.**

## Tech stack

- **Android** — Kotlin, Jetpack Compose, Hilt, Room, Ktor
- **Backend** — Supabase (PostgreSQL + Edge Functions), hash-keyed reputation storage
- **Architecture** — Clean Architecture + MVVM; Call Screening Service with circuit breaker

## Status

Currently in pre-development. Phase 1 (MVP) documentation is complete. See [`docs/`](docs/) for full product and technical specifications.

| Document | Description |
|---|---|
| [`docs/Roadmap.md`](docs/Roadmap.md) | Phase-by-phase feature plan |
| [`docs/PRD/Phase 1 PRD.md`](docs/PRD/Phase%201%20PRD.md) | MVP requirements |
| [`docs/Tech Stack.md`](docs/Tech%20Stack.md) | Architecture and schema |
| [`docs/Developer Guidelines.md`](docs/Developer%20Guidelines.md) | Engineering rules and conventions |

## License

Copyright (c) 2026 Fenn. All rights reserved. See [LICENSE](LICENSE) for details.
