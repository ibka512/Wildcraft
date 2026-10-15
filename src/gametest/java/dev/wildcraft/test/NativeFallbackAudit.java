package dev.wildcraft.test;
/** Test-only actual native collector observations, no alternate rendering or ownership. */
public final class NativeFallbackAudit {
 public static net.minecraft.client.renderer.item.ItemStackRenderState expectedBackShield;public static net.minecraft.resources.Identifier expectedArmorTexture;public static int armorModels,lateContinued,lateCancelled;public static boolean enabled;public static int kind,bodies,bowQuads,otherQuads,shieldModels;public static String skin;
 public static void begin(){enabled=true;expectedBackShield=null;expectedArmorTexture=null;kind=bodies=bowQuads=otherQuads=shieldModels=0;armorModels=0;skin=null;}
}
