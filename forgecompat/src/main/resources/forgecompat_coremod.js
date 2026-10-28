// ForgeCompat CoreMod Transformer
// This JavaScript file is loaded by Forge's CoreMod system and bridges
// to our Java ForgeCompatTransformer class for actual bytecode manipulation.
//
// Location: src/main/resources/forgecompat_coremod.js
// Referenced in: META-INF/mods.toml

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var ForgeCompatTransformer = Java.type('net.minecraftforge.compat.transformer.ForgeCompatTransformer');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

function initializeCoreMod() {
    return {
        'forgecompat_transformer': {
            'target': {
                'type': 'CLASS',
                'names': getTargetClasses
            },
            'transformer': function(classNode) {
                try {
                    return ForgeCompatTransformer.transform(
                        classNode.name, 
                        classNode
                    );
                } catch (e) {
                    // Log but don't crash - let the game continue
                    print('[ForgeCompat] Error transforming ' + classNode.name + ': ' + e);
                    return classNode;
                }
            }
        }
    };
}

/**
 * Determine which classes should be transformed.
 * We target classes that look like they're from mods (not vanilla/forge).
 * The ForgeCompatTransformer.shouldTransform() method does the final check.
 */
function getTargetClasses(classNames) {
    var targets = [];
    for (var i = 0; i < classNames.length; i++) {
        var name = classNames[i];
        if (ForgeCompatTransformer.shouldTransform(name)) {
            targets.push(name);
        }
    }
    return targets;
}
