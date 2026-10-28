package net.minecraftforge.compat.shim.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Shim for the 1.19.2 SimpleChannel network API.
 * 
 * In 1.19.2:
 *   channel.registerMessage(id, MsgClass, encoder, decoder, handler, Optional<NetworkDirection>)
 * 
 * In 1.20.1:
 *   channel.messageBuilder(MsgClass, id, direction)
 *     .encoder(encoder).decoder(decoder)
 *     .consumerMainThread(handler)
 *     .register()
 * 
 * This shim provides the old registerMessage API that internally uses the new builder.
 */
public class ShimSimpleChannel {
    
    private final SimpleChannel internal;
    private int nextId = 0;
    
    public ShimSimpleChannel(ResourceLocation channelName) {
        this.internal = NetworkRegistry.newSimpleChannel(
            channelName,
            () -> "1.0.0",
            version -> true,
            version -> true
        );
    }
    
    /**
     * 1.19.2 API: registerMessage with a single consumer.
     * 
     * The consumer in 1.19.2 handled both network thread and main thread work.
     * In 1.20.1, these are split into consumerNetworkThread and consumerMainThread.
     * 
     * We map the old single consumer to consumerMainThread (most common use case).
     */
    public <MSG> void registerMessage(
            int id,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler,
            Optional<NetworkDirection> direction) {
        
        // Build using 1.20.1 API, wrapping the old handler
        var builder = internal.messageBuilder(messageType, id);
        
        if (direction.isPresent()) {
            builder = internal.messageBuilder(messageType, id, direction.get());
        }
        
        builder.encoder(encoder)
             .decoder(decoder)
             .consumerMainThread(handler)  // Old API was main-thread by default
             .register();
    }
    
    /**
     * Convenience overload without direction.
     */
    public <MSG> void registerMessage(
            int id,
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        registerMessage(id, messageType, encoder, decoder, handler, Optional.empty());
    }
    
    /**
     * Auto-incrementing ID version (common 1.19.2 pattern).
     */
    public <MSG> void registerMessage(
            Class<MSG> messageType,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        registerMessage(nextId++, messageType, encoder, decoder, handler);
    }
    
    /**
     * Get the underlying SimpleChannel.
     */
    public SimpleChannel getInternal() {
        return internal;
    }
    
    /**
     * Send to server (1.19.2 API).
     */
    public <MSG> void sendToServer(MSG message) {
        internal.sendToServer(message);
    }
    
    /**
     * Send to player (1.19.2 API).
     */
    public <MSG> void sendTo(MSG message, net.minecraft.server.level.ServerPlayer player, 
                             NetworkDirection direction) {
        internal.sendTo(message, player.connection.connection, direction);
    }
}
