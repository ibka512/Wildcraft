package dev.wildcraft.fuse;
import dev.wildcraft.Wildcraft;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
public final class FusionCombat{
    public static final AttachmentType<FusionVisual> ARROW_VIEW=AttachmentRegistry.create(Wildcraft.id("fusion_arrow_view"),b -> b.syncWith(FusionVisual.STREAM_CODEC,AttachmentSyncPredicate.all()));
    private FusionCombat(){}
    public static void initialize(){
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target,source,base,taken,blocked) -> {
            if(taken>0&&source.is(DamageTypes.PLAYER_ATTACK)&&source.getDirectEntity() instanceof Player p&&source.getEntity()==p){var host=p.getMainHandItem();if(!host.is(Items.SHIELD)&&!FusionRules.arrow(host))hit(host,target,source);}
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity,level) -> {if(entity instanceof AbstractArrow arrow){var v=arrow.getPickupItemStackOrigin().get(FusionContent.VIEW);if(v!=null)arrow.setAttached(ARROW_VIEW,v);}});
    }
    public static java.util.Optional<ItemStack> material(ItemStack host,ServerLevel level){var d=host.get(FusionContent.DATA);return d==null?java.util.Optional.empty():d.material(level.registryAccess());}
    public static float bonus(ItemStack host,ServerLevel level){if(!host.is(net.minecraft.tags.ItemTags.SWORDS)&&!host.is(net.minecraft.tags.ItemTags.AXES))return 0;return material(host,level).map(m -> (float)FusionRules.bonus(FusionRules.kind(m))).orElse(0f);}
    public static void hit(ItemStack host,LivingEntity target,DamageSource source){
        if(!(target.level() instanceof ServerLevel level))return;var m=material(host,level);if(m.isEmpty())return;effect(FusionRules.kind(m.get()),target,source);use(host,level);
    }
    public static void shield(ItemStack shield,LivingEntity defender,DamageSource source){
        if(!(defender.level() instanceof ServerLevel level)||!shield.is(Items.SHIELD))return;var m=material(shield,level);if(m.isEmpty())return;
        if(source.getDirectEntity() instanceof LivingEntity attacker&&attacker!=defender&&(!(defender instanceof Player p&&attacker instanceof Player q)||p.canHarmPlayer(q))){
            int kind=FusionRules.kind(m.get());if(kind==FusionRules.GENERIC||kind==FusionRules.ROCK||kind==FusionRules.METAL)attacker.knockback(.15+.1*FusionRules.bonus(kind),defender.getX()-attacker.getX(),defender.getZ()-attacker.getZ(),source,0,true);else effect(kind,attacker,source);
        }use(shield,level);
    }
    public static void effect(int kind,LivingEntity target,DamageSource source){
        if(!target.isAlive())return;
        if(kind==FusionRules.FIRE)target.igniteForSeconds(3);
        else if(kind==FusionRules.ICE)target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,60,0),source.getEntity());
        else if(kind==FusionRules.ELASTIC){var from=source.getSourcePosition();if(from!=null)target.knockback(.65,from.x-target.getX(),from.z-target.getZ(),source,0,true);}
    }
    public static void use(ItemStack host,ServerLevel level){var d=host.get(FusionContent.DATA);if(d==null||d.isProjection())return;if(d.remaining()==1){host.remove(FusionContent.DATA);host.remove(FusionContent.VIEW);}else{d=d.used();host.set(FusionContent.DATA,d);var m=d.material(level.registryAccess());if(m.isPresent())host.set(FusionContent.VIEW,new FusionVisual(BuiltInRegistries.ITEM.getKey(m.get().getItem()),FusionRules.kind(m.get()),d.remaining(),m.get().hasFoil()));}}
    public static void clearArrow(AbstractArrow arrow){var s=arrow.getPickupItemStackOrigin();s.remove(FusionContent.DATA);s.remove(FusionContent.VIEW);arrow.removeAttached(ARROW_VIEW);}
}
