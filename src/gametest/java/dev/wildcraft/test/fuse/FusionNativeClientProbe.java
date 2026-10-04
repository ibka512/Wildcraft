package dev.wildcraft.test.fuse;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.fuse.FusionPresentation;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.fuse.*;
import net.minecraft.client.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Ordinary frame scheduler; no fabricated shared client state or gametest scheduling. */
public final class FusionNativeClientProbe {
    private static final AABB AREA=new AABB(-6,118,-6,6,125,6);
    private static long started,due;
    private static int stage=-1;
    private static boolean acted,sent,primed;
    private FusionNativeClientProbe(){}
    public static void frame(Minecraft c){
        String role=System.getProperty("wildcraft.p9.role","");if(role.isEmpty())return;
        long now=System.nanoTime();if(started==0)started=now;if(now-started>180_000_000_000L)throw new AssertionError("Fusion "+role+" timed out at "+stage);
        if(c.player==null||c.level==null||c.gui.overlay()!=null)return;
        check(System.getProperty("fabric.client.gametest")==null,"Ordinary scheduler required");c.getTutorial().stop();c.gui.toastManager().clear();c.options.pauseOnLostFocus=false;c.options.enableVsync().set(false);
        Player actor=role.equals("actor")?c.player:c.level.players().stream().filter(p -> p.getGameProfile().name().equals("WCActor")).findFirst().orElse(null);if(actor==null)return;
        int current=actor.getAttachedOrElse(FusionMultiplayerProbe.STAGE,-1);if(current<0)return;
        if(stage!=current){stage=current;acted=false;sent=false;primed=false;due=now+(stage==0?1_200_000_000L:400_000_000L);System.out.println("WILDCRAFT P9 ordinary "+role+" stage="+stage+" fabric.client.gametest="+System.getProperty("fabric.client.gametest"));}
        if(stage==6){if(now>=due)c.stop();return;}
        boolean localActor=role.equals("actor");
        if(!acted&&now>=due){
            if(localActor){
                if(stage==0){click(c,false);}
                else if(stage==1){var targets=c.level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class,AREA);if(targets.size()!=1)return;c.gameMode.attack(c.player,targets.getFirst());c.player.getInventory().setSelectedSlot(1);}
                else if(stage==2){if(!primed){c.player.getInventory().setSelectedSlot(0);primed=true;due=now+250_000_000L;return;}if(!c.player.getMainHandItem().has(FusionContent.VIEW))return;c.gameMode.dropItem(c.player,false);}
            }else if(stage==4||stage==5){if(!c.player.getMainHandItem().has(FusionContent.VIEW))return;click(c,true);}
            acted=true;due=now+400_000_000L;
        }
        if(!acted||sent||now<due)return;
        if(localActor){
            if(actor.getAttachedOrElse(FusionMultiplayerProbe.ACK,-1)==stage){c.options.keyShift.setDown(false);c.getConnection().sendCommand("p9next");sent=true;}
            return;
        }
        boolean correct=switch(stage){
            case 0 -> publicOnly(actor.getMainHandItem(),32);
            case 1 -> {var back=BackEquipment.view(actor).melee();yield actor.getMainHandItem().is(Items.BOW)&&back.has(FusionContent.VIEW)&&back.get(FusionContent.VIEW).uses()==31&&!back.has(FusionContent.DATA);}
            case 2 -> c.level.getEntitiesOfClass(ItemEntity.class,AREA).stream().anyMatch(e -> publicOnly(e.getItem(),31));
            case 3 -> java.util.stream.IntStream.range(0,36).anyMatch(n -> publicOnly(c.player.getInventory().getItem(n),31))&&actor.getMainHandItem().isEmpty()&&BackEquipment.view(actor).melee().isEmpty();
            case 4 -> publicOnly(c.player.getMainHandItem(),31)&&c.player.getInventory().getItem(5).is(Items.STONE);
            case 5 -> !c.player.getMainHandItem().has(FusionContent.VIEW)&&c.player.getInventory().getItem(5).is(Items.SHULKER_BOX)&&c.player.getInventory().getItem(5).getOrDefault(FusionContent.WEAR,0)==31;
            default -> false;
        };
        if(correct){c.options.keyShift.setDown(false);Screenshot.grab(c.gameDirectory,"p9-observer-"+stage+".png",c.gameRenderer.mainRenderTarget(),1,msg -> System.out.println("WILDCRAFT P9 screenshot "+msg.getString()));c.getConnection().sendCommand("p9observe "+stage);sent=true;System.out.println("WILDCRAFT P9 independently observed="+stage);}
    }
    private static boolean publicOnly(net.minecraft.world.item.ItemStack stack,int uses){var v=stack.get(FusionContent.VIEW);var d=stack.get(FusionContent.DATA);return v!=null&&v.uses()==uses&&d!=null&&d.isProjection()&&d.rawMaterial().isEmpty();}
    private static void click(Minecraft c,boolean split){c.options.keyShift.setDown(split);KeyMapping.click(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_V));}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
