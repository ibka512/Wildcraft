package dev.wildcraft.client.combat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import dev.wildcraft.client.input.WildcraftBindings;

public final class CombatSettingsScreen extends Screen {
    private final Screen parent;
    public CombatSettingsScreen(Screen parent){super(Component.translatable("combat.wildcraft.settings"));this.parent=parent;}
    @Override protected void init(){
        var o=CombatOptions.get();int w=Math.min(300,width-40),top=34;
        var content=LinearLayout.vertical().spacing(6);
        content.addChild(Button.builder(mode(),b->{o.hold=!o.hold;CombatOptions.save();b.setMessage(mode());}).size(w,20).build());
        content.addChild(Button.builder(range(),b->{o.range=(o.range+1)%3;CombatOptions.save();b.setMessage(range());}).size(w,20).build());
        content.addChild(Button.builder(neutral(),b->{o.neutral=!o.neutral;CombatOptions.save();b.setMessage(neutral());}).size(w,20).build());
        content.addChild(Button.builder(follow(),b->{o.follow=(o.follow+1)%4;CombatOptions.save();b.setMessage(follow());}).size(w,20).build());
        content.addChild(Button.builder(jump(),b->{o.jumpDodge=!o.jumpDodge;CombatOptions.save();b.setMessage(jump());}).size(w,20).build());
        content.addChild(Button.builder(skill(true),b->{o.bowSkill=(o.bowSkill+1)%3;CombatOptions.save();b.setMessage(skill(true));}).size(w,20).build());
        content.addChild(Button.builder(skill(false),b->{o.dodgeSkill=(o.dodgeSkill+1)%3;CombatOptions.save();b.setMessage(skill(false));}).size(w,20).build());
        var scroll=new ScrollableLayout(minecraft,content,Math.max(20,height-top-42),ScrollableLayout.ReserveStrategy.RIGHT);
        scroll.arrangeElements();scroll.setX((width-w)/2);scroll.setY(top);scroll.visitWidgets(this::addRenderableWidget);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds((width-w)/2,height-28,w,20).build());
    }
    private Component skill(boolean bow){return Component.translatable(bow?"combat.wildcraft.bow_skill":"combat.wildcraft.dodge_skill",Component.translatable("combat.wildcraft.skill_mode."+(bow?CombatOptions.get().bowSkill:CombatOptions.get().dodgeSkill)),WildcraftBindings.CONFIRM_SKILL.getTranslatedKeyMessage());}
    private Component jump(){return Component.translatable("combat.wildcraft.jump_dodge",Component.translatable(CombatOptions.get().jumpDodge?"options.on":"options.off"));}
    private Component mode(){return Component.translatable("combat.wildcraft.mode",Component.translatable(CombatOptions.get().hold?"combat.wildcraft.hold":"combat.wildcraft.toggle"),WildcraftBindings.LOCK_TARGET.getTranslatedKeyMessage());}
    private Component range(){return Component.translatable("combat.wildcraft.range",(int)dev.wildcraft.combat.Combat.searchRange(CombatOptions.get().range));}
    private Component neutral(){return Component.translatable("combat.wildcraft.neutral",Component.translatable(CombatOptions.get().neutral?"options.on":"options.off"));}
    private Component follow(){return Component.translatable("combat.wildcraft.follow",Component.translatable("combat.wildcraft.follow."+CombatOptions.get().follow));}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float partial){g.centeredText(font,title,width/2,12,0xFFFFFFFF);super.extractRenderState(g,x,y,partial);}
}
