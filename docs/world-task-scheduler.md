# World Task Scheduler

Marauroa 1.42 provides a generic scheduler for small world mutations that must execute on the RP thread at a deterministic safe turn boundary.

## Safe boundary

`RPServerManager` processes scheduled world tasks after `world.nextTurn()` and resource reload apply, before the next `ruleProcessor.beginTurn()`.

Tasks must remain small. They must not perform blocking I/O, parse large resources or run expensive validation. Heavy preparation belongs on another thread and the scheduler should receive only the final world mutation.

## Scheduling

```java
WorldTaskScheduler scheduler = RPWorld.get().getWorldTaskScheduler();

scheduler.scheduleNextTurn(
        WorldTaskOwner.zone("0_semos_city"),
        task);

scheduler.scheduleInTurns(
        WorldTaskOwner.entity("12345"),
        20,
        task);
```

A delay of one means the next safe boundary. Zero and negative delays are rejected. A task scheduled from another task cannot execute reentrantly on the same boundary.

## Owners and cancellation

Tasks have an opaque lifecycle owner of type:

* `GLOBAL`
* `ZONE`
* `ENTITY`
* `INSTANCE`

One task can be cancelled through its `WorldTaskHandle`. A successful cancellation guarantees that the callback cannot begin afterwards. If the task is still queued, it is removed from its due-turn bucket immediately.

All tasks belonging to a lifecycle can be cancelled together:

```java
scheduler.cancelOwner(WorldTaskOwner.zone(zoneId));
```

Owner cancellation invalidates tasks that were already detached into the current execution snapshot but have not started yet. A later schedule for the same owner creates a fresh lifecycle epoch.

`ZONE` and `INSTANCE` owners are integrated with engine lifecycle:

* `RPWorld.removeRPZone(...)` cancels `WorldTaskOwner.zone(zoneId)` before the zone is detached and finished.
* `InstanceZoneManager` cancels both `WorldTaskOwner.instance(runtimeZoneId)` and `WorldTaskOwner.zone(runtimeZoneId)` before an ephemeral instance is detached and destroyed. This also protects generic zone scoped work scheduled for an Instance Zone.

`ENTITY` and `GLOBAL` owners remain explicit. Marauroa does not infer an entity task key from an `RPObject.ID`, because games may choose a different stable lifecycle key. Code that owns such tasks must call `cancelOwner(...)` when that lifecycle ends.

## Failure isolation

An exception thrown by one task is logged with owner, sequence and scheduler turn. Other due tasks continue to execute.

## Persistence

The scheduler is intentionally memory only. Scheduled callbacks do not survive a server restart. Games must persist business state separately if a future action must be reconstructed after restart.

No persistence format or wire protocol is changed by the scheduler.

## Metrics

`getCurrentTurn()` and `getPendingTaskCount()` are deliberately exposed as low-cost inputs for the Observability layer. They do not alter scheduling semantics.
