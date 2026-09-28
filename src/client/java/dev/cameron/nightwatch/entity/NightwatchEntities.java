package dev.cameron.nightwatch.entity;

import dev.cameron.nightwatch.model.WatcherModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registration recipe for the Watcher entity on Minecraft 26.3 / Fabric API 0.161.0+26.3.
 *
 * <p>Verified 26.3 API notes (names checked against the mapped client jar, not memory):
 * <ul>
 *   <li>{@code EntityType.Builder.build(ResourceKey<EntityType<?>>)} — it no longer takes a String key.</li>
 *   <li>Key is {@code ResourceKey.create(Registries.ENTITY_TYPE, Identifier)}.</li>
 *   <li>Vanilla registration is {@code Registry.register(BuiltInRegistries.ENTITY_TYPE, Identifier, type)}
 *       — {@code FabricEntityTypeBuilder} no longer exists.</li>
 *   <li>Attributes: {@code FabricDefaultAttributeRegistry.register(type, AttributeSupplier.Builder)}.</li>
 *   <li>Model layer: Fabric's class is now {@code ModelLayerRegistry} (renamed from EntityModelLayerRegistry).</li>
 *   <li>Renderer: {@code EntityRendererRegistry.register(EntityType, EntityRendererProvider)}.</li>
 * </ul>
 *
 * <p>Known warning: {@code EntityRendererRegistry} is marked {@code @Deprecated} in Fabric API
 * 27.0.14+901a437c5d, but it is still the only registration route — vanilla
 * {@code net.minecraft.client.renderer.entity.EntityRenderers} exposes no {@code register} in 26.3
 * (only {@code createEntityRenderers}). Kept deliberately; flagged to Lead/Runtime.
 */
public final class NightwatchEntities {
    private NightwatchEntities() {}

    public static final Identifier WATCHER_ID = Identifier.fromNamespaceAndPath("nightwatch", "watcher");

    public static final EntityType<WatcherEntity> WATCHER = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        WATCHER_ID,
        EntityType.Builder.of(WatcherEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 2.9F)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, WATCHER_ID)));

    /** Common-side registration. Call from the mod initialiser. */
    public static void register() {
        FabricDefaultAttributeRegistry.register(WATCHER, WatcherEntity.createAttributes());
    }

    /** Client-side registration (model layer + renderer). Call from the client initialiser. */
    public static void registerClient() {
        ModelLayerRegistry.registerModelLayer(WatcherModel.LAYER, WatcherModel::createBodyLayer);
        EntityRendererRegistry.register(WATCHER, WatcherRenderer::new);
    }
}
