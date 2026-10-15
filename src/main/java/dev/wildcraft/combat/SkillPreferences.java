package dev.wildcraft.combat;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.network.SkillIntent;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
/** Per connection; settings live in the client file, never in a saved player inventory. */
public final class SkillPreferences {
    private static final AttachmentType<SkillIntent> OPTIONS=AttachmentRegistry.create(Wildcraft.id("skill_preferences"));
    private SkillPreferences(){}
    public static int bow(ServerPlayer p){var v=p.getAttached(OPTIONS);return v==null?0:v.bow();}
    public static int dodge(ServerPlayer p){var v=p.getAttached(OPTIONS);return v==null?0:v.dodge();}
    public static void initialize(){PayloadTypeRegistry.serverboundPlay().register(SkillIntent.TYPE,SkillIntent.CODEC);ServerPlayNetworking.registerGlobalReceiver(SkillIntent.TYPE,(i,c)->receive(c.player(),i));}
    public static void receive(ServerPlayer p,SkillIntent i){
        if(!i.valid())return;
        if(i.operation()==SkillIntent.PREFERENCES){p.setAttached(OPTIONS,i);if(i.bow()!=0)FocusTime.interrupt(p);if(i.dodge()!=0)ReactionTime.close(p.level().getServer());}
        else if(i.operation()==SkillIntent.CANCEL){FocusTime.interrupt(p);ReactionTime.cancel(p);}
        else {if(!ReactionTime.confirm(p))FocusTime.confirm(p);}
    }
}
