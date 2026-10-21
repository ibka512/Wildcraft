package dev.wildcraft.test.industry.mixin;
import dev.wildcraft.test.industry.NativeGroundAudit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Test-only read observations distinguish real transient collision from a packet-supplied ground flag. */
@Mixin(Entity.class)
public abstract class NativeGroundWriterMixin {
 @Inject(method="move",at=@At("HEAD")) private void wildcraft$moveHead(MoverType type,Vec3 movement,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-move-head",movement,null,null);}
 @Inject(method="move",at=@At("RETURN")) private void wildcraft$moveReturn(MoverType type,Vec3 movement,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-move-return",movement,null,null);}
 @Inject(method="setPos(DDD)V",at=@At("HEAD")) private void wildcraft$positionHead(double x,double y,double z,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-setpos-head",null,null,y);}
 @Inject(method="setPos(DDD)V",at=@At("RETURN")) private void wildcraft$positionReturn(double x,double y,double z,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-setpos-return",null,null,y);}
 @Inject(method="setOnGround",at=@At("HEAD")) private void wildcraft$groundHead(boolean value,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-ground-head",null,value,null);}
 @Inject(method="setOnGroundWithMovement(ZLnet/minecraft/world/phys/Vec3;)V",at=@At("HEAD")) private void wildcraft$groundMovementHead(boolean value,Vec3 movement,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-ground-movement-head",movement,value,null);}
 @Inject(method="setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V",at=@At("HEAD")) private void wildcraft$groundCollisionHead(boolean value,boolean horizontal,Vec3 movement,CallbackInfo ci){NativeGroundAudit.detail((Entity)(Object)this,"native-ground-collision-head",movement,value,null);}
}
