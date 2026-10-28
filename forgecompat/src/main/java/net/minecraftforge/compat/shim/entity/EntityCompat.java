package net.minecraftforge.compat.shim.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Shim for 1.19.2 Entity field access patterns.
 * 
 * In 1.19.2, Entity had public fields:
 *   entity.level (Level field)
 *   entity.isOnGround (boolean field)
 * 
 * In 1.20.1, these became methods:
 *   entity.level() (returns Level)
 *   entity.onGround() (returns boolean)
 * 
 * The CoreMod transformer converts GETFIELD instructions to INVOKEVIRTUAL.
 * This class provides static helper methods for cases where transformation
 * isn't possible (e.g., reflection-based access).
 */
public class EntityCompat {
    
    /**
     * Get the level for an entity (1.19.2 field access pattern).
     * Used as a fallback when ASM transformation isn't possible.
     */
    public static Level getLevel(Entity entity) {
        return entity.level();
    }
    
    /**
     * Check if entity is on ground (1.19.2 field access pattern).
     */
    public static boolean isOnGround(Entity entity) {
        return entity.onGround();
    }
    
    /**
     * 1.19.2: ServerPlayer.getLevel()
     * 1.20.1: ServerPlayer.serverLevel()
     */
    public static ServerLevel getServerLevel(ServerPlayer player) {
        return player.serverLevel();
    }
    
    /**
     * 1.19.2: Entity.outOfWorld()
     * 1.20.1: Entity.onBelowWorld()
     */
    public static boolean isBelowWorld(Entity entity) {
        return entity.onBelowWorld();
    }
    
    /**
     * 1.19.2: Entity.checkOutOfWorld()
     * 1.20.1: Entity.checkBelowWorld()
     */
    public static void checkBelowWorld(Entity entity) {
        entity.checkBelowWorld();
    }
}
