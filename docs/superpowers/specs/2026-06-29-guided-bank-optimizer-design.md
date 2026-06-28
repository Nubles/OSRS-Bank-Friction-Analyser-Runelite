# Guided Bank Optimizer Design

## Goal

Enhance Bank Friction Analyser from a recommendation-only side panel into a guided, rules-safe bank optimization assistant. The plugin will remain observational and local-only: it may observe RuneLite client state and display guidance, but it must never click, withdraw, deposit, move, tag, search, reorder, consume menu actions, or perform bank organization for the player.

## Rule-Safe Boundaries

The plugin may:

- Observe bank open/close state through RuneLite widgets and events.
- Record local menu-click signals that the player already performed.
- Read search text state exposed by the client.
- Resolve item names and metadata through RuneLite API services.
- Display recommendations in the side panel.
- Optionally highlight relevant bank items or UI regions as passive visual guidance.
- Store analysis history in RuneLite config storage on the local client.

The plugin must not:

- Execute player actions.
- Modify menu entries to make actions easier or different.
- Insert text into the bank search box.
- Move, withdraw, deposit, tag, or rearrange items.
- Use reflection, native code, subprocesses, runtime-downloaded code, or external services.
- Send bank behavior data outside the local client.

## Architecture

`BankFrictionPlugin` remains the RuneLite integration layer. It owns dependency injection, subscriptions, toolbar setup, persistence timing, and the safe conversion of RuneLite client events into domain-level observations.

`BankFrictionAnalyser` remains pure Java analysis logic. It should not depend on RuneLite classes. This keeps the recommendation engine easy to test and lets API-facing changes stay in the plugin layer.

`BankFrictionSnapshot` remains the stored history model, but it can gain versioned fields if needed for richer sessions. History should remain bounded so config storage does not grow without limit.

`BankFrictionPanel` becomes a clearer recommendation dashboard. It should show each recommendation title, reason, confidence, evidence count, and affected items. It should also expose safe local actions such as clearing history or refreshing recommendations.

A new passive overlay component can be added for optional item highlighting. The overlay should render only when the bank is open, highlighting is enabled, and the selected recommendation has item ids that can be matched to visible bank widgets.

## RuneLite API Usage

Session detection should become more accurate by tracking bank widget availability and bank interface lifecycle instead of relying only on the first recorded item interaction. The plugin can still fall back to game-tick polling because the current `isBankOpen()` check is simple and reliable.

Item naming should prefer API-resolved item composition names where practical, with cleaned menu targets as a fallback. This should reduce noisy names caused by tags, colors, or menu formatting.

Search tracking should keep using `VarClientStr.INPUT_TEXT`, but searches should be recorded only when they are stable enough to represent intent. For example, the plugin can record the final non-empty search when the search changes, the bank closes, or the session idles out.

Passive highlighting should use RuneLite overlay APIs and visible bank item widgets. Highlighting must be visual only and should never affect menus or input.

## Recommendation Improvements

The first implementation should focus on higher-quality versions of the current recommendation types:

- Frequent loadout groups: identify item sets repeatedly withdrawn in the same session.
- Repeated searches: show repeated search terms with count and recentness.
- Slow loadout assembly: use better session start/end times to measure time spent preparing.
- Near duplicates: improve base-name grouping for potions, charged items, and equipment variants.
- Manual repositioning: keep tracking explicit player reposition menu actions.
- Tab switching: retain tab-switch friction signals, but present them as softer evidence.

New recommendation types can wait until the core signal is cleaner.

## User Experience

The side panel should stay compact and familiar to RuneLite users. Each recommendation card should answer:

- What should I consider changing?
- Why is the plugin suggesting it?
- How much evidence supports it?
- Which items are involved?

If highlighting is enabled, selecting or focusing a recommendation should highlight related visible bank items. If none of the items are currently visible, the panel should say that the recommendation items are not visible in the current bank view rather than implying something is broken.

Configuration should include:

- Analyse bank behavior.
- Show passive bank highlights.
- Maximum retained sessions.
- Clear local history action or equivalent panel control.

## Data Flow

1. RuneLite emits a bank, menu, var-client-string, or tick signal.
2. `BankFrictionPlugin` checks that data collection is enabled and the bank context is valid.
3. The plugin converts the RuneLite signal into a small domain observation.
4. `BankFrictionAnalyser` records the observation into the current session or search history.
5. The plugin periodically persists the bounded snapshot through `ConfigManager`.
6. The analyser builds recommendations from the snapshot.
7. The panel displays recommendations, and the optional overlay highlights visible items for the selected recommendation.

## Error Handling And Privacy

Malformed stored JSON should continue to reset to an empty snapshot rather than breaking startup.

Missing item metadata, missing widgets, or hidden bank views should degrade gracefully to fallback names and no overlay rendering.

All history remains local. The clear-history action should remove the stored snapshot and refresh the panel immediately.

## Testing

Add unit tests for `BankFrictionAnalyser` before changing recommendation behavior. Tests should cover:

- Frequent loadout detection.
- Repeated search normalization.
- Slow loadout timing.
- Near-duplicate grouping.
- History trimming.
- Session start/end behavior.

Plugin-layer tests can be lighter because RuneLite client state is harder to unit test directly. Keep most logic in pure Java classes so the important behavior remains testable without a running client.

## Rollout Plan

Implement in small steps:

1. Restore analyser unit tests and add coverage for existing behavior.
2. Improve session lifecycle and search finalization.
3. Improve item naming and recommendation evidence display.
4. Add clear-history and history limit controls.
5. Add optional passive highlighting after the analysis and panel behavior are stable.

This order keeps the safest and most measurable improvements first, then adds the more visible overlay feature once the underlying recommendations are trustworthy.
