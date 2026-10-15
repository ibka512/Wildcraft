package dev.wildcraft.test.research.combat;

import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.focus.TimeLease;
import dev.wildcraft.input.Action;
import dev.wildcraft.input.ActionFrame;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

/** Actual render/world scheduling, without Fabric's controlled client-test clock. */
public final class InputNativeProbe {
    private record Start(TimeLease lease,int tick) {}
    private record End(int tick,float rate) {}
    private static int stage,frames,taps,startTick;
    private static long began,timeout;
    private static ActionFrame previous;
    private static CompletableFuture<Start> start;
    private static CompletableFuture<End> end;
    private static TimeLease lease;
    private static int oldFps;
    private InputNativeProbe() {}
    public static void frame(Minecraft c) {
        if(!Boolean.getBoolean("wildcraft.c0.native"))return;
        if(stage==0){timeout=System.nanoTime()+40_000_000_000L;stage=1;}
        if(System.nanoTime()>timeout){
            c.options.keyAttack.setDown(false);
            if(lease!=null&&c.getSingleplayerServer()!=null)c.getSingleplayerServer().execute(lease::close);
            throw new IllegalStateException("C0 native fixture timed out");
        }
        if(c.player==null||c.getSingleplayerServer()==null||c.gui.screen()!=null||c.gui.overlay()!=null)return;
        var server=c.getSingleplayerServer();
        if(stage==1){
            c.player.setNoGravity(true);oldFps=c.options.framerateLimit().get();c.options.framerateLimit().set(120);
            start=new CompletableFuture<>();
            server.execute(()->{try{
                var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setHealth(20);p.getInventory().clearContent();
                var owned=TimeLease.acquire(server,5);check(owned!=null,"Existing time lease can serve another singleplayer reason");
                start.complete(new Start(owned,server.getTickCount()));
            }catch(Throwable e){start.completeExceptionally(e);}});stage=2;return;
        }
        if(stage==2){if(!start.isDone())return;var sample=start.join();lease=sample.lease;startTick=sample.tick;began=System.nanoTime();stage=3;}
        if(stage==3){
            long elapsed=System.nanoTime()-began;frames++;
            // Forty-millisecond taps are shorter than a 200ms world tick.
            long period=elapsed%300_000_000L;c.options.keyAttack.setDown(period>=80_000_000L&&period<120_000_000L);
            c.player.setYRot(c.player.getYRot()+.2F);
            var frame=ActionInput.tickFrame();
            if(frame!=previous){previous=frame;if(frame.fresh(Action.ATTACK)&&frame.released().contains(Action.ATTACK)&&!frame.down(Action.ATTACK))taps++;}
            if(elapsed>2_500_000_000L){
                c.options.keyAttack.setDown(false);end=new CompletableFuture<>();
                server.execute(()->{lease.close();end.complete(new End(server.getTickCount(),server.tickRateManager().tickrate()));});stage=4;
            }
        }else if(stage==4&&end.isDone()){
            var finish=end.join();int ticks=finish.tick-startTick;
            check(finish.rate==20,"Time control restores prior 20 TPS");
            check(ticks>0&&ticks<25,"World actually ran at slow rate");
            check(frames>ticks*4&&taps>0,"Frame input retains complete sub-world-tick taps");
            check(System.getProperty("fabric.client.gametest")==null,"Uses ordinary renderer/world scheduling");
            System.out.println("C0 NATIVE PASS: frames="+frames+", worldTicks="+ticks+", completeSubTickTaps="+taps+", restoredRate="+finish.rate+"; native melee timing still needs C5 design");
            c.options.framerateLimit().set(oldFps);stage=5;c.stop();
        }
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
