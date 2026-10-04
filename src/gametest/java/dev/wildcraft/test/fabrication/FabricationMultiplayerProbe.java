package dev.wildcraft.test.fabrication;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.wildcraft.fabrication.*;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Test-only shared container race on a fresh loopback world. */
public final class FabricationMultiplayerProbe {
    public static final BlockPos POS=new BlockPos(0,120,2);
    public static final AttachmentType<Integer> STAGE=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p8_stage"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> ACK=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p8_ack"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    private static FabricatorEntity machine;
    private static ItemStack expected;
    public static void initialize(){
        CommandRegistrationCallback.EVENT.register((d,r,env) -> d.register(Commands.literal("p8collected").executes(ctx -> {var p=ctx.getSource().getPlayerOrException();if(p.containerMenu instanceof FabricatorMenu menu && menu.getSlot(2).getItem().isEmpty() && FabricationPool.valid(p.getInventory().getItem(0)))p.setAttached(ACK,99);return 1;})));
        if(!Boolean.getBoolean("wildcraft.p8.server"))return;
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
            var p=handler.player;var level=server.overworld();
            // Prepare the platform before either role can fall during the other JVM's startup.
            if(machine==null||machine.isRemoved()){
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=119;y<=124;y++)level.setBlock(new BlockPos(x,y,z),y==119?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                level.setBlockAndUpdate(POS,FabricationContent.BLOCK.defaultBlockState());machine=(FabricatorEntity)level.getBlockEntity(POS);machine.setItem(0,new ItemStack(Items.COPPER_INGOT,8));machine.setItem(1,new ItemStack(Items.REDSTONE,4));
            }
            p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.getInventory().clearContent();p.teleportTo(level,p.getGameProfile().name().equals("WCActor")?.5:-.5,120,.5,Set.of(),0,30,false);
            if(p.getGameProfile().name().equals("WCActor")){p.setAttached(STAGE,0);p.setAttached(ACK,-1);}p.inventoryMenu.broadcastChanges();
        });
        CommandRegistrationCallback.EVENT.register((d,r,env) -> {d.register(Commands.literal("p8observe").then(Commands.argument("stage",IntegerArgumentType.integer(0,3)).executes(ctx -> {var p=ctx.getSource().getPlayerOrException();var actor=ctx.getSource().getServer().getPlayerList().getPlayerByName("WCActor");if(p.getGameProfile().name().equals("WCObserver")&&actor!=null)actor.setAttached(ACK,IntegerArgumentType.getInteger(ctx,"stage"));return 1;})));d.register(Commands.literal("p8next").executes(ctx -> {var p=ctx.getSource().getPlayerOrException();if(p.getGameProfile().name().equals("WCActor"))advance(p);return 1;}));});
    }
    private static void advance(ServerPlayer p){
        int stage=p.getAttachedOrElse(STAGE,-1);var other=p.level().getServer().getPlayerList().getPlayerByName("WCObserver");check(other!=null&&p.getAttachedOrElse(ACK,-1)==stage,"Independent shared menu acknowledged "+stage);
        switch(stage){
            case 0 -> check(p.containerMenu instanceof FabricatorMenu&&other.containerMenu instanceof FabricatorMenu,"Both ordinary users opened actual same block");
            case 1 -> {check(machine.hasJob()&&machine.getItem(0).getCount()==4&&machine.getItem(1).getCount()==2,"Simultaneous native buttons debit one job only");expected=machine.pendingResult();}
            case 2 -> check(machine.hasJob()&&ItemStack.matches(expected,machine.pendingResult())&&machine.getItem(0).getCount()==4,"Native reopen and replay cannot redraw a paid job");
            case 3 -> {int outputs=0;for(var player:new ServerPlayer[]{p,other})for(int n=0;n<36;n++){var stack=player.getInventory().getItem(n);if(ItemStack.matches(expected,stack))outputs+=stack.getCount();}check(outputs==1&&!machine.hasJob()&&machine.getItem(2).isEmpty()&&machine.getItem(0).getCount()==4&&machine.getItem(1).getCount()==2,"Competing native quick-move gives one exact output to one player");System.out.println("WILDCRAFT P8 TWO ORDINARY CLIENTS PASSED: shared native menus, concurrent button commits once, hidden result during job, reopen/replay preserves result and competing collection transfers one output");}
            default -> throw new AssertionError("Unexpected factory stage");
        }
        p.setAttached(STAGE,stage+1);p.setAttached(ACK,-1);System.out.println("WILDCRAFT P8 server stage="+(stage+1));
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
