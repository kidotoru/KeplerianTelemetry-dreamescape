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

## Orbit lines

Both dashboards draw orbit lines from the point arrays KSD sends (`orbitLegs`,
fetched with `?orbits=true` only when an object's `orbitRev` changes), not from
`kep`. The points already have every KSD-side frame conversion applied (parent
body's axial tilt, ground-fixed display during launch, SOI legs), so the line
is identical to KSD's own and the marker always sits on it. `kep` is in the
parent's equatorial frame and does **not** match `cart.pos`; don't compute
orbit lines from it. `cart.vel` is relative to the parent body, in the same
axes as `cart.pos`. See README "軌道線の描き方".

(This replaced an earlier `earth.html` workaround that derived elements from
`cart.pos`/`cart.vel`; that was unreliable because KSD used to send `cart.vel`
in the untilted parent frame.)
