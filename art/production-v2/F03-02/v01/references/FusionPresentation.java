package dev.wildcraft.client.fuse;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.fuse.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
public final class FusionPresentation{
    public static final KeyMapping FUSE=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildcraft.fuse",InputConstants.Type.KEYBOARD,InputConstants.KEY_V,KeyMapping.Category.register(Wildcraft.id("fusion"))));
    private FusionPresentation(){}
    public static ItemStack display(FusionVisual v){var s=new ItemStack(BuiltInRegistries.ITEM.getValue(v.material()));if(s.isEmpty())s=new ItemStack(Items.BARRIER);s.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,v.glint());return s;}
    public static void initialize(){
        ClientTickEvents.START_CLIENT_TICK.register(c -> {while(FUSE.consumeClick())if(c.player!=null&&c.gui.screen()==null&&ClientPlayNetworking.canSend(FusionIntent.TYPE))ClientPlayNetworking.send(new FusionIntent(c.options.keyShift.isDown()));});
        ItemTooltipCallback.EVENT.register((stack,context,flag,lines) -> {var v=stack.get(FusionContent.VIEW);if(v!=null){lines.add(Component.translatable("fuse.wildcraft.tooltip",display(v).getHoverName(),v.uses()));lines.add(Component.translatable("fuse.wildcraft.kind."+v.kind()));if(v.kind()==FusionRules.INACTIVE)lines.add(Component.literal(v.material().toString()));lines.add(Component.translatable("fuse.wildcraft.controls",FUSE.getTranslatedKeyMessage()));}else if(FusionItems.host(stack))lines.add(Component.translatable("fuse.wildcraft.controls",FUSE.getTranslatedKeyMessage()));});
    }
}
