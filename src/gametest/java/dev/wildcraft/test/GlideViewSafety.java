package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.CombatRig;
import dev.wildcraft.client.render.ParagliderMesh;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Uses the actual submitted poses and native HUD projection; no duplicated view-offset formula. */
public final class GlideViewSafety {
    private record Triangle(Vector3f a,Vector3f b,Vector3f c){}
    private static final List<String> ROWS=new ArrayList<>();
    public static void begin(){ROWS.clear();ROWS.add("variant,main_arm,width,height,world_fov_option,native_hud_fov,bank_degrees,rays,geometry_hits");}
    public static void check(Minecraft client){
        var frame=GlideRigAudit.viewFrame;if(frame==null||GlideRigAudit.viewPose==null||GlideRigAudit.canopyPose==null)throw new AssertionError("Actual first-person poses not submitted");
        var triangles=new ArrayList<Triangle>();var data=frame.data();var skin=new Matrix4f[data.skinBones()];var normal=new Matrix3f[skin.length];
        for(int i=0;i<skin.length;i++){skin[i]=new Matrix4f(frame.global()[i]).mul(new Matrix4f().set(data.inverseBind()[i]));normal[i]=skin[i].normal(new Matrix3f());}
        for(var mesh:data.meshes())if(mesh.part().endsWith("arm")){
            var points=CombatRig.deform(mesh,skin,normal);var positions=new Vector3f[points.length/8];
            for(int i=0;i<positions.length;i++)positions[i]=GlideRigAudit.viewPose.transformPosition(new Vector3f(points[i*8],points[i*8+1],points[i*8+2]));
            for(int i=0;i<mesh.indices().length;i+=3)triangles.add(new Triangle(positions[mesh.indices()[i]],positions[mesh.indices()[i+1]],positions[mesh.indices()[i+2]]));
        }
        try{
            var field=ParagliderMesh.class.getDeclaredField("QUADS");field.setAccessible(true);var quads=(float[][])field.get(null);
            for(int i=114;i<quads.length;i++){
                var q=quads[i];var p=new Vector3f[4];for(int j=0;j<4;j++)p[j]=GlideRigAudit.canopyPose.transformPosition(new Vector3f(q[j*5],q[j*5+1],q[j*5+2]));
                triangles.add(new Triangle(p[0],p[1],p[2]));triangles.add(new Triangle(p[0],p[2],p[3]));
            }
        }catch(ReflectiveOperationException e){throw new AssertionError("Actual approved canopy mesh unavailable",e);}
        var game=client.gameRenderer.gameRenderState();var camera=game.levelRenderState.cameraRenderState;var projection=new Projection();
        projection.setupPerspective(.05F,camera.depthFar,camera.hudFov,game.windowRenderState.width,game.windowRenderState.height);var inverse=projection.getMatrix(new Matrix4f()).invert();int hits=0;
        for(int y=0;y<7;y++)for(int x=0;x<7;x++){
            var far=inverse.transform(new Vector4f(2*(.35F+x*.05F)-1,1-2*(.3F+y*.05F),1,1));var direction=new Vector3f(far.x/far.w,far.y/far.w,far.z/far.w).normalize();
            boolean hit=false;for(var t:triangles)if(intersects(direction,t)){hit=true;break;}if(hit)hits++;
        }
        var canopy=game.levelRenderState.playerRenderState.avatarRenderState.getData(dev.wildcraft.client.animation.rig.GlideRig.CANOPY);
        float bank=canopy==null?0:canopy.bank();ROWS.add(data.variant()+","+client.player.getMainArm()+","+game.windowRenderState.width+","+game.windowRenderState.height+","+client.options.fov().get()+","+camera.hudFov+","+bank+",49,"+hits);
        if(hits!=0)throw new AssertionError("Actual first-person geometry obscures center safe rectangle: "+hits+"/49; "+ROWS.getLast());
    }
    private static boolean intersects(Vector3f d,Triangle t){
        var e1=new Vector3f(t.b).sub(t.a);var e2=new Vector3f(t.c).sub(t.a);var h=new Vector3f(d).cross(e2);float det=e1.dot(h);if(java.lang.Math.abs(det)<1e-7)return false;
        var s=new Vector3f(t.a).negate();float u=s.dot(h)/det;if(u<0||u>1)return false;var q=new Vector3f(s).cross(e1);float v=d.dot(q)/det;if(v<0||u+v>1)return false;return e2.dot(q)/det>.05;
    }
    public static void save(){try{java.nio.file.Files.writeString(java.nio.file.Path.of("glide-view-safe-rays.csv"),String.join("\n",ROWS)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
}
