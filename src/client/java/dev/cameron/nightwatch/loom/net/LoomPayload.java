package dev.cameron.nightwatch.loom.net;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Minimal, no-position payload: actions only, decided by LoomSequence on the client.
 * The server computes every coordinate itself from its own anchor store, so there is
 * nothing client-controlled to validate beyond the bounded action int. */
public record LoomPayload(int action) implements CustomPacketPayload {
    public static final int ENTER = 0;
    public static final int EXIT_NORMAL = 1;
    public static final int EXIT_CAUGHT = 2;
    public static final LoomPayload ENTER_ACTION = new LoomPayload(ENTER);
    public static final LoomPayload EXIT_ACTION = new LoomPayload(EXIT_NORMAL);
    public static final LoomPayload EXIT_CAUGHT_ACTION = new LoomPayload(EXIT_CAUGHT);
    public static final CustomPacketPayload.Type<LoomPayload> TYPE =
        new CustomPacketPayload.Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("nightwatch", "loom_move"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, LoomPayload> CODEC =
        net.minecraft.network.codec.StreamCodec.composite(
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, LoomPayload::action, LoomPayload::new);

    @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
