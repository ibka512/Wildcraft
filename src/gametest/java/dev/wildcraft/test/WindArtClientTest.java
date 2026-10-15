package dev.wildcraft.test;

import dev.wildcraft.client.cooking.MealHud;
import dev.wildcraft.client.temperature.TemperatureHud;
import dev.wildcraft.client.weather.WindHud;
import dev.wildcraft.cooking.*;
import dev.wildcraft.network.WindView;
import dev.wildcraft.player.*;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Gliding;
import dev.wildcraft.weather.WindSystem;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Real world wind, camera direction, language and window rendering of the adopted F07 atlas. */
public final class WindArtClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        int oldScale=c.computeOnClient(v->v.options.guiScale().get());
        String oldLanguage=c.computeOnClient(v->v.options.languageCode);
        try{
            rules(c);
            try(TestSingleplayerContext world=c.worldBuilder().create()){
                world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
                world.getServer().runCommand("weather thunder");world.getServer().runCommand("fillbiome -8 160 -8 8 250 8 minecraft:plains");
                world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);p.setHealth(60);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(8);p.setAbsorptionAmount(8);GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));PlayerStamina.fill(p);for(int k=1;k<=3;k++)CookingEffects.eat(p,new MealData(1,k,2,300));p.teleportTo(s.overworld(),.5,220,.5,Set.of(),0,0,false);p.setNoGravity(true);p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);s.overworld().setRainLevel(1);s.overworld().setThunderLevel(1);WindSystem.publish(p);});
                c.waitFor(v->v.player.getY()>219&&GliderEquipment.equipped(v.player)&&WindSystem.local(v.player).precipitation()>.9);
                open(c);
                for(String lang:new String[]{"zh_cn","en_us"}){
                    language(c,lang);if(!c.computeOnClient(v->Gliding.active(v.player)))open(c);
                    for(int scale:new int[]{1,2,3}){
                        c.getInput().resizeWindow(1280,960);c.runOnClient(v->v.options.guiScale().set(scale));c.waitTicks(3);
                        check(c.computeOnClient(v->v.getWindow().getGuiScale()==scale),"Native GUI scale is actually selected");
                        check(c.computeOnClient(v->row(v)!=null),"Adopted wind line fits below real multiheart/temperature/three-food stack");
                        c.takeScreenshot("f07-multiheart-food-"+lang+"-scale"+scale);
                    }
                }
                c.runOnClient(v->v.options.guiScale().set(2));
                for(int index=0;index<8;index++){
                    final int target=index;
                    c.runOnClient(v->{var wind=WindSystem.local(v.player);float bearing=(float)Math.toDegrees(Math.atan2(-wind.x(),wind.z()));v.player.setYRot(bearing-45*target);v.player.setXRot(0);});
                    c.waitTicks(1);check(c.computeOnClient(v->WindHud.direction(v.player.getYRot(),WindSystem.local(v.player))==target),"Real sampled wind maps to requested camera-relative direction "+index);
                    c.takeScreenshot("f07-real-view-direction-"+index);
                }
                c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(2);c.takeScreenshot("f07-third-person-glide");
                c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));
                // Native minimum window and maximal allowed scaling may compact or omit, never move old HUD.
                c.getInput().resizeWindow(640,480);c.runOnClient(v->v.options.guiScale().set(3));c.waitTicks(3);c.takeScreenshot("f07-small-native-window");
                c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);c.waitFor(v->v.gui.hud.isHidden());c.takeScreenshot("f07-native-hidden-hud");c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);c.waitFor(v->!v.gui.hud.isHidden());
                c.getInput().resizeWindow(1280,960);c.runOnClient(v->v.options.guiScale().set(2));
                world.getServer().runCommand("fillbiome -8 118 -8 8 126 8 minecraft:plains");
                world.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var l=s.overworld();Gliding.reset(p,false);p.setNoGravity(false);p.teleportTo(l,.5,120,.5,Set.of(),0,0,false);p.setDeltaMovement(Vec3.ZERO);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)l.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,119,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());var body=dev.wildcraft.mechanics.MechanicsContent.MACHINE.create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);body.setPos(.5,120,.5);body.setOwner(p.getUUID());l.addFreshEntity(body);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(dev.wildcraft.mechanics.MechanicsContent.WING));check(body.install(p,0,net.minecraft.world.InteractionHand.MAIN_HAND),"Real wing installed for wind presentation");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();check(p.startRiding(body),"Actual wing rider mounted");WindSystem.publish(p);});
                c.waitFor(v->v.player.getVehicle() instanceof dev.wildcraft.mechanics.MachineEntity m&&m.kind(0)==3&&!Gliding.active(v.player)&&WindSystem.local(v.player).outdoors());c.waitTicks(3);c.takeScreenshot("f07-native-wing-rider-first-person");
                c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(3);c.takeScreenshot("f07-native-wing-rider-third-person");
            }
        }finally{c.getInput().releaseKey(v->v.keyJump);c.runOnClient(v->{v.options.guiScale().set(oldScale);v.options.setCameraType(CameraType.FIRST_PERSON);});language(c,oldLanguage);}
        System.out.println("F07 ART actual eight-direction/bilingual GUI1-2-3/multiheart-threefood/native small window/F1 rendering PASS");
    }
    private static WindHud.Row row(net.minecraft.client.Minecraft v){
        int y=TemperatureHud.rowY()+14;var food=v.player.getAttached(CookingEffects.VIEW);
        for(var b:MealHud.layout(v.getWindow().getGuiScaledWidth(),v.getWindow().getGuiScaledHeight(),y,new int[]{food.warmth(),food.cooling(),food.recovery()},new int[]{food.warmSeconds(),food.coolSeconds(),food.recoverySeconds()}))y=Math.max(y,b.y()+(b.compact()?20:16)+4);
        var wind=WindSystem.local(v.player);String key=!wind.outdoors()?"sheltered":switch(WindHud.strength(wind)){case 0->"light";case 1->"moderate";default->"strong";};
        String label=net.minecraft.network.chat.Component.translatable("hud.wildcraft.wind."+key).getString();if(wind.precipitation()>.2)label+=net.minecraft.network.chat.Component.translatable("hud.wildcraft.wind.wet").getString();
        return WindHud.layout(v.getWindow().getGuiScaledWidth(),v.getWindow().getGuiScaledHeight(),y,v.font.width(label),wind.outdoors(),wind.precipitation()>.2);
    }
    private static void rules(ClientGameTestContext c){
        c.runOnClient(v->{var dim=net.minecraft.world.level.Level.OVERWORLD.identifier();
            for(int yaw:new int[]{-180,-90,0,90,180,450})for(int i=0;i<8;i++){double a=Math.toRadians(yaw+45*i);var wind=new WindView(dim,(float)-Math.sin(a)*.02F,(float)Math.cos(a)*.02F,0,true);check(WindHud.direction(yaw,wind)==i,"World-to-camera direction including wrapped yaw");}
            for(float n:new float[]{0,.0119F,.012F,.0239F,.024F,.04F})check(WindHud.strength(new WindView(dim,n,0,0,true))==(n<.012?0:n<.024?1:2),"Actual strength threshold "+n);
            var full=WindHud.layout(427,240,60,60,true,true);check(full!=null&&!full.compact(),"Readable native text uses full row");
            var compact=WindHud.layout(160,180,40,130,true,true);check(compact!=null&&compact.compact()&&compact.width()==56,"Long translation falls back to icon components");
            check(WindHud.layout(70,100,40,130,true,true)==null,"No layout overlaps reticle/bottom in an impossible viewport");
            for(int width:new int[]{70,160,320,427,640})for(int height:new int[]{100,180,240,320})for(int y:new int[]{20,60,100,150})for(boolean outdoors:new boolean[]{false,true})for(boolean wet:new boolean[]{false,true}){
                var r=WindHud.layout(width,height,y,120,outdoors,wet);if(r==null)continue;
                check(14+r.width()<=width-10&&r.y()+20<=height-46,"Every chosen row keeps viewport/bottom bounds");
                check(14+r.width()<=width/2-10||r.y()+20<=height/2-10||r.y()>=height/2+10,"Every chosen row keeps full reticle exclusion");
            }
        });
    }
    private static void open(ClientGameTestContext c){c.runOnClient(v->{v.player.setNoGravity(true);v.player.setOnGround(false);v.player.setDeltaMovement(Vec3.ZERO);});c.getInput().releaseKey(v->v.keyJump);c.waitTicks(2);c.waitFor(v->Gliding.eligible(v.player));c.getInput().holdKeyFor(v->v.keyJump,1);c.waitFor(v->Gliding.active(v.player));}
    private static void language(ClientGameTestContext c,String code){var reload=c.computeOnClient(v->{v.options.languageCode=code;v.getLanguageManager().setSelected(code);return v.reloadResourcePacks();});c.waitFor(v->reload.isDone());reload.join();c.waitFor(v->v.gui.overlay()==null);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
