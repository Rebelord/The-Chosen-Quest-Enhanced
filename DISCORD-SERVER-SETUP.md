# The Chosen Quest Enhanced — Discord Setup

This is the working blueprint for the project's tester and community server.
Discord is the conversational layer; confirmed work remains tracked in GitHub.

## Server identity

- Name: `The Chosen Quest Enhanced`
- Description: `A compact fantasy RPG rebuilt with tactical combat, finite progression, handcrafted character builds, and community-driven playtesting.`
- Icon: `assets/community/discord-server-icon.png`
- Discord/community banner: `assets/community/The-Chosen-Quest-Banner.png`

Use this wide scene with the four character figures as the selected Discord banner.
The source is 1983×793 (approximately 2.5:1), so Discord may crop the far-left and
far-right scenery in its 16:9 banner placement. The important title and figures are
centered. Retain the original source for release announcements and other wide
promotional placements.

All future Discord banners, announcement cards, tester invitations, and event art
follow `BRAND-VISUAL-SYSTEM.md`. Use the neutral four-class ensemble treatment for
general community material. Class- or race-specific test events may use the matching
`HeroVisualTheme`, but should remain compositions of the same charcoal, parchment,
antique-gold, painterly Art Deco brand system.

## Roles

Create these from highest to lowest:

1. `Developer` — project administration; assign only to project owners.
2. `Moderator` — manage messages, threads, and members without Administrator.
3. `Contributor` — recognized code, design, writing, audio, or art contributors.
4. `Beta Tester` — active playtest participants.
5. `Artist Credit` — credited source artists and asset contributors when present.

Do not give `Beta Tester`, `Contributor`, or `Artist Credit` moderation permissions.

## Channels

### START HERE

- `#welcome-and-rules` — read-only project summary and conduct expectations.
- `#announcements` — read-only builds, test windows, and important changes.
- `#download-and-install` — current GitHub release and novice-friendly setup steps.
- `#known-issues` — current limitations and workarounds.

### THE TAVERN

- `#general` — community discussion.
- `#screenshots-and-clips` — playthrough media.
- `#builds-and-strategies` — race/class builds and combat tactics.
- `#lore-and-ideas` — world, story, enemy, and feature ideas.

### BETA TESTING

- `#testing-instructions` — expected test routes and current priorities.
- `#release-feedback` — discussion around the current build.
- `#balance-discussion` — class, item, economy, and encounter balance.
- `bug-reports` — Forum channel with required tags.

Recommended bug tags:

- `Cannot Progress`
- `Combat`
- `UI / UX`
- `Map / Travel`
- `Inventory / Shops`
- `Audio`
- `Performance`
- `Balance`
- `Fixed Next Build`
- `Needs More Info`

### DEVELOPMENT

Keep this category private to `Developer` and `Moderator`.

- `#bug-triage`
- `#development-notes`
- `#release-planning`

## Tester links

- Feedback: https://forms.gle/GJLCPtzP7LN9PeBeA
- Bug report: https://forms.gle/F8LtxPn5sKbwPAxX9
- Releases: https://github.com/Rebelord/The-Chosen-Quest-Enhanced/releases

## Welcome message

> Welcome to **The Chosen Quest Enhanced**!
>
> This server is the home of our beta-testing community. Start with
> **#download-and-install**, check **#known-issues**, and share your playthroughs in
> **#release-feedback**.
>
> Found a defect? Submit the structured
> [bug report](https://forms.gle/F8LtxPn5sKbwPAxX9). Finished a test run? Send
> [general feedback](https://forms.gle/GJLCPtzP7LN9PeBeA).
>
> Please avoid posting personal information, unreleased private material, or
> unmarked story spoilers. Ask before listing another tester's real name or gamer
> tag in public credits.

## Bug forum guidelines

Use a short, specific title. Include:

1. Game version and operating system.
2. Character race, class, and level.
3. What you were doing.
4. What you expected.
5. What happened instead.
6. Steps that reproduce it.
7. Screenshot or diagnostics, when available.

For structured/private details, use the bug form instead of posting personal
information publicly.

## Automation status

The bug form currently writes to its private Google response Sheet. Automatic
GitHub issue creation is not yet connected. The planned Apps Script must sanitize
public issue content, avoid duplicates, store secrets only in Script Properties,
and write the created issue URL or failure status back to the Sheet.
