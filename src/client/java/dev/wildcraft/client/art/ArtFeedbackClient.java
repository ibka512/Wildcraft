package dev.wildcraft.client.art;

import dev.wildcraft.art.ArtFeedback;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.client.particle.*;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.particles.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Twelve short positional cues and five bounded native-particle presets. No deferred event queue. */
public final class ArtFeedbackClient {
    private record Voice(UUID source,ArtFeedback.Cue cue,SoundInstance sound,long started){}
    private record Fleck(UUID source,Particle particle,Vec3 origin,long started){}
    private static final List<Voice> VOICES=new ArrayList<>();
    private static final List<Fleck> FLECKS=new ArrayList<>();
    private static Identifier dimension;private static long lastSequence;
    private ArtFeedbackClient(){}
    public static void initialize(){
        ClientPlayNetworking.registerGlobalReceiver(ArtFeedback.TYPE,(event,c)->receive(c.client(),event));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->reset(c));
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.level==null||dimension!=null&&!dimension.equals(c.level.dimension().identifier()))reset(c);prune(c);});
    }
    private static void reset(Minecraft c){for(var v:VOICES)c.getSoundManager().stop(v.sound);VOICES.clear();for(var f:FLECKS)f.particle.remove();FLECKS.clear();dimension=null;lastSequence=0;}
    private static void prune(Minecraft c){
        long now=System.nanoTime();
        VOICES.removeIf(v->{boolean gone=now-v.started>1_000_000_000L||now-v.started>100_000_000L&&!sourcePresent(c,v.source);if(gone)c.getSoundManager().stop(v.sound);return gone||now-v.started>60_000_000L&&!c.getSoundManager().isActive(v.sound);});
        FLECKS.removeIf(f->!f.particle.isAlive()||(now-f.started>100_000_000L&&!sourcePresent(c,f.source)||f.particle.getBoundingBox().getCenter().distanceToSqr(f.origin)>.35*.35)&&remove(f));
    }
    // Give entity/chunk tracking a short grace period; removed sources never leave queued feedback.
    private static boolean sourcePresent(Minecraft c,UUID source){
        if(c.level==null)return false;
        if(source.getMostSignificantBits()==0){var pos=net.minecraft.core.BlockPos.of(source.getLeastSignificantBits());return c.level.hasChunkAt(pos)&&c.level.getBlockEntity(pos)!=null;}
        for(var entity:c.level.entitiesForRendering())if(entity.getUUID().equals(source))return !entity.isRemoved();
        return false;
    }
    private static boolean remove(Fleck f){f.particle.remove();return true;}
    public static int voices(){return VOICES.size();}public static int particles(){return FLECKS.size();}
    public static void receive(Minecraft c,ArtFeedback event){
        if(c.level==null||c.player==null||c.isPaused()||!event.dimension().equals(c.level.dimension().identifier()))return;
        if(dimension!=null&&!dimension.equals(event.dimension()))reset(c);
        if(dimension==null)dimension=event.dimension();if(event.sequence()<=lastSequence)return;lastSequence=event.sequence();prune(c);
        if(event.sound()>=0)play(c,event);
        if(event.effect()>=0)particles(c,event);
    }
    private static void play(Minecraft c,ArtFeedback e){
        var cue=ArtFeedback.Cue.values()[e.sound()];if(c.player.position().distanceToSqr(e.position())>cue.distance*cue.distance)return;
        var sameSource=VOICES.stream().filter(v->v.source.equals(e.source())).findFirst().orElse(null);
        if(sameSource!=null){if(sameSource.cue.priority>cue.priority)return;c.getSoundManager().stop(sameSource.sound);VOICES.remove(sameSource);}
        if(VOICES.stream().filter(v->v.cue==cue).count()>=2)return;
        if(VOICES.size()>=6){var lowest=VOICES.stream().min(Comparator.comparingInt((Voice v)->v.cue.priority).thenComparingLong(Voice::started)).orElseThrow();if(lowest.cue.priority>=cue.priority)return;c.getSoundManager().stop(lowest.sound);VOICES.remove(lowest);}
        float pitch=cue==ArtFeedback.Cue.INSTALL||cue==ArtFeedback.Cue.REMOVE?.97F+c.level.getRandom().nextFloat()*.06F:1;
        var p=e.position();var sound=new SimpleSoundInstance(cue.event,cue.category(),cue.volume,pitch,SoundInstance.createUnseededRandom(),p.x,p.y,p.z);
        c.getSoundManager().play(sound);VOICES.add(new Voice(e.source(),cue,sound,System.nanoTime()));
    }
    private static void particles(Minecraft c,ArtFeedback e){
        var status=c.options.particles().get();if(status==ParticleStatus.MINIMAL)return;
        var camera=c.gameRenderer.mainCamera();var cameraPos=camera.position();var toward=e.position().subtract(cameraPos);double d=toward.length();
        if(d<.8||d>16)return;
        var look=new Vec3(camera.forwardVector());if(look.dot(toward.scale(1/d))>Math.cos(.018))return;
        int kind=e.effect(),count=kind==2?5:kind==4?4:6;if(status==ParticleStatus.DECREASED)count/=2;
        count=Math.min(count,Math.min(48-FLECKS.size(),12-(int)FLECKS.stream().filter(f->f.source.equals(e.source())).count()));
        ParticleOptions type=switch(kind){case 0->ParticleTypes.FLAME;case 1->ParticleTypes.SNOWFLAKE;case 2->ParticleTypes.ITEM_SLIME;default->new DustParticleOptions(0xB5BDC4,.25F);};
        for(int i=0;i<count;i++){
            var random=c.level.getRandom();double x=(random.nextDouble()-.5)*.24,y=(random.nextDouble()-.5)*.24,z=(random.nextDouble()-.5)*.24;
            var p=e.position().add(x,y,z);var particle=c.particleEngine.createParticle(type,p.x,p.y,p.z,kind==3?-x*.2:x*.04,kind==0?.008:y*.04,kind==3?-z*.2:z*.04);
            if(particle!=null){particle.setLifetime(kind==1||kind==2?8:6);particle.setParticleSpeed(kind==3?-x*.2:x*.04,kind==0?.008:y*.04,kind==3?-z*.2:z*.04);if(particle instanceof SingleQuadParticle quad)quad.scale(.25F);FLECKS.add(new Fleck(e.source(),particle,e.position(),System.nanoTime()));}
        }
    }
}
