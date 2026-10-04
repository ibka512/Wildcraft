package dev.wildcraft.test.mechanics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Disposable localhost fixtures only. The production jar contains neither commands nor test state. */
public final class MachineMultiplayerProbe {
    public static final AttachmentType<Integer> STAGE=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p6_stage"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> ACK=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p6_ack"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    private static MachineEntity body;
    private static int remaining;
    public static void initialize() {
        if(!Boolean.getBoolean("wildcraft.p6.server"))return;
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
            var p=handler.player;p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
            p.teleportTo(server.overworld(),p.getGameProfile().name().equals("WCActor")?.5:-.3,120,.5,Set.of(),0,30,false);
            if(p.getGameProfile().name().equals("WCActor")) {
                for(int x=-8;x<=12;x++)for(int z=-8;z<=30;z++)for(int y=119;y<125;y++)server.overworld().setBlock(new BlockPos(x,y,z),y==119?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                body=MechanicsContent.MACHINE.create(server.overworld(),EntitySpawnReason.COMMAND);body.setPos(.5,120,2.5);body.setOwner(p.getUUID());server.overworld().addFreshEntity(body);
                p.getInventory().setItem(0,new ItemStack(MechanicsContent.FAN,2));var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,240);p.getInventory().setItem(1,battery);
                p.setAttached(STAGE,0);p.setAttached(ACK,-1);
            } else p.getInventory().setItem(0,new ItemStack(MechanicsContent.FAN));
            p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();
        });
        CommandRegistrationCallback.EVENT.register((d,r,env) -> {
            d.register(Commands.literal("p6observe").then(Commands.argument("stage",IntegerArgumentType.integer(0,7)).executes(ctx -> {
                var p=ctx.getSource().getPlayerOrException();var actor=ctx.getSource().getServer().getPlayerList().getPlayerByName("WCActor");
                if(p.getGameProfile().name().equals("WCObserver") && actor!=null)actor.setAttached(ACK,IntegerArgumentType.getInteger(ctx,"stage"));return 1;
            })));
            d.register(Commands.literal("p6next").executes(ctx -> {var p=ctx.getSource().getPlayerOrException();if(p.getGameProfile().name().equals("WCActor"))advance(p);return 1;}));
        });
    }
    private static void advance(ServerPlayer p) {
        int stage=p.getAttachedOrElse(STAGE,-1);var observer=p.level().getServer().getPlayerList().getPlayerByName("WCObserver");
        check(observer!=null && p.getAttachedOrElse(ACK,-1)==stage,"Independent observer acknowledged stage "+stage);
        switch(stage) {
            case 0 -> check(body.kind(2)==1 && count(p,MechanicsContent.FAN)==1 && count(observer,MechanicsContent.FAN)==1,"Competing native installations consume creator's one fan only");
            case 1 -> check(body.energy()==240 && count(p,EnergyContent.BATTERY)==0,"Native install transfers actual battery");
            case 2 -> check(p.getVehicle()==body,"Real passenger arrives through client interaction");
            case 3 -> check(body.enabled() && body.energy()<240,"Rider intent consumes finite energy");
            case 4 -> {check(!body.enabled() && p.getVehicle()==null && body.energy()>0,"Native rider stops and dismounts");remaining=body.energy();body.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                p.teleportTo(p.level(),body.getX(),120,body.getZ()-2,Set.of(),0,30,false);observer.teleportTo(observer.level(),body.getX()-1,120,body.getZ()-2,Set.of(),0,30,false);}
            case 5 -> check(body.batteryNode()==-1 && count(p,EnergyContent.BATTERY)==1 && Batteries.energy(p.getInventory().getItem(1))==remaining,"Actual returned battery keeps exact paid energy");
            case 6 -> check(body.fanMask()==0 && count(p,MechanicsContent.FAN)==2,"Native recovery returns a fan once");
            case 7 -> {check(body.isRemoved() && count(p,MechanicsContent.BODY)==1 && count(observer,MechanicsContent.FAN)==1,"Body recovered once; other actor kept its item");System.out.println("WILDCRAFT P6 TWO ORDINARY CLIENTS PASSED: competing native installs, owner guard, battery/rider intent, independent public tracking, finite charge and exact native recovery; remaining="+remaining);}
            default -> throw new AssertionError("Unexpected P6 stage "+stage);
        }
        p.setAttached(STAGE,stage+1);p.setAttached(ACK,-1);p.inventoryMenu.broadcastChanges();System.out.println("WILDCRAFT P6 server stage="+(stage+1));
    }
    private static int count(ServerPlayer p,net.minecraft.world.item.Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
