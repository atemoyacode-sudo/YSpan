# Changelog

## v0.1.0.preview.3 — 2026-11-01 (Forge Preview)

- Add a separate Forge 1.16.5 client artifact, tested with Forge 36.2.42 and Java 8.
- Share companion negotiation, world lifecycle and supplemental chunk handling with the Fabric implementation.
- Adapt class names at build time and provide a light-engine interface bridge for Forge's older bundled Mixin.
- Keep the Fabric Preview 2 and server companion artifacts unchanged.
- Preserve plugin-free server fallback; no server reinstall or restart is needed for the client addition.
- Other mods, OptiFine/shaders and the friend's actual platform remain unverified.

## v0.1.0.preview.2 — 2026-11-01 (Preview)

- Enable supplemental height/rendering only after the server advertises the companion channel and completes negotiation.
- Send no YSpan registration or hello on ordinary servers; preserve vanilla rendering methods while inactive.
- Scope negotiation and supplemental state to the connection/world; clear it on disconnect, respawn, unregister, malformed replies, or negotiation timeout.
- Add 20 isolated negotiation checks and packaged-client vanilla/companion/reconnect regression evidence.
- Rename the client artifact/display to YSpan Preview; internal version is `0.1.0-preview.2`.
- Server companion and wire protocol are unchanged. This remains an early preview; broader support is planned.

## v0.1.0-preview.1

Initial experimental Fabric 1.16.5 + Paper 26.3 preview. Historical height-boundary, native interaction and End-portal evidence is retained separately from Preview 2 results.
