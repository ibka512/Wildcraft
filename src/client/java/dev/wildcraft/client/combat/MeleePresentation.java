package dev.wildcraft.client.combat;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.network.MeleeView;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
/** Procedural phase feedback; no new adopted artwork. */
public final class MeleePresentation {
    private MeleePresentation(){}
    public static void initialize(){HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,Wildcraft.id("melee_charge"),(g,t)->{
        var c=Minecraft.getInstance();var p=c.player;if(p==null||c.gui.screen()!=null||c.gui.hud.isHidden()||!MeleeActions.eligible(p))return;
        var v=MeleeActions.view(p);if(v.phase()!=MeleeView.CHARGE)return;
        int x=g.guiWidth()/2,y=g.guiHeight()/2+40;boolean ready=v.age()>=v.duration();float progress=Math.clamp((float)v.age()/Math.max(1,v.duration()),0,1);
        g.fill(x-22,y,x+22,y+3,0xA0203025);g.fill(x-22,y,x-22+(int)(44*progress),y+3,ready?0xFFFFD377:0xFF95CF84);
        var label=Component.translatable(ready?"combat.wildcraft.charge_ready":"combat.wildcraft.charging");g.text(c.font,label,x-c.font.width(label)/2,y+5,0xFFE5EBDD);
    });}
}
