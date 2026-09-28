package dev.cameron.nightwatch.entity;

import dev.cameron.nightwatch.model.WatcherModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Client renderer for the Watcher, bound to Models' exported {@link WatcherModel}.
 *
 * <p>26.3 renderers build a render state and submit it; the standard living-entity pipeline is
 * inherited from {@link LivingEntityRenderer}, so this class supplies the baked model, a shadow
 * radius, texture and the "seen" signal Models' model uses to freeze its idle animation.
 *
 * <p>No tab-list entry, no name tag is drawn (no override of shouldShowName).
 */
public class WatcherRenderer extends LivingEntityRenderer<WatcherEntity, LivingEntityRenderState, WatcherModel> {
    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath("nightwatch", "textures/entity/watcher.png");
    private static final double SEEN_DOT = 0.9D;

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherModel(context.bakeLayer(WatcherModel.LAYER)), 0.35F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public void extractRenderState(WatcherEntity entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.model.setSeen(isLookedAt(entity));
        this.model.setVanishing(entity.isVanishing()
            ? Math.min(1.0F, entity.vanishTicks() / (float) WatcherEntity.SINK_TICKS)
            : 0.0F);
        feedMotion(entity);
    }

    /** Projects the entity's velocity onto its facing so Models' model can lag parts behind the whole. */
    private void feedMotion(WatcherEntity entity) {
        Vec3 motion = entity.getDeltaMovement();
        if (motion.horizontalDistanceSqr() < 1.0E-6) {
            this.model.setMotion(0.0F, 0.0F);
            return;
        }
        float yaw = entity.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        this.model.setMotion((float) motion.dot(facing), (float) motion.dot(right));
    }

    /** True when the local player's view direction is closely aligned with the direction to the Watcher. */
    private static boolean isLookedAt(WatcherEntity entity) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        Vec3 toEntity = entity.position().subtract(client.player.getEyePosition());
        if (toEntity.lengthSqr() < 1.0E-4) return false;
        return client.player.getViewVector(1.0F).dot(toEntity.normalize()) > SEEN_DOT;
    }
}
