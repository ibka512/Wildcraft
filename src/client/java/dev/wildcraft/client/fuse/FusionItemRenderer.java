package dev.wildcraft.client.fuse;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.fuse.*;
import dev.wildcraft.mixin.client.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.function.Consumer;

/** Adds a bounded native material model after extraction, sharing the host's real display transform. */
public final class FusionItemRenderer implements SpecialModelRenderer<FusionItemRenderer.Part>{
    public static final FusionItemRenderer INSTANCE=new FusionItemRenderer();
    public record Part(ItemStackRenderState model,float x,float y,float z,float scale,float cx,float cy,float cz){}
    private FusionItemRenderer(){}
    public static void append(ItemModelResolver resolver,ItemStackRenderState out,ItemStack host,ItemDisplayContext context,Level level,ItemOwner owner,int seed){
        var v=host.get(FusionContent.VIEW);if(v==null||out.isEmpty())return;
        var child=FusionPresentation.model(resolver,v,level,owner,seed);
        if(child.isEmpty())return;var bounds=child.getModelBoundingBox();double size=Math.max(bounds.getXsize(),Math.max(bounds.getYsize(),bounds.getZsize()));if(!Double.isFinite(size)||size<.001||size>100)return;
        var center=bounds.getCenter();var base=((FusionStateAccessor)out).wildcraft$layers()[0];var transform=(FusionLayerAccessor)base;
        out.appendModelIdentityElement(v);var added=out.newLayer();added.setItemTransform(transform.wildcraft$transform());added.setLocalTransform(new Matrix4f(transform.wildcraft$local()));added.setUsesBlockLight(true);
        boolean shield=host.is(Items.SHIELD),axe=host.is(net.minecraft.tags.ItemTags.AXES),arrow=host.is(Items.ARROW);
        float scale=(float)((shield?.24:axe?.30:arrow?.20:.28)/size);
        float x=shield?.25F:axe?.66F:arrow?.77F:.8F,y=shield?-.54F:axe?.70F:arrow?.77F:.82F;
        float z=shield?-.125F:.53125F;float penetration=shield?.006F:.008F;
        z+=(shield?-1:1)*(bounds.getZsize()*scale/2-penetration);
        var part=new Part(child,x,y,z,scale,(float)center.x,(float)center.y,(float)center.z);added.setupSpecialModel(INSTANCE,part);
        float hx=(float)bounds.getXsize()*scale/2,hy=(float)bounds.getYsize()*scale/2,hz=(float)bounds.getZsize()*scale/2;
        final float cx=x,cy=y,cz=z;
        added.setExtents(()->new Vector3fc[]{new Vector3f(cx-hx,cy-hy,cz-hz),new Vector3f(cx+hx,cy+hy,cz+hz)});

    }
    @Override public void submit(Part p,PoseStack poses,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){if(p==null)return;poses.pushPose();poses.translate(p.x,p.y,p.z);poses.scale(p.scale,p.scale,p.scale);poses.translate(-p.cx,-p.cy,-p.cz);p.model.submit(poses,collector,light,overlay,outline);poses.popPose();}
    @Override public void getExtents(Consumer<Vector3fc> output){output.accept(new Vector3f(-.5f,-.5f,-.5f));output.accept(new Vector3f(.6f,.6f,.5f));}
    @Override public Part extractArgument(ItemStack stack){return null;}
}
