# Instance Zones

Marauroa 1.42 provides a generic runtime foundation for ephemeral zone instances. The engine owns identity, membership and cleanup. Games continue to own concrete map construction, portal routing and game rules.

## Scope

An instance is identified by three logical parts:

1. A base zone id chosen by the game.
2. A stable `instanceId` valid for the lifetime of the logical instance.
3. An `InstanceScope` of type `PLAYER` or `GROUP` with an opaque game supplied key.

Marauroa does not inspect player names, party implementations or map templates.

`InstanceZoneDescriptor` derives a deterministic runtime `IRPZone.ID` from those values. Values are encoded before being placed in the runtime id, so separators or whitespace in game supplied keys cannot cause id collisions.

## Lifecycle

Instances are acquired through `RPWorld.getInstanceZoneManager()`.

```java
IRPZone zone = world.getInstanceZoneManager().acquire(
        "dungeon-template",
        "run-42",
        InstanceScope.group("party-123"),
        "alice",
        factory);
```

The first acquire asks the supplied `InstanceZoneFactory` to build a complete zone whose id equals `descriptor.getRuntimeZoneId()`. Later acquires for the same descriptor reuse that zone. Repeated acquire by the same member is idempotent.

Members are released explicitly:

```java
manager.release(zone.getID(), "alice");
```

A disconnect or timeout safety path can remove one opaque member id from every active instance:

```java
manager.releaseMember("alice");
```

When the final member leaves, the instance is detached from `RPWorld` and `InstanceZoneFactory.destroy(...)` is called.

## Ephemeral persistence model

Instance registry state is memory only in 1.42.

Managed instance zones deliberately bypass normal `IRPZone.onFinish()` during instance cleanup. This is important because the default `MarauroaRPZone.onFinish()` stores zone state in persistence.

`RPWorld.onFinish()` destroys all managed instances before finishing ordinary zones, so active instances are not accidentally persisted during server shutdown.

A game that needs transient cleanup should implement it in `InstanceZoneFactory.destroy(...)`.

After a server restart no instance registry is restored. A game may recreate a logical instance later from the same base id, scope and `instanceId`, but v1 does not restore its previous runtime contents.

## Isolation

Each logical instance is a separate `IRPZone` object with a separate object map and perception state. Two instances may therefore contain entities with the same local semantics without exposing them to one another.

The existing player-specific perception filtering remains available inside each instance when a game also needs per-viewer visibility.

## Safety rules

1. A factory must return the exact runtime zone id requested by the descriptor.
2. Creation is rejected if the derived runtime id is already occupied by a normal zone.
3. A managed instance cannot be removed through normal `RPWorld.removeRPZone()`. It must be released through `InstanceZoneManager` so membership and ephemeral cleanup remain consistent.
4. The manager is designed for the normal RP/game lifecycle thread. Its public lifecycle methods are synchronized so repeated or defensive calls remain deterministic.
5. Marauroa does not route portals itself. Games should resolve the proper descriptor and acquire membership before teleporting an entity to the resulting runtime zone.

## Compatibility

The foundation does not change the wire protocol. Runtime instances use the existing string based `IRPZone.ID` carried by normal perceptions and object ids.

The foundation does not change the persistence format. v1 instance registry state is intentionally not persisted.
