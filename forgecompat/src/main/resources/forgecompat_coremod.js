// ForgeCompat CoreMod - Pure bytecode translator
// Rewrites 1.19.2 Forge mod bytecode directly to 1.20.1.
// No shim classes. No wrappers. Just translation.

var Transformer = Java.type('net.devmaster.forgecompat.transformer.ForgeCompatTransformer');

function initializeCoreMod() {
    return {
        'forgecompat_translator': {
            'target': {
                'type': 'CLASS',
                'names': function(classNames) {
                    var targets = [];
                    for (var i = 0; i < classNames.length; i++) {
                        if (Transformer.shouldTransform(classNames[i])) {
                            targets.push(classNames[i]);
                        }
                    }
                    return targets;
                }
            },
            'transformer': function(classNode) {
                try {
                    return Transformer.transform(classNode.name, classNode);
                } catch (e) {
                    // Don't crash the game — log and pass through
                    print('[ForgeCompat] Error transforming ' + classNode.name + ': ' + e);
                    return classNode;
                }
            }
        }
    };
}
