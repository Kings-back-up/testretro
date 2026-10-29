package net.devmaster.legacylink.translate;

/**
 * Minimal data holder for Material constants.
 * 
 * THIS IS NOT A RE-IMPLEMENTATION OF THE OLD MATERIAL CLASS.
 * 
 * The old Material class had methods like isFlammable(), canBurn(), etc.
 * that mods would call. We DON'T provide those. Instead, the transformer
 * rewrites all calls to those methods into their 1.20.1 equivalents.
 * 
 * This class exists ONLY because sometimes a Material is stored in a variable:
 *   Material m = getMaterial();
 *   Properties p = Properties.of(m);
 * 
 * In that case, the transformer can't inline the flags (it doesn't know
 * which Material at compile time). So it rewrites to:
 *   MaterialData m = getMaterial();  // type rewrite
 *   Properties p = CompatHelpers.of(m);  // method rewrite
 * 
 * And MaterialData is just a bag of the flag values that CompatHelpers reads.
 * 
 * For the common case (Properties.of(Material.WOOD)), the transformer
 * inlines everything and this class is never used at runtime.
 */
public class MaterialData {
    
    // Data fields matching 1.19.2 Material properties
    public final boolean flammable;
    public final boolean liquid;
    public final boolean blocksMotion;
    public final boolean replaceable;
    public final String mapColor;
    public final String instrument;
    public final int pushReaction;
    
    public MaterialData(boolean flammable, boolean liquid, boolean blocksMotion,
                        boolean replaceable, String mapColor, String instrument, int pushReaction) {
        this.flammable = flammable;
        this.liquid = liquid;
        this.blocksMotion = blocksMotion;
        this.replaceable = replaceable;
        this.mapColor = mapColor;
        this.instrument = instrument;
        this.pushReaction = pushReaction;
    }
    
    // Static instances matching the old Material constants.
    // The transformer rewrites GETSTATIC Material.WOOD → GETSTATIC MaterialData.WOOD
    // (just a type change — the data is the same)
    
    public static final MaterialData WOOD = fromFlags("WOOD");
    public static final MaterialData STONE = fromFlags("STONE");
    public static final MaterialData METAL = fromFlags("METAL");
    public static final MaterialData DIRT = fromFlags("DIRT");
    public static final MaterialData PLANT = fromFlags("PLANT");
    public static final MaterialData REPLACEABLE_PLANT = fromFlags("REPLACEABLE_PLANT");
    public static final MaterialData WATER = fromFlags("WATER");
    public static final MaterialData LAVA = fromFlags("LAVA");
    public static final MaterialData SAND = fromFlags("SAND");
    public static final MaterialData GLASS = fromFlags("GLASS");
    public static final MaterialData WOOL = fromFlags("WOOL");
    public static final MaterialData ICE = fromFlags("ICE");
    public static final MaterialData CLAY = fromFlags("CLAY");
    public static final MaterialData GRASS = fromFlags("GRASS");
    public static final MaterialData DECORATION = fromFlags("DECORATION");
    public static final MaterialData FIRE = fromFlags("FIRE");
    public static final MaterialData AIR = fromFlags("AIR");
    public static final MaterialData EXPLOSIVE = fromFlags("EXPLOSIVE");
    public static final MaterialData NETHER_WOOD = fromFlags("NETHER_WOOD");
    public static final MaterialData BAMBOO = fromFlags("BAMBOO");
    public static final MaterialData HEAVY_METAL = fromFlags("HEAVY_METAL");
    public static final MaterialData AMETHYST = fromFlags("AMETHYST");
    public static final MaterialData MOSS = fromFlags("MOSS");
    public static final MaterialData CAKE = fromFlags("CAKE");
    public static final MaterialData WEB = fromFlags("WEB");
    public static final MaterialData SPONGE = fromFlags("SPONGE");
    public static final MaterialData SCULK = fromFlags("SCULK");
    public static final MaterialData FABRIC = fromFlags("FABRIC");
    public static final MaterialData VEGETABLE = fromFlags("VEGETABLE");
    public static final MaterialData PORTAL = fromFlags("PORTAL");
    public static final MaterialData BARRIER = fromFlags("BARRIER");
    public static final MaterialData PISTON = fromFlags("PISTON");
    public static final MaterialData MUD = fromFlags("MUD");
    public static final MaterialData SNOW = fromFlags("SNOW");
    public static final MaterialData BUBBLE_COLUMN = fromFlags("BUBBLE_COLUMN");
    public static final MaterialData SHULKER_SHELL = fromFlags("SHULKER_SHELL");
    public static final MaterialData STRUCTURAL_AIR = fromFlags("STRUCTURAL_AIR");
    public static final MaterialData WATER_PLANT = fromFlags("WATER_PLANT");
    public static final MaterialData NETHER_PLANT = fromFlags("NETHER_PLANT");
    public static final MaterialData CLOTH_DECORATION = fromFlags("CLOTH_DECORATION");
    public static final MaterialData BUILDABLE_GLASS = fromFlags("BUILDABLE_GLASS");
    public static final MaterialData EGG = fromFlags("EGG");
    public static final MaterialData FROGED_ICE = fromFlags("FROGED_ICE");
    public static final MaterialData REPLACEABLE_FIREPROOF_PLANT = fromFlags("REPLACEABLE_FIREPROOF_PLANT");
    
    private static MaterialData fromFlags(String name) {
        TranslationRules.MaterialFlags flags = TranslationRules.MATERIAL_FLAGS.get(name);
        if (flags == null) {
            throw new IllegalArgumentException("Unknown material: " + name);
        }
        return new MaterialData(
            flags.flammable(),
            flags.liquid(),
            flags.blocksMotion(),
            flags.replaceable(),
            flags.mapColor(),
            flags.instrument(),
            flags.pushReaction()
        );
    }
}
