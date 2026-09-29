# CLAUDE.md

Project context for Claude Code sessions working on this repo.

## What this is

Spring Boot server + reference web client for KSD (Keplerian Space Discovery,
an Unreal Engine project: https://github.com/kidotoru/keplerian) telemetry.
KSD pushes object/orbit data over WebSocket; this server exposes it via REST
(`/api/objects`), and `src/main/resources/static/` holds the reference web
dashboards. See `README.md` for the API spec.

- `index.html` — full solar-system view, tree-navigable (Sun → planets →
  satellites).
- `earth.html` — Earth-fixed view: always centered on Earth (object id 4,
  fixed), shows only Earth's own orbiting spacecraft/moon. Added for the
  dreamscape exhibition (see below).

## dreamscape exhibition (2026)

KSD will be exhibited at the "dreamscape" event: https://dreamscape-game.com/

Planned booth setup: **two monitors side by side**.
1. One runs KSD itself (the Unreal Engine game/sim).
2. The other shows this web dashboard (`earth.html`), live via the REST API.

Because it's a visitor-facing display, ongoing work on `earth.html` should
prioritize:
- **Visual polish** — this is a showpiece, not just a debug view.
- **Rich on-screen data** — surfacing more of the telemetry numbers
  (orbital elements, altitude, velocity, etc.) is a feature, not clutter.

## Known technical debt

`earth.html`'s orbit-line rendering currently derives Keplerian elements
client-side from each spacecraft's `cart.pos`/`cart.vel` instead of trusting
the server's `kep` field. This is a workaround for a KSD-side bug (KSD sends
`kep` in Earth's tilted-equatorial frame, not the same world frame as
`cart.pos`/`cart.vel`, causing marker/orbit-line misalignment for anything
orbiting a tilted body). See the `TODO(KSD-side fix)` comments in
`earth.html` near `cartesianToKepElements()` for the full explanation and
exactly what to revert once KSD is fixed server-side.
