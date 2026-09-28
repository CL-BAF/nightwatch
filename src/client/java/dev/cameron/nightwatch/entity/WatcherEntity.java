package dev.cameron.nightwatch.entity;

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
    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        // Intentionally empty: Stage 1 Watcher only watches. No goals = no combat, no pathing aggression.
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
