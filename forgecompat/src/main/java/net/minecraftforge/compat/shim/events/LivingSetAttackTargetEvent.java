package net.minecraftforge.compat.shim.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Shim for the 1.19.2 LivingSetAttackTargetEvent.
 * 
 * In 1.19.2: LivingSetAttackTargetEvent had getEntity() and getTarget()
 * In 1.20.1: This was renamed to LivingChangeTargetEvent with getEntity(), getOldTarget(), getNewTarget()
 * 
 * This shim extends the new event to provide the old API.
 * The transformer rewrites:
 *   LivingSetAttackTargetEvent → this class
 *   event.getTarget() → event.getNewTarget()
 */
public class LivingSetAttackTargetEvent extends LivingChangeTargetEvent {
    
    public LivingSetAttackTargetEvent(LivingChangeTargetEvent original) {
        super(original.getEntity(), original.getOldTarget(), original.getNewTarget());
    }
    
    /**
     * 1.19.2 API: getTarget() returns the NEW target (what the entity is now targeting).
     * In 1.20.1, this is getNewTarget().
     */
    @Nullable
    public LivingEntity getTarget() {
        return getNewTarget();
    }
    
    /**
     * 1.19.2 API: getEntityLiving() was an alias for getEntity().
     */
    public LivingEntity getEntityLiving() {
        return getEntity();
    }
}
