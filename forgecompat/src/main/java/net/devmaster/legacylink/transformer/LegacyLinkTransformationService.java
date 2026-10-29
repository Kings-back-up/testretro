package net.devmaster.legacylink.transformer;

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.devmaster.legacylink.LegacyLink;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.util.*;

/**
 * LegacyLink Transformation Service
 * 
 * This service registers the LegacyLinkTransformer with Forge's ModLauncher,
 * allowing bytecode transformation without requiring a CoreMod JS file.
 */
public class LegacyLinkTransformationService implements ITransformationService {
    
    @Override
    public String name() {
        return "legacylink";
    }
    
    @Override
    public void onInitialize(IEnvironment environment) {
        LegacyLink.LOGGER.info("[LegacyLink] Transformation service initializing");
    }
    
    @Override
    public void onLoad(IEnvironment env, Set<String> otherTargets) {
        LegacyLink.LOGGER.info("[LegacyLink] Transformation service loaded");
    }
    
    @Override
    public List<ITransformer<?>> transformers() {
        LegacyLink.LOGGER.info("[LegacyLink] Registering class transformers");
        
        // Return a transformer that applies LegacyLinkTransformer to all classes
        return Collections.singletonList(new ITransformer<ClassNode>() {
            @Override
            public ClassNode transform(ClassNode input, ITransformer.Context context) {
                String className = input.name;
                
                // Check if this class should be transformed
                if (!LegacyLinkTransformer.shouldTransform(className)) {
                    return input;
                }
                
                LegacyLink.LOGGER.debug("[LegacyLink] Transforming class via service: {}", className);
                
                try {
                    return LegacyLinkTransformer.transform(className, input);
                } catch (Exception e) {
                    LegacyLink.LOGGER.error("[LegacyLink] Error transforming class: {}", className, e);
                    return input;
                }
            }
            
            @Override
            public com.google.common.collect.ImmutableSet<String> targets() {
                // Transform all classes - we'll filter in the transform method
                return com.google.common.collect.ImmutableSet.of("*");
            }
        });
    }
}
