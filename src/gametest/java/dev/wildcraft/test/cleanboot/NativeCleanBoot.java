package dev.wildcraft.test.cleanboot;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.focus.FocusSettingsScreen;
import dev.wildcraft.client.input.WildcraftBindings;
import dev.wildcraft.player.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

/** A tiny ordinary-menu observer, no research registry, fixtures, commands or accelerated gametest hooks. */
public final class NativeCleanBoot {
    private static final List<Map<String,Object>> EVENTS=new ArrayList<>();
    private static CompletableFuture<Map<String,Object>> pending;
    private static Map<String,Object> before;
    private static long boot,deadline;
    private static int phase;
    private static String folder;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static List<Button> buttons(GuiEventListener root){var result=new ArrayList<Button>();if(root instanceof Button b)result.add(b);if(root instanceof ContainerEventHandler c)for(var child:c.children())result.addAll(buttons(child));return result;}
    private static void press(Minecraft c,String key){
        String text=Component.translatable(key).getString();var matches=buttons(c.gui.screen()).stream().filter(b->b.active&&b.visible&&b.getMessage().getString().equals(text)).toList();
        check(matches.size()==1,"Expected one native button "+key+" on "+c.gui.screen().getClass().getSimpleName()+", actual="+buttons(c.gui.screen()).stream().map(b->b.getMessage().getString()).toList());
        EVENTS.add(Map.of("screen",c.gui.screen().getClass().getSimpleName(),"buttonKey",key,"buttonText",text));matches.getFirst().onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    private static void key(KeyMapping key){key.setDown(true);KeyMapping.click(InputConstants.getKey(key.saveString()));}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-clean-"+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static CompletableFuture<Map<String,Object>> inspect(Minecraft c){
        var f=new CompletableFuture<Map<String,Object>>();var s=c.getSingleplayerServer();s.execute(()->{try{
            check(s.tickRateManager().tickrate()==20,"Clean world stays at native20TPS");var p=s.getPlayerList().getPlayers().getFirst();
            check(p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Native UI creates normal survival");check(p.hasAttached(PlayerStamina.DATA),"Production JOIN initialized actual stamina without test registry");
            var stamina=PlayerStamina.get(p);check(stamina.highestLevel()==0&&p.experienceLevel==0&&stamina.stamina()==stamina.capacity(),"Empty fresh player has native full starting stamina/zero XP peak");
            check(p.getInventory().isEmpty()&&GliderEquipment.get(p).isEmpty()&&p.inventoryMenu.getCarried().isEmpty(),"No provided items or synthetic kit in clean world");
            check(p.inventoryMenu.slots.size()==47&&p.inventoryMenu.slots.stream().filter(slot->slot instanceof GliderSlot).count()==1,"Production inventory has exactly one extra real glider slot");
            f.complete(Map.of("uuid",p.getUUID().toString(),"name",p.getName().getString(),"peak",stamina.highestLevel(),"stamina",stamina.stamina(),"capacity",stamina.capacity(),"inventorySlots",p.inventoryMenu.slots.size(),"inventoryEmpty",true,"gliderEmpty",true,"worldTick",s.overworld().getGameTime(),"nativeRate",s.tickRateManager().tickrate()));
        }catch(Throwable e){f.completeExceptionally(e);}});return f;
    }
    private static void write(Minecraft c,String status,String message){
        var data=new LinkedHashMap<String,Object>();data.put("status",status);data.put("message",message);data.put("phase",phase);data.put("worldFolder",folder);data.put("events",EVENTS);data.put("beforeSave",before);data.put("acceleratedGameTest",false);data.put("researchRegistryLoaded",false);data.put("providedItemsOrPositionWrites",false);data.put("manualSurvivalVerified",false);data.put("foregroundPerformanceQualified",false);
        try{Files.writeString(c.gameDirectory.toPath().resolve("c7-clean-boot.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    public static void frame(Minecraft c){
        long now=System.nanoTime();if(boot==0){boot=now;deadline=now+2_000_000_000L;}
        try{
            check(System.getProperty("fabric.client.gametest")==null,"Clean ordinary client cannot use accelerated client gametest");check(now-boot<240_000_000_000L,"Clean native-menu flow timed out at phase"+phase+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getName()));
            check(!FabricLoader.getInstance().isModLoaded("wildcraft-test")&&!FabricLoader.getInstance().isModLoaded("wildcraft-c7-performance"),"Old test mods cannot be on clean runtime classpath");
            check(!FabricLoader.getInstance().isModLoaded("fabric-gametest-api-v1")&&!FabricLoader.getInstance().isModLoaded("fabric-client-gametest-api-v1"),"Clean package menu flow excludes optional gametest API modules, not just their accelerated flags");
            check(BuiltInRegistries.ENTITY_TYPE.keySet().stream().noneMatch(id->id.getNamespace().equals("wildcraft-test")),"Old research entity registry must be absent");
            if(c.gui.overlay()!=null)return;
            if(phase==0&&now>=deadline&&c.gui.screen() instanceof TitleScreen){shot(c,"title");press(c,"menu.singleplayer");phase=1;
            }else if(phase==1){
                if(c.gui.screen() instanceof CreateWorldScreen)phase=2;else if(c.gui.screen() instanceof SelectWorldScreen){press(c,"selectWorld.create");phase=2;}
            }else if(phase==2&&c.gui.screen() instanceof CreateWorldScreen create){
                var state=create.getUiState();check(!state.isAllowCommands()&&!state.isBonusChest()&&!state.isHardcore(),"Default clean UI uses no commands/bonus chest/hardcore");
                state.setName("C7CleanBoot");state.setSeed("20261020");folder=state.getTargetFolder();check(!Files.exists(c.gameDirectory.toPath().resolve("saves").resolve(folder)),"Never overwrite an existing world");shot(c,"native-create-world");press(c,"selectWorld.create");phase=3;
            }else if(phase==3&&c.player!=null&&c.level!=null&&c.getSingleplayerServer()!=null&&c.gui.screen()==null&&c.player.tickCount>=25){pending=inspect(c);phase=4;
            }else if(phase==4&&pending.isDone()){before=pending.join();EVENTS.add(Map.of("firstWorldInspection",before));shot(c,"fresh-survival");key(c.options.keyInventory);deadline=now+500_000_000L;phase=5;
            }else if(phase==5&&now>=deadline&&c.gui.screen() instanceof InventoryScreen){c.options.keyInventory.setDown(false);check(c.player.inventoryMenu.slots.size()==47,"Native E opened actual extended inventory");shot(c,"real-glider-slot-empty");c.gui.screen().onClose();phase=6;
            }else if(phase==6&&c.gui.screen()==null){key(WildcraftBindings.SETTINGS);deadline=now+500_000_000L;phase=7;
            }else if(phase==7&&now>=deadline&&c.gui.screen() instanceof FocusSettingsScreen){WildcraftBindings.SETTINGS.setDown(false);shot(c,"real-F8-settings");press(c,"gui.done");phase=8;
            }else if(phase==8&&c.gui.screen()==null){c.pauseGame(false);deadline=now+500_000_000L;phase=9;
            }else if(phase==9&&now>=deadline&&c.gui.screen() instanceof PauseScreen){shot(c,"native-save-menu");press(c,"menu.returnToMenu");phase=10;
            }else if(phase==10&&c.gui.screen() instanceof TitleScreen&&c.level==null&&c.getSingleplayerServer()==null){check(Files.isRegularFile(c.gameDirectory.toPath().resolve("saves").resolve(folder).resolve("level.dat")),"Native save menu actually wrote level.dat");press(c,"menu.singleplayer");phase=11;
            }else if(phase==11&&c.gui.screen() instanceof SelectWorldScreen screen){
                var lists=screen.children().stream().filter(child->child instanceof WorldSelectionList).map(child->(WorldSelectionList)child).toList();if(lists.size()!=1)return;var list=lists.getFirst();var entries=list.children().stream().filter(e->e instanceof WorldSelectionList.WorldListEntry).map(e->(WorldSelectionList.WorldListEntry)e).filter(e->e.getLevelSummary().getLevelId().equals(folder)).toList();if(entries.isEmpty())return;check(entries.size()==1,"Saved normal world appears once in native selection");list.setSelected(entries.getFirst());shot(c,"native-saved-world-selection");press(c,"selectWorld.select");phase=12;
            }else if(phase==12&&c.player!=null&&c.level!=null&&c.getSingleplayerServer()!=null&&c.gui.screen()==null&&c.player.tickCount>=25){pending=inspect(c);phase=13;
            }else if(phase==13&&pending.isDone()){
                var after=pending.join();for(String k:List.of("uuid","name","peak","capacity","inventorySlots","inventoryEmpty","gliderEmpty"))check(Objects.equals(before.get(k),after.get(k)),"Native saved kit changed on reopen: "+k);EVENTS.add(Map.of("afterNativeReopen",after));shot(c,"native-reopened-survival");write(c,"complete","Normal menu creation/E/F8/save/selected-world reopening, no research registry or provided resources");phase=14;c.stop();
            }
        }catch(RuntimeException|AssertionError e){c.options.keyInventory.setDown(false);WildcraftBindings.SETTINGS.setDown(false);shot(c,"failure");write(c,"failed",e.toString());throw e;}
    }
}
