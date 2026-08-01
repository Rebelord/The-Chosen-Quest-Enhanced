# Playtest Findings Log

This is the durable index for human playtest evidence. Keep one concise row per
finding, then add a detailed record below when reproduction or balance context matters.

| Finding | Date | Build | Hero / loadout | Seed / route | Level | Equipment / relics | Result | Type | Severity | Reproduces | Backlog | Status |
|---|---|---|---|---|---:|---|---|---|---|---|---|---|
| PT-000 | YYYY-MM-DD | version or commit | Race Class / kit | seed / cautious, exploratory, or spam | 1–3 | weapon; armour; offhand; relic count | win, loss, flee, or stopped | defect, balance, usability, preference | low–critical | 0/1 | TASK-ID | new |
| PT-001 | 2026-07-31 | v0.7.0-beta.1 → v0.7.0-beta.2 | N/A | updater flow | — | N/A | all Phase 1 actions passed | usability / release safety | high | 1/1 | UPDATE-001 | verified |

### PT-001 — Published prerelease updater flow passes

- **Tester / environment:** Project owner; macOS desktop
- **Build / seed:** Retained `v0.7.0-beta.1` checking published `v0.7.0-beta.2`
- **Linked backlog ID:** `UPDATE-001`
- **Classification:** usability observation / release safety
- **Expected:** The older beta discovers beta.2 without blocking launch; manual
  checking, release-note navigation, reminder behavior, and official download
  handoff work without clipping or crashing.
- **Observed:** The update notification appeared automatically on launch. Manual
  Check Updates worked, release/version copy was not clipped, and every tested
  updater action completed successfully.
- **Frequency:** 1/1 full end-to-end run
- **Evidence:** Live published GitHub prerelease plus tester confirmation
- **Triage decision:** accepted
- **Resolution and verification:** Phase 1 is complete and human-verified. Keep
  `UPDATE-001` open for checksum-verified download, assisted installation, and the
  later separately tested Update & Restart workflow.

## Detailed finding template

### PT-000 — Short observable title

- **Tester / environment:**
- **Build / seed:**
- **Hero / route:**
- **Starting and ending resources:**
- **Linked backlog ID:**
- **Classification:** defect / balance observation / usability observation / preference
- **Expected:**
- **Observed:**
- **Steps to reproduce:**
  1. 
  2. 
  3. 
- **Frequency:** 0/3
- **Evidence:** screenshot, save, combat-log excerpt, or notes
- **Triage decision:** accepted / needs more runs / duplicate / declined
- **Resolution and verification:**

## Session summary template

### Session — YYYY-MM-DD — tester — build

- **Environment:**
- **Hero / loadout / seed:**
- **Route:** cautious / exploratory / attack-spam
- **Final state:** level; health/mana; equipment; potions; gold; relics; result; duration
- **Best moments:**
  1. 
  2. 
  3. 
- **Confusing moments:**
  1. 
  2. 
  3. 
- **Most desired change:**
- **Finding IDs created:**
