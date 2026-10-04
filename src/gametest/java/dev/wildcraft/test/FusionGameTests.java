package dev.wildcraft.test;

import dev.wildcraft.fuse.*;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.HashOps;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

public final class FusionGameTests {
    @GameTest public void exactHostMaterialAndReadCopies(GameTestHelper h){
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.setDamageValue(123);tool.set(DataComponents.CUSTOM_NAME,Component.literal("private material"));
        var box=new ItemStack(Items.SHULKER_BOX);box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(tool.copy(),new ItemStack(Items.DIAMOND,3))));
        for(var material:List.of(new ItemStack(Items.COBBLESTONE,3),tool,box)){
            var expected=material.copyWithCount(1);int count=material.getCount();var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(17);sword.set(DataComponents.CUSTOM_NAME,Component.literal("host"));
            h.assertTrue(FusionItems.attach(sword,material,h.getLevel().registryAccess()),"Lossless bounded material accepted");h.assertTrue(material.getCount()==count-1,"One material consumed");
            var d=sword.get(FusionContent.DATA);h.assertTrue(ItemStack.matches(expected,d.material(h.getLevel().registryAccess()).orElseThrow()),"Complete exact material, including nested inventory");
            var copy=d.rawMaterial();copy.putString("id","minecraft:dirt");h.assertTrue(d.material(h.getLevel().registryAccess()).orElseThrow().is(expected.getItem()),"Read cannot mutate retained raw record");
            var ops=RegistryOps.create(NbtOps.INSTANCE,h.getLevel().registryAccess());var restored=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,sword).getOrThrow()).getOrThrow();h.assertTrue(ItemStack.matches(sword,restored)&&restored.getDamageValue()==17,"Host name, damage and complete component value survive codec");
            h.assertTrue(!FusionItems.attach(sword,new ItemStack(Items.STONE),h.getLevel().registryAccess()),"Repeated fuse has no side effects");
        }h.succeed();
    }
    @GameTest public void unknownIdAndUnknownComponentKeepRaw(GameTestHelper h){
        var original=FusionData.capture(new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess()).getOrThrow();var t=original.tag();var raw=t.getCompoundOrEmpty("material");raw.putString("id","missing-mod:material");
        var d=FusionData.CODEC.parse(NbtOps.INSTANCE,t).getOrThrow();h.assertTrue(d.material(h.getLevel().registryAccess()).isEmpty()&&d.rawMaterial().getStringOr("id","").equals("missing-mod:material"),"Unavailable material retained, no substitute item");
        var saved=FusionData.CODEC.encodeStart(NbtOps.INSTANCE,d).getOrThrow();h.assertTrue(saved.equals(t),"Unknown raw ID roundtrips unchanged");
        t=original.tag();var components=new CompoundTag();components.putString("missing-mod:secret","retained");t.getCompoundOrEmpty("material").put("components",components);d=FusionData.CODEC.parse(NbtOps.INSTANCE,t).getOrThrow();h.assertTrue(d.material(h.getLevel().registryAccess()).isEmpty()&&FusionData.CODEC.encodeStart(NbtOps.INSTANCE,d).getOrThrow().equals(t),"Unknown component cannot partially decode into a fake recoverable item");var host=new ItemStack(Items.IRON_SWORD);host.set(FusionContent.DATA,d);host.set(FusionContent.VIEW,new FusionVisual(net.minecraft.resources.Identifier.withDefaultNamespace("cobblestone"),2,32,true));FusionViews.refresh(host,h.getLevel().registryAccess());h.assertTrue(host.get(FusionContent.DATA).equals(d)&&host.get(FusionContent.VIEW).kind()==FusionRules.INACTIVE&&!host.get(FusionContent.VIEW).glint()&&FusionCombat.bonus(host,h.getLevel())==0&&FusionItems.splitMaterial(host,h.getLevel().registryAccess()).isEmpty(),"Unavailable component disables public effects and recovery, keeping exact raw data");h.succeed();
    }
    @GameTest public void boundsNestedAndFailuresNeverConsume(GameTestHelper h){
        var sword=new ItemStack(Items.IRON_SWORD);var big=new ItemStack(Items.PAPER,2);var custom=new CompoundTag();custom.putString("secret","x".repeat(9000));big.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(custom));
        h.assertTrue(!FusionItems.attach(sword,big,h.getLevel().registryAccess())&&!sword.has(FusionContent.DATA)&&big.getCount()==2,"Over budget rejection is atomic");
        var nested=new ItemStack(Items.SHULKER_BOX);var inside=new ItemStack(Items.IRON_SWORD);FusionItems.attach(inside,new ItemStack(Items.STONE),h.getLevel().registryAccess());nested.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(inside)));
        h.assertTrue(!FusionItems.attach(sword,nested,h.getLevel().registryAccess())&&nested.getCount()==1,"Nested fused containers rejected before debit");
        h.assertTrue(!FusionItems.attach(sword,sword,h.getLevel().registryAccess())&&!FusionItems.attach(new ItemStack(Items.BOW),new ItemStack(Items.STONE),h.getLevel().registryAccess()),"Identity and unsupported host cannot consume");
        var t=FusionData.capture(new ItemStack(Items.STONE),h.getLevel().registryAccess()).getOrThrow().tag();t.putInt("remaining",33);h.assertTrue(FusionData.CODEC.parse(NbtOps.INSTANCE,t).error().isPresent(),"Charges bounded");t.putInt("remaining",1);t.putInt("schema",2);h.assertTrue(FusionData.CODEC.parse(NbtOps.INSTANCE,t).error().isPresent(),"Unknown format refuses reinterpretation");h.succeed();
    }
    @GameTest public void publicProjectionHashesWithoutPrivatePayload(GameTestHelper h){
        var secret=new ItemStack(Items.SHULKER_BOX);secret.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,17))));var d=FusionData.capture(secret,h.getLevel().registryAccess()).getOrThrow();
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());FusionContent.DATA.streamCodec().encode(b,d);h.assertTrue(b.readableBytes()==4,"Four-byte native hash only, no material payload");var projection=FusionContent.DATA.streamCodec().decode(b);b.release();
        h.assertTrue(projection.isProjection()&&projection.rawMaterial().isEmpty()&&projection.material(h.getLevel().registryAccess()).isEmpty(),"Projection is not a material restoration capability");
        h.assertTrue(FusionData.CODEC.encodeStart(HashOps.CRC32C_INSTANCE,d).getOrThrow().equals(FusionData.CODEC.encodeStart(HashOps.CRC32C_INSTANCE,projection).getOrThrow()),"Native container hash equal on both sides without private data");
        h.assertTrue(FusionData.CODEC.encodeStart(NbtOps.INSTANCE,projection).error().isPresent(),"Client projection cannot silently save as a full material");var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.STONE),h.getLevel().registryAccess());var used=sword.get(FusionContent.DATA).used();sword.set(FusionContent.DATA,used);h.assertTrue(FusionItems.splitMaterial(sword,h.getLevel().registryAccess()).orElseThrow().get(FusionContent.WEAR)==31,"Splitting preserves actual remaining uses");h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer survival(GameTestHelper h){var p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());h.setBlock(new net.minecraft.core.BlockPos(1,0,1),net.minecraft.world.level.block.Blocks.STONE);p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5,1,1.5)));return p;}
    @GameTest public void actualHandsAtomicActionsAndFullSplit(GameTestHelper h){
        var p=survival(h);var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(21);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND,2));
        h.assertTrue(FusionActions.receive(p,new FusionIntent(false)),"Live owned hands fuse via intent");h.assertTrue(!FusionActions.receive(p,new FusionIntent(true))&&p.getOffhandItem().getCount()==1,"Same-tick replay cannot immediately undo or double consume");
        FusionCombat.use(sword,h.getLevel());for(int n=0;n<36;n++)if(n!=p.getInventory().getSelectedSlot())p.getInventory().setItem(n,new ItemStack(Items.STONE,64));
        h.assertTrue(!FusionActions.split(p)&&sword.has(FusionContent.DATA),"Full inventory and occupied offhand leave fusion untouched");p.getInventory().setItem(5,ItemStack.EMPTY);h.assertTrue(FusionActions.split(p)&&sword.getDamageValue()==21&&!sword.has(FusionContent.DATA)&&p.getInventory().getItem(5).get(FusionContent.WEAR)==31,"One recovered worn material, host damage preserved");
        h.assertTrue(!FusionActions.split(p),"Second split produces nothing");p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);h.assertTrue(!FusionActions.fuse(p),"No creative material cloning entry");p.discard();h.succeed();
    }
    @GameTest public void arrowsSplitOneHostAndNeverFreeAmmo(GameTestHelper h){
        var p=survival(h);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.ARROW,4));p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.BLAZE_POWDER,2));
        h.assertTrue(FusionActions.fuse(p)&&p.getMainHandItem().getCount()==1&&p.getInventory().getItem(1).getCount()==3,"One real fused arrow, other arrows still owned");
        var fused=p.getMainHandItem().copy();fused.setCount(2);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,fused);h.assertTrue(FusionActions.split(p)&&p.getMainHandItem().getCount()==1&&!p.getMainHandItem().has(FusionContent.DATA)&&p.getInventory().getItem(2).has(FusionContent.DATA),"Splitting a merged arrow stack leaves other fusion intact");
        var ammo=p.getInventory().getItem(2).copy();ammo.setCount(2);var weapon=new ItemStack(Items.BOW);net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(weapon,e -> e.set(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.INFINITY),1));
        var actual=dev.wildcraft.mixin.ProjectileWeaponItemAccess.wildcraft$useAmmo(weapon,ammo,p,false);h.assertTrue(actual.has(FusionContent.DATA)&&ammo.getCount()==1,"Infinity still consumes one actual fused arrow");
        var virtual=dev.wildcraft.mixin.ProjectileWeaponItemAccess.wildcraft$useAmmo(new ItemStack(Items.CROSSBOW),ammo,p,true);h.assertTrue(!virtual.has(FusionContent.DATA)&&!virtual.has(FusionContent.VIEW)&&ammo.getCount()==1,"Multishot virtual extras do not copy fusion");p.discard();h.succeed();
    }
    @GameTest(maxTicks=80) public void nativeAttackAddsScaledDamageAndOneUse(GameTestHelper h){
        var a=survival(h);var b=survival(h);var vanilla=h.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER,new net.minecraft.core.BlockPos(2,1,4));var fused=h.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER,new net.minecraft.core.BlockPos(3,1,4));vanilla.setNoAi(true);fused.setNoAi(true);a.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.DIAMOND),h.getLevel().registryAccess());b.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
        h.runAfterDelay(25,() -> {for(int n=0;n<30;n++){a.doTick();b.doTick();}float av=vanilla.getHealth(),bv=fused.getHealth();a.attack(vanilla);b.attack(fused);System.out.println("P9 attack deltas vanilla="+(av-vanilla.getHealth())+" fused="+(bv-fused.getHealth())+" uses="+sword.get(FusionContent.DATA).remaining());h.assertTrue(Math.abs((bv-fused.getHealth())-(av-vanilla.getHealth())-3)<.01,"Native full attack adds metal bonus before native mitigation");h.assertTrue(sword.get(FusionContent.DATA).remaining()==31&&sword.getDamageValue()>0,"Native durability and exactly one successful hit use");a.discard();b.discard();h.succeed();});
    }
    @GameTest(maxTicks=60) public void nativeShieldKeepsBlockAndWear(GameTestHelper h){
        var p=survival(h);var attacker=h.spawn(net.minecraft.world.entity.EntityTypes.HUSK,new net.minecraft.core.BlockPos(1,1,3));attacker.setNoAi(true);var shield=new ItemStack(Items.SHIELD);FusionItems.attach(shield,new ItemStack(Items.BLAZE_POWDER),h.getLevel().registryAccess());p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,shield);p.setYHeadRot(0);p.setYRot(0);p.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        h.runAfterDelay(10,() -> {for(int n=0;n<10;n++)p.doTick();float health=p.getHealth();p.hurtServer(h.getLevel(),p.damageSources().mobAttack(attacker),6);h.assertTrue(p.getHealth()==health&&shield.getDamageValue()>0&&shield.get(FusionContent.DATA).remaining()==31&&attacker.isOnFire(),"Actual full native shield block consumes one use, keeps shield wear and counters");p.discard();h.succeed();});
    }
    private static final class ImpactArrow extends net.minecraft.world.entity.projectile.arrow.Arrow{
        ImpactArrow(GameTestHelper h,net.minecraft.world.entity.LivingEntity owner,ItemStack ammo){super(h.getLevel(),owner,ammo,new ItemStack(Items.BOW));}
        void impact(net.minecraft.world.entity.Entity entity){super.onHitEntity(new net.minecraft.world.phys.EntityHitResult(entity));}
        void wall(net.minecraft.core.BlockPos p){super.onHitBlock(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),net.minecraft.core.Direction.NORTH,p,false));}
    }
    @GameTest public void nativeArrowImpactAndWallRemoveMaterialOnce(GameTestHelper h){
        var p=survival(h);var target=h.spawn(net.minecraft.world.entity.EntityTypes.VILLAGER,new net.minecraft.core.BlockPos(2,1,4));target.setNoAi(true);var ammo=new ItemStack(Items.ARROW);FusionItems.attach(ammo,new ItemStack(Items.SNOWBALL),h.getLevel().registryAccess());var arrow=new ImpactArrow(h,p,ammo);arrow.setDeltaMovement(0,0,1);h.getLevel().addFreshEntity(arrow);h.assertTrue(arrow.getAttached(FusionCombat.ARROW_VIEW)!=null,"Public arrow view tracks, without material payload");arrow.impact(target);h.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)&&!arrow.getPickupItemStackOrigin().has(FusionContent.DATA)&&arrow.getAttached(FusionCombat.ARROW_VIEW)==null,"One native impact effect and ordinary pickup, not reusable fused arrow");arrow.discard();
        var second=new ImpactArrow(h,p,ammo);h.getLevel().addFreshEntity(second);second.wall(h.absolutePos(new net.minecraft.core.BlockPos(3,1,3)));h.assertTrue(!second.getPickupItemStackOrigin().has(FusionContent.DATA),"Wall also spends material");second.discard();p.discard();h.succeed();
    }
    @GameTest public void nativeRepairAndIngredientPreserveOrRefuse(GameTestHelper h){
        var p=survival(h);var host=new ItemStack(Items.IRON_SWORD);host.setDamageValue(100);FusionItems.attach(host,new ItemStack(Items.DIAMOND_PICKAXE),h.getLevel().registryAccess());FusionCombat.use(host,h.getLevel());var plain=new ItemStack(Items.IRON_SWORD);plain.setDamageValue(200);
        var recipe=new net.minecraft.world.item.crafting.RepairItemRecipe();var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(host,plain));h.assertTrue(recipe.matches(input,h.getLevel()),"Single fused repair accepted");var repaired=recipe.assemble(input);h.assertTrue(repaired.get(FusionContent.DATA).equals(host.get(FusionContent.DATA))&&repaired.getDamageValue()<host.getDamageValue(),"Crafting repair keeps one exact material and remaining uses");
        h.assertTrue(recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(host,host.copy()))).isEmpty()&&!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(host,host.copy())),h.getLevel()),"Two fused hosts cannot silently sacrifice another material");
        var anvil=new net.minecraft.world.inventory.AnvilMenu(1,p.getInventory());anvil.getSlot(0).set(host.copy());anvil.getSlot(1).set(new ItemStack(Items.IRON_INGOT));anvil.createResult();h.assertTrue(anvil.getSlot(2).getItem().get(FusionContent.DATA).equals(host.get(FusionContent.DATA)),"Native anvil repairs host without refreshing fusion");anvil.getSlot(1).set(host.copy());anvil.createResult();h.assertTrue(anvil.getSlot(2).getItem().isEmpty(),"Anvil refuses consuming a fused second input");
        var grinder=new net.minecraft.world.inventory.GrindstoneMenu(2,p.getInventory());grinder.getSlot(0).set(plain);grinder.getSlot(1).set(host.copy());grinder.slotsChanged(grinder.getSlot(0).container);h.assertTrue(grinder.getSlot(2).getItem().get(FusionContent.DATA).equals(host.get(FusionContent.DATA)),"Grindstone keeps material whichever input owns it");
        var arrow=new ItemStack(Items.ARROW);FusionItems.attach(arrow,new ItemStack(Items.STONE),h.getLevel().registryAccess());h.assertTrue(!net.minecraft.world.item.crafting.Ingredient.of(Items.ARROW).test(arrow),"Ordinary arrow recipes cannot discard fused material");p.discard();h.succeed();
    }

    @GameTest public void nativeCreativePacketCannotCloneOrOverwrite(GameTestHelper h){
        var p=survival(h);var host=new ItemStack(Items.IRON_SWORD);FusionItems.attach(host,new ItemStack(Items.DIAMOND),h.getLevel().registryAccess());p.getInventory().setItem(0,host);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var retained=host.get(FusionContent.DATA);var forged=host.copy();forged.set(FusionContent.DATA,FusionData.projection(retained.networkHash()));
        p.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(37,forged));h.assertTrue(p.getInventory().getItem(1).isEmpty(),"Native creative edit cannot clone redacted projection");
        p.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(36,new ItemStack(Items.STONE)));h.assertTrue(p.getInventory().getItem(0)==host&&host.get(FusionContent.DATA).equals(retained),"Native creative edit cannot erase an actual retained material");
        p.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(37,new ItemStack(Items.STONE)));h.assertTrue(p.getInventory().getItem(1).is(Items.STONE),"Unrelated ordinary creative inventory still works");p.discard();h.succeed();
    }

}
