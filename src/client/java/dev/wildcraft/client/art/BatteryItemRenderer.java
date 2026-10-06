package dev.wildcraft.client.art;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import dev.wildcraft.energy.Batteries;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.*;
import java.util.function.Consumer;
public final class BatteryItemRenderer implements SpecialModelRenderer<Integer> {
    @Override public void submit(Integer energy,PoseStack poses,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){
        poses.pushPose();poses.translate(-.5,-.5,-.5);ArtMesh.get("battery_item").submit(poses,collector,light,false,0,false,false,energy==null?0:energy);poses.popPose();
    }
    @Override public void getExtents(Consumer<Vector3fc> out){ArtMesh.get("battery_item").extents(out,-.5F);}
    @Override public Integer extractArgument(ItemStack stack){return Batteries.energy(stack);}
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Integer>{
        public static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public SpecialModelRenderer<Integer> bake(SpecialModelRenderer.BakingContext context){return new BatteryItemRenderer();}
        @Override public MapCodec<Unbaked> type(){return CODEC;}
    }
}
