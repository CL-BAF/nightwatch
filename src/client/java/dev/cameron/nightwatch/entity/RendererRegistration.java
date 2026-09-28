package dev.cameron.nightwatch.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Single migration point for client entity-renderer registration (Lead ruling: isolate the
 * deprecated API in exactly one class).
 *
 * <p>WARNING: {@code EntityRendererRegistry} is {@code @Deprecated} in Fabric API
 * 27.0.14+901a437c5d, but it is the only route on Minecraft 26.3 — the vanilla
 * {@code net.minecraft.client.renderer.entity.EntityRenderers} exposes no {@code register(...)}
 * (only {@code createEntityRenderers}). When Fabric ships a replacement or we bump the API,
 * change ONLY this class; no other file imports the deprecated symbol.
 */
public final class RendererRegistration {
    private RendererRegistration() {}

    public static void register() {
        EntityRendererRegistry.register(NightwatchEntities.WATCHER, WatcherRenderer::new);
    }
}
