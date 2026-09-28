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
