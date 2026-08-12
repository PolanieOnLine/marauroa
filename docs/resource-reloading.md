# Safe resource reload

## Goal

Marauroa should support data reloads without restarting the server while keeping the active world state valid when a changed resource is malformed.

This infrastructure is deliberately generic. It does not know about PolanieOnLine items, creatures, NPC templates or balance rules. Games register their own reloadable components and decide how resource data is parsed and represented.

## Reload lifecycle

A reload has two separate phases.

### 1. Prepare before the RP safe-point apply

`ResourceReloadService.requestReload(id)` runs synchronously on the requesting thread:

1. find the registered `ReloadableResource`,
2. load a candidate state through the configured `ResourceProvider`,
3. validate the complete candidate,
4. queue it only when loading and validation both succeed.

`load()` and `validate()` must not mutate the active component state. If either phase fails, the active state stays untouched.

The service deliberately does not create a hidden worker thread. A transport or administration adapter that may perform expensive I/O or parsing should call `requestReload()` from its own control/worker thread, not from RP turn processing. This keeps threading and lifecycle ownership explicit.

### 2. Apply at the RP safe point

`RPServerManager` calls `processPendingReloads()` after `world.nextTurn()` and before the next `beginTurn()`.

At that point no parsing or resource I/O is performed. The service only calls `ReloadableResource.apply(candidate)` for candidates that were already validated.

`apply()` should therefore be a small atomic replacement, for example replacing a map or immutable repository snapshot reference. It should not perform file I/O, parsing or expensive validation.

## Registration

Each component has a stable id:

```java
public final class ItemDefinitions implements ReloadableResource<ItemDefinitions.State> {
	@Override
	public String getId() {
		return "item-definitions";
	}

	@Override
	public State load(ResourceProvider provider) throws Exception {
		// Parse all source data into a detached State instance.
	}

	@Override
	public void validate(State candidate) throws Exception {
		// Check required fields, references, duplicates and invariants.
	}

	@Override
	public void apply(State candidate) {
		// Atomic state replacement only.
	}
}
```

Registration and reload requests use the service:

```java
ResourceReloadService reloads = ResourceReloadService.getInstance();
reloads.register(itemDefinitions);
reloads.requestReload("item-definitions");
```

The server configures a `PersistenceResourceProvider`, so normal resource paths use the same `basedir` and `relativeToHome` semantics as other Marauroa files. Alternative providers can be installed with `setResourceProvider()`.

## Repeated requests

Only the newest successfully prepared candidate for a resource id is kept before the next safe point.

If a valid candidate is waiting and a later reload attempt is invalid, the invalid attempt does not remove the valid candidate. The previously validated candidate is still applied.

Unregistering a resource discards its pending candidate.

## Runtime metrics

`ResourceReloadService.getMetricsSnapshot()` returns an immutable, process-local `ResourceReloadMetricsSnapshot` for observability tooling.

The snapshot reports:

- current registered resource and pending candidate counts,
- reload request and unknown-id counts,
- successful and failed load and validation phases,
- successfully prepared candidates and candidates coalesced by a newer valid request,
- prepared or pending candidates rejected because their registration became stale,
- successful and failed safe-point apply operations,
- total and maximum duration of load, validation and apply work.

Load and validation durations describe work performed on the requesting thread. Apply duration describes only the safe-point state swap on the RP thread. The slow-turn diagnostics may include these process-local counters as context, while the named `resourceReload` turn phase remains the measurement of the actual reload cost paid by that RP turn.

Timing is collected only when reload work actually occurs. A server with no reload requests does not gain additional `System.nanoTime()` calls from these metrics on normal turns. Creating a metrics snapshot is also explicit and is not performed on every turn.

## Security boundary

External admin tooling should request a registered resource **id**, not accept an arbitrary filesystem path from the caller. `getRegisteredResourceIds()` provides the known ids for future CLI, HTTP or administration adapters.

The transport that triggers reloads is intentionally outside this change.

## Non-goals

This mechanism does not:

- hot reload Java classes or class loaders,
- define PolanieOnLine-specific item, creature or NPC semantics,
- add a filesystem watcher,
- add REST, WebSocket or console administration endpoints,
- create an internal background executor for resource parsing,
- change persistence formats,
- change the network protocol.

## Compatibility

Games that never register a `ReloadableResource` keep their existing behaviour. The normal turn loop performs only an empty queue check at the reload safe point.

The metrics are runtime diagnostics only. They are not serialized, do not alter resource data, and reset when the process restarts.
