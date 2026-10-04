package dev.wildcraft.test.mechanics;

import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;

/** Each ordinary client acknowledges only its own received world and inventory. */
public final class MachineNativeClientProbe {
    private static long started,due;
    private static int stage=-1;
    private static boolean acted,sent;
    public static void frame(Minecraft c) {
        String role=System.getProperty("wildcraft.p6.role","");if(role.isEmpty())return;
        long now=System.nanoTime();if(started==0)started=now;if(now-started>150_000_000_000L)throw new AssertionError("P6 ordinary "+role+" timed out at "+stage);
        if(c.player==null || c.level==null || c.gui.screen()!=null || c.gui.overlay()!=null)return;
        check(System.getProperty("fabric.client.gametest")==null,"Ordinary scheduler required");c.getTutorial().stop();c.gui.toastManager().clear();c.options.pauseOnLostFocus=false;c.options.enableVsync().set(false);
        Player actor=role.equals("actor")?c.player:c.level.players().stream().filter(p -> p.getGameProfile().name().equals("WCActor")).findFirst().orElse(null);
        if(actor==null)return;int current=actor.getAttachedOrElse(MachineMultiplayerProbe.STAGE,-1);if(current<0)return;
        var b=java.util.stream.StreamSupport.stream(c.level.entitiesForRendering().spliterator(),false).filter(e -> e instanceof MachineEntity).map(e -> (MachineEntity)e).findFirst().orElse(null);
        if(current!=stage){stage=current;acted=false;sent=false;due=now+1_600_000_000L;c.options.keyShift.setDown(false);
            if(role.equals("actor")){c.player.getInventory().setSelectedSlot(stage==0?0:stage==1?1:2);if(stage>=5&&stage<=7)c.options.keyShift.setDown(true);}
            System.out.println("WILDCRAFT P6 native "+role+" stage="+stage+" fabric.client.gametest="+System.getProperty("fabric.client.gametest"));}
        if(stage==8){if(now>=due)c.stop();return;}
        Vec3 local=stage==1||stage==5?MachineNodes.point(0):MachineNodes.point(2);
        if(b!=null && actor.getVehicle()==null){var d=b.position().add(MachineNodes.rotate(local,b.getYRot())).subtract(c.player.getEyePosition());c.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));}
        if(!acted && now>=due) {
            if(role.equals("actor")) {
                if(stage==3||stage==4){ClientPlayNetworking.send(new MachineToggle());if(stage==4)c.options.keyShift.setDown(true);}
                else if(b==null)return;
                else if(stage==7)c.gameMode.attack(c.player,b);
                else c.gameMode.interact(c.player,b,new EntityHitResult(b,b.position().add(MachineNodes.rotate(local,b.getYRot()))),InteractionHand.MAIN_HAND);
            } else if(stage==0){if(b==null)return;c.gameMode.interact(c.player,b,new EntityHitResult(b,b.position().add(MachineNodes.point(2))),InteractionHand.MAIN_HAND);}
            acted=true;due=now+1_600_000_000L;
        }
        if(!acted || sent || now<due)return;
        boolean correct=switch(stage){
            case 0 -> b!=null&&b.kind(2)==1;
            case 1 -> b!=null&&b.energy()==240;
            case 2 -> actor.getVehicle() instanceof MachineEntity;
            case 3 -> b!=null&&b.enabled()&&b.energy()<240;
            case 4 -> b!=null&&!b.enabled()&&actor.getVehicle()==null;
            case 5 -> b!=null&&b.batteryNode()==-1;
            case 6 -> b!=null&&b.fanMask()==0;
            case 7 -> b==null;
            default -> false;
        };
        if(!correct)return;
        if(role.equals("observer")) {
            check(c.player.getInventory().getItem(0).is(MechanicsContent.FAN)&&c.player.getInventory().getItem(0).getCount()==1,"Other actor keeps original fan");
            if(b!=null)for(int i=0;i<6;i++)check(b.part(i).isEmpty(),"Tracking contains no private full stacks");
            Screenshot.grab(c.gameDirectory,"p6-observer-"+stage+".png",c.gameRenderer.mainRenderTarget(),1,msg -> System.out.println("WILDCRAFT P6 screenshot "+msg.getString()));
            c.getConnection().sendCommand("p6observe "+stage);sent=true;System.out.println("WILDCRAFT P6 independently observed="+stage);
        } else if(actor.getAttachedOrElse(MachineMultiplayerProbe.ACK,-1)==stage){c.options.keyShift.setDown(false);c.getConnection().sendCommand("p6next");sent=true;}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
