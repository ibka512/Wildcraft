package dev.wildcraft.fuse;

import dev.wildcraft.Wildcraft;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;

/** Re-derive public views after restart or registry/tag changes; never alter the full retained material. */
public final class FusionViews {
    private FusionViews(){}
    public static void refresh(ItemStack host, HolderLookup.Provider registries){
        var d=host.get(FusionContent.DATA);if(d==null||d.isProjection())return;
        var material=d.material(registries);
        var id=Identifier.tryParse(d.rawMaterial().getStringOr("id",""));
        var view=material.map(m -> new FusionVisual(BuiltInRegistries.ITEM.getKey(m.getItem()),FusionRules.kind(m),d.remaining(),m.hasFoil())).orElseGet(() -> new FusionVisual(id==null?Wildcraft.id("unavailable_material"):id,FusionRules.INACTIVE,d.remaining(),false));
        if(!view.equals(host.get(FusionContent.VIEW)))host.set(FusionContent.VIEW,view);
    }
    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(server.overworld().getGameTime()%20!=0)return;
            for(var p:server.getPlayerList().getPlayers()){
                for(int n=0;n<p.getInventory().getContainerSize();n++)refresh(p.getInventory().getItem(n),p.registryAccess());
                for(var slot:p.containerMenu.slots)refresh(slot.getItem(),p.registryAccess());
                refresh(p.containerMenu.getCarried(),p.registryAccess());
            }
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity,level) -> {
            if(entity instanceof ItemEntity item)refresh(item.getItem(),level.registryAccess());
            else if(entity instanceof AbstractArrow arrow){var item=arrow.getPickupItemStackOrigin();refresh(item,level.registryAccess());var v=item.get(FusionContent.VIEW);if(v!=null)arrow.setAttached(FusionCombat.ARROW_VIEW,v);}
        });
    }
}
