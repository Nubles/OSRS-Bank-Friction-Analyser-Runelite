# Bank Friction Analyser

A RuneLite external plugin that studies how the player actually uses their bank and turns repeated friction into organization suggestions.

## What it recommends

- Items frequently withdrawn together as a loadout.
- Searches repeated across bank sessions.
- Loadouts that take unusually long to assemble.
- Near-duplicate potions or equipment variants used close together.
- Items that appear to be manually repositioned often.
- Repeated bank-tab switching while preparing.

Example recommendation:

> Move these items together; you withdraw them as one loadout 83% of the time.

## RuneLite and Jagex rule posture

This plugin is intentionally observational only.

It does not:

- Move, withdraw, deposit, tag, or click anything for the player.
- Modify menu entries or consume menu actions.
- Send chat messages or insert player input.
- Use reflection, native code, subprocesses, runtime-downloaded code, or external services.
- Send bank behavior data anywhere. Stored history stays in RuneLite config storage on the local client.

The recommendations are displayed in a standard RuneLite side panel. The player remains responsible for any bank organization changes.

## Development

Run the unit tests:

```bash
gradle test
```

Run in RuneLite developer mode from an IDE by launching:

```text
com.bankfriction.BankFrictionPluginRunner
```

## Notes

RuneLite exposes menu interactions reliably, but exact bank search text and drag semantics can vary by client UI path. The analyser supports exact search terms and manual reposition events; the plugin records conservative menu-action signals rather than using private client internals.