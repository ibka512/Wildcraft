package dev.wildcraft.mixin.client;

import dev.wildcraft.client.animation.rig.CombatRigLayer;
import net.minecraft.client.model.player.PlayerCapeModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the native cape and its original flap attached to the qualified source torso. */
@Mixin(PlayerCapeModel.class)
public abstract class CombatRigCapeMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("RETURN"))
    private void wildcraft$waistCape(AvatarRenderState state,CallbackInfo ci){
        var frame=state.getData(CombatRigLayer.FRAME);
        if(frame==null||!(frame.clip().startsWith("equipment_")||frame.clip().startsWith("axe_equipment_")||frame.clip().startsWith("trident_equipment_")||frame.clip().startsWith("mace_equipment_")||frame.clip().startsWith("spear_equipment_")||frame.clip().startsWith("shield_equipment_")||frame.clip().startsWith("bow_equipment_")||frame.clip().startsWith("crossbow_equipment_"))||!frame.clip().contains("_waist_"))return;
        var transform=CombatRigLayer.body(frame);
        var position=transform.getTranslation(new Vector3f());
        var rotation=transform.getUnnormalizedRotation(new org.joml.Quaternionf()).normalize().getEulerAnglesZYX(new Vector3f());
        var scale=transform.getScale(new Vector3f());
        var body=((PlayerCapeModel)(Object)this).body;
        body.x=position.x*16;body.y=position.y*16;body.z=position.z*16;
        body.xRot=rotation.x;body.yRot=rotation.y;body.zRot=rotation.z;
        body.xScale=scale.x;body.yScale=scale.y;body.zScale=scale.z;
    }
}
