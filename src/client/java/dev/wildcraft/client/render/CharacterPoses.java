package dev.wildcraft.client.render;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import dev.wildcraft.network.ClimbVisual;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;

/** Authored limb transforms baked from Blender, excluding preview-only root motion. */
public final class CharacterPoses {
    public static final RenderStateDataKey<Pose> POSE = RenderStateDataKey.create(() -> "wildcraft:character_pose");
    public record Pose(String clip, float frame, float facing) { }
    private record Clips(Map<String, float[][][]> standard, Map<String, float[][][]> slim) { }
    private static final Clips DATA = load();
    private static final Map<Player, Motion> MOTION = new WeakHashMap<>();
    private static final class Motion { double x,y,z; float distance; boolean gliding, climbing; long opened, released; String clip; }
    private CharacterPoses() { }
    private static Clips load() {
        var input = CharacterPoses.class.getResourceAsStream("/assets/wildcraft/animations/character-poses.json");
        if (input == null) throw new IllegalStateException("Missing approved character poses");
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Clips.class);
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    public static void initialize() { ClientPlayConnectionEvents.DISCONNECT.register((h,c) -> MOTION.clear()); }
    public static void extract(Player p, AvatarRenderState state, float partial) {
        state.setData(POSE, null);
        var m = MOTION.computeIfAbsent(p, ignored -> { var a=new Motion();a.x=p.getX();a.y=p.getY();a.z=p.getZ();return a; });
        double distance = Math.sqrt(Math.pow(p.getX()-m.x,2)+Math.pow(p.getY()-m.y,2)+Math.pow(p.getZ()-m.z,2));
        m.x=p.getX();m.y=p.getY();m.z=p.getZ();
        if (distance < 1) m.distance += (float)distance;
        if (Gliding.active(p)) {
            if (!m.gliding) m.opened=System.nanoTime();
            m.gliding=true;m.climbing=false;
            float elapsed = (System.nanoTime()-m.opened)/1_000_000_000F;
            state.setData(POSE,new Pose("glide",Math.min(9,elapsed*30),state.bodyRot));
        } else {
            m.gliding=false;
            ClimbVisual visual=p.getAttached(Climbing.VISUAL);
            if (visual != null && visual.active() && p.isAlive() && !p.isSpectator() && !p.isInvisible()) {
                if (!m.climbing) { m.distance=0; m.clip=null; }
                m.climbing=true; m.released=0;
                String clip=switch(visual.motion()) {case 1->"up";case 2->"down";case 3->"left";case 4->"right";case 5->"mantle";default->"hold";};
                if (!clip.equals(m.clip)) { m.distance=0; m.clip=clip; }
                Direction face=Direction.values()[Math.clamp(visual.face(),2,5)];
                state.setData(POSE,new Pose(clip,clip.equals("hold")?0:m.distance*18,face.toYRot()));
                state.yRot=Math.clamp(net.minecraft.util.Mth.wrapDegrees(state.yRot+state.bodyRot-face.toYRot()),-70,70);
                state.bodyRot=face.toYRot();
            } else {
                if (m.climbing) m.released=System.nanoTime();
                m.climbing=false;m.clip=null;
                float elapsed=(System.nanoTime()-m.released)/1_000_000_000F;
                if (m.released!=0 && elapsed<.20F && p.isAlive() && !p.isUsingItem()
                        && p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty())
                    state.setData(POSE,new Pose("release",elapsed*30,state.bodyRot));
            }
        }
    }
    public static void apply(PlayerModel model, AvatarRenderState state) {
        Pose pose=state.getData(POSE);if(pose==null || state.isInvisible || state.isSpectator)return;
        boolean slim=state.skin.model()==PlayerModelType.SLIM;
        float[][][] frames=(slim?DATA.slim:DATA.standard).get(pose.clip);
        float frame=(pose.clip.equals("glide") || pose.clip.equals("release") || pose.clip.equals("mantle"))?Math.min(frames.length-1,pose.frame):pose.frame%(frames.length-1);
        int a=(int)frame,b=Math.min(a+1,frames.length-1);float t=frame-a;
        ModelPart[] limbs={model.rightArm,model.leftArm,model.rightLeg,model.leftLeg};
        for(int i=0;i<4;i++) {
            float[] value=new float[6];for(int k=0;k<6;k++) value[k]=frames[a][i][k]+t*(frames[b][i][k]-frames[a][i][k]);
            var part=limbs[i];part.x=value[0];part.y=value[1];part.z=value[2];part.xRot=value[3];part.yRot=value[4];part.zRot=value[5];
        }
    }
}
