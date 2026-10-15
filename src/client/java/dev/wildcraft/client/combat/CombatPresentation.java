package dev.wildcraft.client.combat;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.Combat;
import dev.wildcraft.input.Action;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Target information and bounded third-person direction help; never centers a shot. */
public final class CombatPresentation {
    private static long previous,mouseUntil;
    private static float lastYaw;
    private static Object player;
    private CombatPresentation(){}
    public static void initialize(){
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,Wildcraft.id("combat_target"),(g,t)->{
            var c=Minecraft.getInstance();var p=c.player;
            if(p==null||c.gui.screen()!=null||c.gui.hud.isHidden())return;
            var target=Combat.target(p);if(target==null)return;
            float offset=Mth.wrapDegrees(Combat.yawTo(p,target)-p.getYRot());
            String arrow=offset>12?"< ":offset< -12?"> ":"<> ";
            String name=c.font.plainSubstrByWidth(target.getDisplayName().getString(),Math.max(35,g.guiWidth()/3));
            String label=arrow+Component.translatable("combat.wildcraft.target",name,(int)p.distanceTo(target)).getString();
            int width=c.font.width(label),x=(g.guiWidth()-width)/2,y=g.guiHeight()/2+24;
            g.fill(x-3,y-2,x+width+3,y+11,0x7015231B);g.text(c.font,label,x,y,0xFFD7E8B7);
        });
    }
    public static void frame(Minecraft c){
        long now=System.nanoTime();var p=c.player;
        if(p!=player){player=p;previous=now;lastYaw=p==null?0:p.getYRot();mouseUntil=now+150_000_000L;return;}
        float dt=previous==0?0:Math.clamp((now-previous)/1_000_000_000F,0,.05F);previous=now;
        if(p==null)return;
        if(Math.abs(Mth.wrapDegrees(p.getYRot()-lastYaw))>.01F)mouseUntil=now+150_000_000L;
        var target=Combat.target(p);var o=CombatOptions.get();
        if(target!=null&&o.follow>0&&c.options.getCameraType()==net.minecraft.client.CameraType.THIRD_PERSON_BACK&&ActionInput.tickFrame().gameplay()
                &&!c.isPaused()&&c.isWindowActive()&&c.gui.screen()==null&&c.gui.overlay()==null&&!p.isUsingItem()
                &&!ActionInput.down(Action.USE)&&now>=mouseUntil){
            float diff=Mth.wrapDegrees(Combat.yawTo(p,target)-p.getYRot());
            if(Math.abs(diff)>60){float step=Math.copySign(Math.min(Math.abs(diff)-60,8*o.follow*dt),diff);p.setYRot(p.getYRot()+step);}
        }
        lastYaw=p.getYRot();
    }
}
