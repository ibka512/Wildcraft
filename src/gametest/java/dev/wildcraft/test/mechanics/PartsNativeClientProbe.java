package dev.wildcraft.test.mechanics;

import dev.wildcraft.mechanics.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;

/** Independent ordinary JVMs; no graphical test scheduler or shared client state. */
public final class PartsNativeClientProbe {
    private static long started,due;
    private static int stage=-1;
    private static boolean acted,sent;
    public static void frame(Minecraft c) {
        String role=System.getProperty("wildcraft.p7.role","");if(role.isEmpty())return;
        long now=System.nanoTime();if(started==0)started=now;if(now-started>150_000_000_000L)throw new AssertionError("P7 ordinary "+role+" timed out at "+stage);
        if(c.player==null||c.level==null||c.gui.screen()!=null||c.gui.overlay()!=null)return;
        check(System.getProperty("fabric.client.gametest")==null,"Ordinary scheduler required");c.getTutorial().stop();c.gui.toastManager().clear();c.options.pauseOnLostFocus=false;c.options.enableVsync().set(false);
        Player actor=role.equals("actor")?c.player:c.level.players().stream().filter(p -> p.getGameProfile().name().equals("WCActor")).findFirst().orElse(null);
        if(actor==null)return;int current=actor.getAttachedOrElse(PartsMultiplayerProbe.STAGE,-1);if(current<0)return;
        var b=java.util.stream.StreamSupport.stream(c.level.entitiesForRendering().spliterator(),false).filter(e -> e instanceof MachineEntity).map(e -> (MachineEntity)e).findFirst().orElse(null);
        if(current!=stage){stage=current;acted=false;sent=false;due=now+(stage==0?1_200_000_000L:250_000_000L);c.options.keyShift.setDown(false);
            if(role.equals("actor")){c.player.getInventory().setSelectedSlot(stage<=2?stage:3);if(stage>=6&&stage<=7)c.options.keyShift.setDown(true);}
            System.out.println("WILDCRAFT P7 native "+role+" stage="+stage+" fabric.client.gametest="+System.getProperty("fabric.client.gametest"));}
        if(stage==8){if(now>=due)c.stop();return;}
        Vec3 local=MachineNodes.point(stage==0?4:stage==1?5:2);
        if(b!=null&&actor.getVehicle()==null){var d=b.position().add(MachineNodes.rotate(local,b.getYRot())).subtract(c.player.getEyePosition());c.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));}
        if(!acted&&now>=due) {
            if(role.equals("actor")) {
                if(stage==4||stage==5)ClientPlayNetworking.send(new MachineToggle());
                else {if(b==null)return;if(stage==7&&b.rocketFuel(2)>0)return;c.gameMode.interact(c.player,b,new EntityHitResult(b,b.position().add(MachineNodes.rotate(local,b.getYRot()))),InteractionHand.MAIN_HAND);}
            } else if(stage==0){if(b==null)return;c.gameMode.interact(c.player,b,new EntityHitResult(b,b.position().add(local)),InteractionHand.MAIN_HAND);}
            acted=true;due=now+250_000_000L;
        }
        if(!acted||sent||now<due)return;
        boolean correct=b!=null&&switch(stage){
            case 0 -> b.kind(4)==7;
            case 1 -> b.kind(5)==8;
            case 2 -> b.kind(2)==4&&b.rocketFuel(2)==80;
            case 3 -> actor.getVehicle() instanceof MachineEntity;
            case 4 -> b.enabled()&&b.active(4)&&b.rocketFuel(2)>0&&b.rocketFuel(2)<80&&b.springCooldown(1)>0;
            case 5 -> !b.enabled()&&b.rocketFuel(2)>0&&b.rocketFuel(2)<80;
            case 6 -> !b.enabled()&&b.kind(2)==4&&b.rocketFuel(2)>0&&actor.getVehicle()==null;
            case 7 -> b.kind(2)==0;
            default -> false;
        };
        if(!correct)return;
        if(role.equals("observer")) {
            check(c.player.getInventory().getItem(0).is(MechanicsContent.STABILIZER)&&c.player.getInventory().getItem(0).getCount()==1,"Other actor keeps original stabilizer");
            for(int i=0;i<6;i++)check(b.part(i).isEmpty()&&b.rocketFuel(i)<=80&&b.springCooldown(i)<=40,"Only bounded views, no original private full stacks");
            Screenshot.grab(c.gameDirectory,"p7-observer-"+stage+".png",c.gameRenderer.mainRenderTarget(),1,msg -> System.out.println("WILDCRAFT P7 screenshot "+msg.getString()));
            c.getConnection().sendCommand("p7observe "+stage);sent=true;System.out.println("WILDCRAFT P7 independently observed="+stage+" fuel="+b.rocketFuel(2)+" spring="+b.springCooldown(1));
        } else if(actor.getAttachedOrElse(PartsMultiplayerProbe.ACK,-1)==stage){c.options.keyShift.setDown(false);c.getConnection().sendCommand("p7next");sent=true;}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
