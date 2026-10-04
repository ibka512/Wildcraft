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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Fresh loopback worlds only; gated out of both production and other test runs. */
public final class PartsMultiplayerProbe {
    public static final AttachmentType<Integer> STAGE=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p7_stage"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> ACK=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","p7_ack"),b -> b.syncWith(ByteBufCodecs.VAR_INT,AttachmentSyncPredicate.all()));
    private static MachineEntity body;
    private static int fuel;
    public static void initialize() {
        if(!Boolean.getBoolean("wildcraft.p7.server"))return;
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> {
            var p=handler.player;p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
            if(p.getGameProfile().name().equals("WCActor")) {
                var level=server.overworld();for(int x=-8;x<=12;x++)for(int z=-8;z<=12;z++)for(int y=119;y<=128;y++)level.setBlock(new BlockPos(x,y,z),y==119||z==5?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                body=MechanicsContent.MACHINE.create(level,EntitySpawnReason.COMMAND);body.setPos(.5,120,2.5);body.setOwner(p.getUUID());level.addFreshEntity(body);p.teleportTo(level,.5,120,.5,Set.of(),0,30,false);
                for(int n:new int[]{0,1,3}){var stack=n==0?new ItemStack(EnergyContent.BATTERY):new ItemStack(n==1?MechanicsContent.SPRING:MechanicsContent.WING);if(n==0)Batteries.charge(stack,1000);p.setItemInHand(InteractionHand.MAIN_HAND,stack);check(body.install(p,n,InteractionHand.MAIN_HAND),"Fixture full stack transfer");}
                p.getInventory().setItem(0,new ItemStack(MechanicsContent.STABILIZER));p.getInventory().setItem(1,new ItemStack(MechanicsContent.BUOYANCY));
                var rocket=new ItemStack(MechanicsContent.ROCKET);rocket.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Private rocket components"));p.getInventory().setItem(2,rocket);
                p.teleportTo(level,-1.5,120,2.5,Set.of(),-90,30,false);p.setAttached(STAGE,0);p.setAttached(ACK,-1);
            } else {p.teleportTo(server.overworld(),-1.5,120,1.5,Set.of(),-70,30,false);p.getInventory().setItem(0,new ItemStack(MechanicsContent.STABILIZER));}
            p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();
        });
        CommandRegistrationCallback.EVENT.register((d,r,env) -> {
            d.register(Commands.literal("p7observe").then(Commands.argument("stage",IntegerArgumentType.integer(0,7)).executes(ctx -> {
                var p=ctx.getSource().getPlayerOrException();var actor=ctx.getSource().getServer().getPlayerList().getPlayerByName("WCActor");
                if(p.getGameProfile().name().equals("WCObserver")&&actor!=null)actor.setAttached(ACK,IntegerArgumentType.getInteger(ctx,"stage"));return 1;
            })));
            d.register(Commands.literal("p7next").executes(ctx -> {var p=ctx.getSource().getPlayerOrException();if(p.getGameProfile().name().equals("WCActor"))advance(p);return 1;}));
        });
    }
    private static void advance(ServerPlayer p) {
        int stage=p.getAttachedOrElse(STAGE,-1);var observer=p.level().getServer().getPlayerList().getPlayerByName("WCObserver");
        check(observer!=null&&p.getAttachedOrElse(ACK,-1)==stage,"Independent observer received stage "+stage);
        switch(stage) {
            case 0 -> {check(body.kind(4)==7&&p.getInventory().getItem(0).isEmpty()&&observer.getMainHandItem().is(MechanicsContent.STABILIZER),"Creator's actual stabilizer transfers once; competitor keeps its own");p.teleportTo(p.level(),2.5,120,2.5,Set.of(),90,30,false);}
            case 1 -> {check(body.kind(5)==8&&p.getInventory().getItem(1).isEmpty(),"Native float installation");p.teleportTo(p.level(),.5,120,.5,Set.of(),0,30,false);}
            case 2 -> check(body.kind(2)==4&&body.rocketFuel(2)==80&&p.getInventory().getItem(2).isEmpty(),"Real fresh finite rocket");
            case 3 -> check(p.getVehicle()==body,"Native passenger control");
            case 4 -> {check(body.enabled()&&body.rocketFuel(2)>0&&body.rocketFuel(2)<80&&body.springCooldown(1)>0&&body.active(4)&&body.energy()<1000,"Fuel, cooldown and paid stabilization run in an ordinary multiplayer server");fuel=body.rocketFuel(2);}
            case 5 -> {check(!body.enabled()&&body.rocketFuel(2)<fuel&&body.rocketFuel(2)>0,"Turning off preserves irreversible live ignition");p.stopRiding();body.setPos(.5,120,4.2);body.setDeltaMovement(Vec3.ZERO);p.teleportTo(p.level(),.5,120,2.2,Set.of(),0,30,false);}
            case 6 -> check(body.kind(2)==4&&body.rocketFuel(2)>0&&p.getMainHandItem().isEmpty(),"Native hand recovery cannot pause finite fuel");
            case 7 -> {check(body.kind(2)==0&&p.getInventory().getItem(0).is(MechanicsContent.SPENT_ROCKET),"Actual exhausted casing returns once");check(p.getInventory().getItem(0).getHoverName().getString().equals("Private rocket components"),"Private original components retained only on real item");System.out.println("WILDCRAFT P7 TWO ORDINARY CLIENTS PASSED: native new-part transfer, owner guard, independent bounded kinds/fuel/cooldown/active tracking, finite combination, ignition survives off, spent recovery and no private full-stack tracking");}
            default -> throw new AssertionError("Unexpected P7 stage "+stage);
        }
        p.setAttached(STAGE,stage+1);p.setAttached(ACK,-1);p.inventoryMenu.broadcastChanges();System.out.println("WILDCRAFT P7 server stage="+(stage+1));
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
