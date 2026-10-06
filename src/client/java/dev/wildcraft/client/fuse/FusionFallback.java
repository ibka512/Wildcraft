package dev.wildcraft.client.fuse;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.Wildcraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.joml.*;
import java.util.function.Consumer;
/** Neutral visual failure marker; never resolves an item model or alters fusion data. */
public final class FusionFallback implements SpecialModelRenderer<Void>{
    private static final FusionFallback INSTANCE=new FusionFallback();
    public static ItemStackRenderState model(){var s=new ItemStackRenderState();var l=s.newLayer();l.setupSpecialModel(INSTANCE,null);l.setExtents(()->new Vector3fc[]{new Vector3f(0,0,0),new Vector3f(1,1,.03125F)});return s;}
    @Override public void submit(Void argument,PoseStack p,SubmitNodeCollector c,int light,int overlay,boolean foil,int outline){c.submitCustomGeometry(p,RenderTypes.entityCutout(Wildcraft.id("textures/item/fuse_unavailable.png")),(pose,b)->{
        float[][] q={{0,0,0,0,1},{1,0,0,1,1},{1,1,0,1,0},{0,1,0,0,0}};
        for(int side=0;side<2;side++)for(int n=0;n<4;n++){var v=q[side==0?n:3-n];b.addVertex(pose,v[0],v[1],side*.03125F).setColor(-1).setUv(v[3],v[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,side==0?1:-1);}
    });}
    @Override public void getExtents(Consumer<Vector3fc> out){out.accept(new Vector3f(0,0,0));out.accept(new Vector3f(1,1,.03125F));}
    @Override public Void extractArgument(ItemStack stack){return null;}
}
