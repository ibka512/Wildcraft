package dev.wildcraft.client.mechanics;
import dev.wildcraft.client.art.ArtMesh;
import dev.wildcraft.mechanics.MachineEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
/** Approved geometry selection and conservative visual ground clearance; no simulation writes. */
public final class BottomArt {
    private BottomArt(){}
    public static String name(int kind){return switch(kind){case 1->"fan";case 3->"wing";case 5->"spring";case 8->"buoyancy";default->null;};}
    public static String body(int kind){var name=name(kind);return name==null?"machine":"machine_bottom_"+name;}
    public static String part(int kind,int node,String legacy){return node==1&&name(kind)!=null?name(kind)+"_bottom":legacy;}
    public static float spring(MachineEntity body,float partial,float roll,boolean requested){
        if(!requested)return 0;
        var position=body.getPosition(partial);var transform=new Matrix4f().rotateY((float)Math.toRadians(-body.getYRot())).rotateZ((float)Math.toRadians(roll)).rotateX((float)Math.PI/2);
        var mesh=ArtMesh.get("spring_bottom");AABB start=mesh.movingBounds(transform,0).move(position),end=mesh.movingBounds(transform,1).move(position);
        var sweep=start.minmax(end);double travel=start.minY-end.minY;if(travel<=1e-6)return 0;
        float result=1;
        // Collision shapes, not block occupancy: slabs/stairs and a rotated swept footprint remain meaningful.
        for(var shape:body.level().getBlockCollisions(body,sweep.inflate(.0001))){
            for(var box:shape.toAabbs())if(box.maxX>sweep.minX&&box.minX<sweep.maxX&&box.maxZ>sweep.minZ&&box.minZ<sweep.maxZ&&box.maxY<=start.maxY+.0001){
                result=Math.min(result,(float)Math.clamp((start.minY-box.maxY)/travel,0,1));
            }
        }
        return result;
    }
}
