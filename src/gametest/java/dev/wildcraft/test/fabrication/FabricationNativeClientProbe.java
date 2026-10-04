package dev.wildcraft.test.fabrication;

import dev.wildcraft.fabrication.*;
import dev.wildcraft.client.fabrication.FabricatorScreen;
import net.minecraft.client.*;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.*;

/** Independent JVM's native menu state and packets. */
public final class FabricationNativeClientProbe {
    private static long started,due;
    private static int stage=-1;
    private static boolean acted,sent;
    public static void frame(Minecraft c){
        String role=System.getProperty("wildcraft.p8.role","");if(role.isEmpty())return;long now=System.nanoTime();if(started==0)started=now;if(now-started>150_000_000_000L)throw new AssertionError("Factory "+role+" timed out at "+stage);
        if(c.player==null||c.level==null||c.gui.overlay()!=null)return;check(System.getProperty("fabric.client.gametest")==null,"Ordinary scheduler required");c.getTutorial().stop();c.gui.toastManager().clear();c.options.pauseOnLostFocus=false;c.options.enableVsync().set(false);
        Player actor=role.equals("actor")?c.player:c.level.players().stream().filter(p -> p.getGameProfile().name().equals("WCActor")).findFirst().orElse(null);if(actor==null)return;int current=actor.getAttachedOrElse(FabricationMultiplayerProbe.STAGE,-1);if(current<0)return;
        if(stage!=current){stage=current;acted=false;sent=false;due=now+(stage==0?1_200_000_000L:250_000_000L);System.out.println("WILDCRAFT P8 ordinary "+role+" stage="+stage+" fabric.client.gametest="+System.getProperty("fabric.client.gametest"));}
        if(stage==4){if(now>=due)c.stop();return;}
        if(!acted&&now>=due){
            if(stage==0){open(c);}
            else if(!(c.player.containerMenu instanceof FabricatorMenu m))return;
            else if(stage==1){c.gameMode.handleInventoryButtonClick(m.containerId,0);c.gameMode.handleInventoryButtonClick(m.containerId,0);}
            else if(stage==2){c.player.closeContainer();open(c);}
            else if(stage==3){if(!m.getSlot(2).hasItem())return;c.gameMode.handleContainerInput(m.containerId,2,0,ContainerInput.QUICK_MOVE,c.player);}
            acted=true;due=now+250_000_000L;
        }
        if(!acted||sent||now<due||!(c.player.containerMenu instanceof FabricatorMenu m)||!(c.gui.screen() instanceof FabricatorScreen))return;
        boolean correct=switch(stage){case 0 -> m.status()==0;case 1,2 -> m.status()==2&&!m.getSlot(2).hasItem()&&m.getSlot(0).getItem().getCount()==4&&m.getSlot(1).getItem().getCount()==2;case 3 -> !m.getSlot(2).hasItem()&&m.status()==0;default -> false;};if(!correct)return;
        if(role.equals("observer")){Screenshot.grab(c.gameDirectory,"p8-observer-"+stage+".png",c.gameRenderer.mainRenderTarget(),1,msg -> System.out.println("WILDCRAFT P8 screenshot "+msg.getString()));c.getConnection().sendCommand("p8observe "+stage);sent=true;System.out.println("WILDCRAFT P8 independently observed="+stage);}
        else if(actor.getAttachedOrElse(FabricationMultiplayerProbe.ACK,-1)==stage){c.getConnection().sendCommand("p8next");sent=true;}
    }
    private static void open(Minecraft c){var pos=FabricationMultiplayerProbe.POS;c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
