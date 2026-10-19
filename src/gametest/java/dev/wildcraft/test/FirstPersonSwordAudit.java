package dev.wildcraft.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.FirstPersonSword;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Actual first-person native base-item queue and deferred skin callbacks; test source set only. */
public final class FirstPersonSwordAudit {
    public static boolean enabled,inSwordItem,inShieldItem,pendingBase;
    public static int arms,skinGpu,swordItems,shieldItems,swordQuads,movingArms,crouchingArms,shieldGpu;
    public static String clip,variant,texture;
    public static float maxPalmGap,maxQuadGap,minTime,maxTime;
    public static Vector3f expectedGrip,expectedPalm;
    public static Matrix4f parent;public static float lastTime,lastPartial;public static int lastAge,lastDuration;
    private FirstPersonSwordAudit() {}
    public static void begin(){enabled=true;arms=skinGpu=swordItems=shieldItems=swordQuads=movingArms=crouchingArms=shieldGpu=0;clip=variant=texture=null;maxPalmGap=maxQuadGap=0;minTime=99;maxTime=-1;}
    public static void arms(FirstPersonSword.Frame f,AvatarRenderState a){
        if(!enabled)return;lastTime=f.sourceTime();lastPartial=a.getDataOrDefault(dev.wildcraft.client.animation.MeleePoses.PARTIAL,0F);var view=a.getData(dev.wildcraft.client.animation.MeleePoses.LAYER);lastAge=view.age();lastDuration=view.duration();arms++;var player=net.minecraft.client.Minecraft.getInstance().player;if(player.getDeltaMovement().horizontalDistanceSqr()>.0004)movingArms++;if(player.isCrouching())crouchingArms++;clip=f.clip();variant=f.data().variant();texture=a.skin.body().texturePath().toString();minTime=Math.min(minTime,f.sourceTime());maxTime=Math.max(maxTime,f.sourceTime());
        for(int side=0;side<2;side++){
            var bone=f.sample().bones()[side*3+1];var q=new org.joml.Quaternionf(bone[3],bone[4],bone[5],bone[6]);
            var p=q.transform(new Vector3f(0,0,-.2425F)).add(bone[0],bone[1],bone[2]);var g=f.sample().grips()[side];float gap=p.distance(g[0],g[1],g[2]);
            if(gap>.0015F)throw new AssertionError("Interpolated physical skin palm left authored grip "+gap);maxPalmGap=Math.max(maxPalmGap,gap);
        }
    }
    public static void item(FirstPersonSword.Frame f,AvatarRenderState a,InteractionHand hand,PoseStack base){
        if(!enabled)return;
        if(hand==InteractionHand.MAIN_HAND){
            swordItems++;inSwordItem=true;pendingBase=true;var b=f.sample().bones()[a.mainArm==HumanoidArm.RIGHT?1:4];
            var palm=new org.joml.Quaternionf(b[3],b[4],b[5],b[6]).transform(new Vector3f(0,0,-.2425F)).add(b[0],b[1],b[2]);
            expectedPalm=base.last().pose().transformPosition(FirstPersonSword.camera(f.data()).transformPosition(palm));
            expectedGrip=base.last().pose().transformPosition(FirstPersonSword.grip(f,a.mainArm));
        }else {shieldItems++;inShieldItem=true;}
    }
    public static void quads(PoseStack poses){
        if(!enabled||!inSwordItem||!pendingBase)return;pendingBase=false;
        var grip=poses.last().pose().transformPosition(new Vector3f(.23F,.23F,.5F));float gap=grip.distance(expectedGrip);
        if(gap>.00003F||grip.distance(expectedPalm)>.0015F)throw new AssertionError("Actual native base sword quads left source hilt/physical palm: "+gap+" physical="+grip.distance(expectedPalm));
        swordQuads++;maxQuadGap=Math.max(maxQuadGap,gap);
    }
}
