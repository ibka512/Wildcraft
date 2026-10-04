package dev.wildcraft.test.fuse;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.wildcraft.fuse.*;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Fresh loopback test only: compare full server ownership with two independent network views. */
public final class FusionMultiplayerProbe {
    public static final AttachmentType<Integer> STAGE=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p9_stage"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> ACK=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p9_ack"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    private static final AABB AREA=new AABB(-6,118,-6,6,125,6);
    private static boolean prepared;
    private static ItemStack original;
    private FusionMultiplayerProbe(){}
    public static void initialize(){
        if(!Boolean.getBoolean("wildcraft.p9.server"))return;
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
            var p=handler.player;var level=server.overworld();
            if(!prepared){
                prepared=true;
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=119;y<=124;y++)level.setBlock(new BlockPos(x,y,z),y==119?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                var target=net.minecraft.world.entity.EntityTypes.VILLAGER.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);target.setNoAi(true);target.setPos(.5,120,2.5);level.addFreshEntity(target);
            }
            p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.getInventory().clearContent();p.teleportTo(level,p.getGameProfile().name().equals("WCActor")?.5:3.5,120,.5,Set.of(),0,0,false);
            if(p.getGameProfile().name().equals("WCActor")){
                var host=new ItemStack(Items.IRON_SWORD);host.setDamageValue(17);p.getInventory().setItem(0,host);p.getInventory().setItem(1,new ItemStack(Items.BOW));
                original=new ItemStack(Items.SHULKER_BOX);var secret=new ItemStack(Items.DIAMOND_PICKAXE);secret.setDamageValue(123);secret.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("P9 private server material"));original.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(secret,new ItemStack(Items.DIAMOND,3))));p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,original.copy());p.setAttached(STAGE,0);p.setAttached(ACK,-1);
            }else{
                for(int n=0;n<36;n++)p.getInventory().setItem(n,new ItemStack(Items.STONE,64));p.getInventory().setItem(5,ItemStack.EMPTY);p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.STONE,64));
            }
            p.inventoryMenu.broadcastChanges();
        });
        CommandRegistrationCallback.EVENT.register((d,r,env) -> {
            d.register(Commands.literal("p9observe").then(Commands.argument("stage",IntegerArgumentType.integer(0,5)).executes(ctx -> {var p=ctx.getSource().getPlayerOrException();var actor=ctx.getSource().getServer().getPlayerList().getPlayerByName("WCActor");if(p.getGameProfile().name().equals("WCObserver")&&actor!=null)actor.setAttached(ACK,IntegerArgumentType.getInteger(ctx,"stage"));return 1;})));
            d.register(Commands.literal("p9next").executes(ctx -> {var p=ctx.getSource().getPlayerOrException();if(p.getGameProfile().name().equals("WCActor"))advance(p);return 1;}));
        });
    }
    private static void advance(ServerPlayer p){
        int stage=p.getAttachedOrElse(STAGE,-1);var other=p.level().getServer().getPlayerList().getPlayerByName("WCObserver");check(other!=null&&p.getAttachedOrElse(ACK,-1)==stage,"Independent observer acknowledged "+stage);
        switch(stage){
            case 0 -> check(p.getInventory().getItem(0).get(FusionContent.DATA).remaining()==32&&p.getOffhandItem().isEmpty()&&ItemStack.matches(original,p.getInventory().getItem(0).get(FusionContent.DATA).material(p.registryAccess()).orElseThrow()),"Once paid actual full private material");
            case 1 -> check(p.getInventory().getItem(0).get(FusionContent.DATA).remaining()==31&&p.getMainHandItem().is(Items.BOW),"Native melee one use then actual back ownership");
            case 2 -> {
                var drops=p.level().getEntitiesOfClass(ItemEntity.class,AREA);check(drops.size()==1&&drops.getFirst().getItem().get(FusionContent.DATA).remaining()==31&&count(p)==0,"One exact native dropped host");var pos=drops.getFirst().position();other.teleportTo(other.level(),pos.x,pos.y,pos.z,Set.of(),0,0,false);
            }
            case 3 -> {
                check(count(p)==0&&count(other)==1,"Native pickup transfers unique ownership between users");int slot=java.util.stream.IntStream.range(0,36).filter(n -> other.getInventory().getItem(n).has(FusionContent.DATA)).findFirst().orElseThrow();var actual=other.getInventory().removeItemNoUpdate(slot);for(int n=0;n<36;n++)other.getInventory().setItem(n,new ItemStack(Items.STONE,64));other.getInventory().setItem(0,actual);other.getInventory().setSelectedSlot(0);other.inventoryMenu.broadcastChanges();
            }
            case 4 -> {
                check(other.getMainHandItem().get(FusionContent.DATA).remaining()==31&&count(other)==1&&java.util.stream.IntStream.range(0,36).noneMatch(n -> other.getInventory().getItem(n).isEmpty()),"Native full split refuses without losing data");other.getInventory().setItem(5,ItemStack.EMPTY);other.inventoryMenu.broadcastChanges();
            }
            case 5 -> {
                var recovered=other.getInventory().getItem(5);var expected=original.copy();expected.set(FusionContent.WEAR,31);check(count(p)==0&&count(other)==0&&ItemStack.matches(expected,recovered)&&other.getMainHandItem().is(Items.IRON_SWORD)&&other.getMainHandItem().getDamageValue()>17,"Second user recovers exact private original, worn material, native host wear and no duplicate");System.out.println("WILDCRAFT P9 TWO ORDINARY CLIENTS PASSED: public-only equipment/back/drop, native attack one use, unique drop/pickup ownership, full split refusal and exact private worn material recovery");
            }
            default -> throw new AssertionError("Unexpected fusion stage "+stage);
        }
        p.setAttached(STAGE,stage+1);p.setAttached(ACK,-1);System.out.println("WILDCRAFT P9 server stage="+(stage+1));
    }
    private static long count(ServerPlayer p){return java.util.stream.IntStream.range(0,36).filter(n -> p.getInventory().getItem(n).has(FusionContent.DATA)).count();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
