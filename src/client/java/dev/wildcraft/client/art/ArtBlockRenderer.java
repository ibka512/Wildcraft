package dev.wildcraft.client.art;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.cooking.CookingPotEntity;
import dev.wildcraft.energy.ChargerEntity;
import dev.wildcraft.fabrication.FabricatorEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** A bounded world snapshot shares the authoritative states used by menus. No contents are revealed. */
public final class ArtBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T,ArtBlockRenderer.State> {
    public static final class State extends BlockEntityRenderState {int kind,status,energy;boolean battery;}
    public ArtBlockRenderer(BlockEntityRendererProvider.Context context){}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(T block,State s,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay breaking){
        BlockEntityRenderer.super.extractRenderState(block,s,partial,camera,breaking);
        if(block instanceof CookingPotEntity pot){s.kind=0;s.status=pot.artStatus();}
        else if(block instanceof ChargerEntity charger){s.kind=1;s.status=charger.artStatus();s.energy=charger.artEnergy();s.battery=charger.artBattery();}
        else if(block instanceof FabricatorEntity fabricator){s.kind=2;s.status=fabricator.artStatus();}
    }
    @Override public void submit(State s,PoseStack p,SubmitNodeCollector collector,CameraRenderState camera){
        if(s.kind==1){
            if(s.battery){p.pushPose();p.translate(0,1.48/16,3.8/16);ArtMesh.get("battery_item").submit(p,collector,s.lightCoords,false,0,false,false,s.energy);p.popPose();}
            panel(p,collector,s.lightCoords,"charger",7/16F,1.3F/16,9/16F,3.1F/16,15.671F/16,2*s.status/16F,12/16F,2/16F,2/16F);
        }else if(s.kind==2){
            int tile=s.status==2?1:s.status==3||s.status==4?2:0;
            panel(p,collector,s.lightCoords,"fabricator",7.05F/16,11.5F/16,8.95F/16,13.1F/16,15.211F/16,(8+tile*2)/16F,12/16F,2/16F,2/16F);
        }
    }
    private static void panel(PoseStack p,SubmitNodeCollector c,int light,String atlas,float x0,float y0,float x1,float y1,float z,float u,float v,float w,float h){
        c.submitCustomGeometry(p,RenderTypes.entityCutout(Wildcraft.id("textures/block/"+atlas+"_atlas.png")),(pose,buffer)->{
            float[][] q={{x0,y0,z,u,v+h},{x1,y0,z,u+w,v+h},{x1,y1,z,u+w,v},{x0,y1,z,u,v}};
            for(var point:q)buffer.addVertex(pose,point[0],point[1],point[2]).setColor(-1).setUv(point[3],point[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,1);
        });
    }
}
