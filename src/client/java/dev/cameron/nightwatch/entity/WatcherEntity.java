package dev.cameron.nightwatch.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The Watcher: a real, observe-from-distance entity for Stage 1.
 *
 * <p>Deliberately harmless: it registers no goals at all, so vanilla AI gives it no
 * attack, target or movement behaviour. It only watches (its model animates) and can be
 * despawned by the director ("vanish"). It never damages the player and never breaks blocks.
 *
 * <p>Attributes: full health (so it cannot be one-shot accidentally into a fight loop),
 * zero movement speed (stationary), bounded follow range. Persistence is forced off so the
 * entity never survives a save/reload as a permanent stalker.
 */
public class WatcherEntity extends PathfinderMob {
    /** ~600ms sink window (Models' setVanishing takes 0..1 over ~500ms). */
    public static final int SINK_TICKS = 12;

    private static final EntityDataAccessor<Boolean> DATA_VANISHING =
        SynchedEntityData.defineId(WatcherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VANISH_TICKS =
        SynchedEntityData.defineId(WatcherEntity.class, EntityDataSerializers.INT);

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VANISHING, false);
        builder.define(DATA_VANISH_TICKS, 0);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty: Stage 1 Watcher only watches. No goals = no combat, no pathing aggression.
    }

    /** Start the exit staging: the model sinks over SINK_TICKS, then the entity discards itself. */
    public void beginVanish() {
        this.entityData.set(DATA_VANISHING, true);
    }

    public boolean isVanishing() {
        return this.entityData.get(DATA_VANISHING);
    }

    public int vanishTicks() {
        return this.entityData.get(DATA_VANISH_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.isVanishing()) {
            int elapsed = this.vanishTicks() + 1;
            this.entityData.set(DATA_VANISH_TICKS, elapsed);
            if (elapsed >= SINK_TICKS) this.discard();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            .add(Attributes.FOLLOW_RANGE, 32.0D)
            .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return false;
    }
}
