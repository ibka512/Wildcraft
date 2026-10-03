package dev.wildcraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import org.joml.Quaternionf;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/** Approved rigid arms. Includes vanilla arm-centre offsets and uses the existing skin/sleeves. */
public final class GliderPose {
    private GliderPose() { }
    public static void apply(PlayerModel model, boolean slim) {
        arm(model.rightArm, -1, slim);
        arm(model.leftArm, 1, slim);
        model.leftLeg.xRot = model.rightLeg.xRot = -0.08F;
    }
    private static void arm(ModelPart arm, int side, boolean slim) {
        // Vanilla cubes are centred 1 unit outward for standard arms, 0.5 for slim.
        float centre = slim ? 0.5F : 1;
        Vector3f direction = new Vector3f(side * (centre == 1 ? 0.1513897F : 0.2000775F),
                centre == 1 ? -0.6913463F : -0.6852656F, centre == 1 ? -0.7064853F : -0.7002714F);
        Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction);
        Vector3f angles = new Matrix3f().rotation(rotation).getEulerAnglesZYX(new Vector3f());
        arm.xRot = angles.x; arm.yRot = angles.y; arm.zRot = angles.z;
        Vector3f end = rotation.transform(new Vector3f(side * centre, 10, 0));
        arm.x = side * 7.5F - end.x; arm.y = -7 - end.y; arm.z = -7 - end.z;
    }
}
