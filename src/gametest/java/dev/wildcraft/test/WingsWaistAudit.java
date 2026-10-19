package dev.wildcraft.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Independent observations at native wing-layer and model submission boundaries. */
public final class WingsWaistAudit {
    public static boolean enabled;
    private static String family="equipment_";
    public static int models,poses,foil,balanced,nativeModels;
    public static float maxError;
    private static Matrix4f parent;
    private static AvatarRenderState owner;
    public static void begin(){begin("equipment_");}
    public static void begin(String sourceFamily){family=sourceFamily;enabled=true;models=poses=foil=balanced=nativeModels=0;maxError=0;parent=null;owner=null;}
    /** Real chest slot and native texture/foil variants, after genuine equipment-use registration. */
    public static void equip(net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext w,String sourceFamily){
        w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var wings=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA);wings.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,p.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT||!p.getOffhandItem().isEmpty());p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,wings);p.inventoryMenu.broadcastChanges();});
        c.waitFor(v->v.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(net.minecraft.world.item.Items.ELYTRA));
        c.runOnClient(v->{var s=RigAudit.fixture;boolean left=v.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT,carry=!v.player.getOffhandItem().isEmpty();var other=new net.minecraft.core.ClientAsset.ResourceTexture(net.minecraft.resources.Identifier.withDefaultNamespace("entity/equipment/wings/elytra"));RigAudit.fixture=new net.minecraft.world.entity.player.PlayerSkin(s.body(),left?other:carry?s.body():null,left?s.body():null,s.model(),false);RigAudit.cape=left||carry;begin(sourceFamily);});
        c.waitTicks(3);
    }
    public static void enter(PoseStack stack,HumanoidRenderState state){if(!enabled||!(state instanceof AvatarRenderState a))return;parent=new Matrix4f(stack.last().pose());owner=a;}
    private static void same(Matrix4f expected,Matrix4f actual,String message){float[] a=expected.get(new float[16]),b=actual.get(new float[16]);for(int i=0;i<16;i++){float e=Math.abs(a[i]-b[i]);maxError=Math.max(maxError,e);if(e>2e-5F)throw new AssertionError(message+" "+e);}}
    public static void exit(PoseStack stack,HumanoidRenderState state){if(!enabled||state!=owner)return;same(parent,stack.last().pose(),"Native wings leaked torso transform into following layers");balanced++;owner=null;parent=null;}
    public static void submit(Object model,Object state,PoseStack stack,RenderType type){
        if(!enabled||!(model instanceof ElytraModel)||!(state instanceof AvatarRenderState a)||a!=owner)return;
        var frame=a.getData(CombatRigLayer.FRAME);var expected=new Matrix4f(parent);
        boolean source=frame!=null&&frame.clip().startsWith(family)&&frame.clip().contains("_waist_");
        if(source)expected.mul(CombatRigLayer.body(frame));expected.translate(0,0,.125F);
        same(expected,stack.last().pose(),"Actually submitted native wings detached from torso/native origin");
        var binding=((dev.wildcraft.test.research.mixin.RenderSetupTexturesAccessor)(Object)((dev.wildcraft.test.research.mixin.RenderTypeStateAccessor)(Object)type).wildcraft$setup()).wildcraft$textures().get("Sampler0");
        var texture=binding==null?null:((dev.wildcraft.test.research.mixin.TextureBindingLocationAccessor)binding).wildcraft$location();
        if(type.toString().toLowerCase(java.util.Locale.ROOT).contains("glint")){if(!a.chestEquipment.hasFoil())throw new AssertionError("Native non-foil wings unexpectedly received glint");if(source)foil++;}
        Identifier wanted=a.skin.elytra()!=null?a.skin.elytra().texturePath():a.showCape&&a.skin.cape()!=null?a.skin.cape().texturePath():Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");
        if(!wanted.equals(texture))throw new AssertionError("Native wings lost original texture priority "+wanted+" actual="+texture);
        if(source)models++;else nativeModels++;
    }
    public static void pose(ElytraModel model,HumanoidRenderState state){
        if(!enabled||!(state instanceof AvatarRenderState a))return;
        var left=model.root().getChild("left_wing");var right=model.root().getChild("right_wing");
        if(Math.abs(left.xRot-state.elytraRotX)>1e-6F||Math.abs(left.yRot-state.elytraRotY)>1e-6F||Math.abs(left.zRot-state.elytraRotZ)>1e-6F||Math.abs(right.yRot+left.yRot)>1e-6F||Math.abs(right.zRot+left.zRot)>1e-6F||Math.abs(right.xRot-left.xRot)>1e-6F||left.y!=(state.isCrouching?3F:0F)||right.y!=left.y)throw new AssertionError("Native wing folding/crouching pose changed");
        var frame=a.getData(CombatRigLayer.FRAME);if(frame!=null&&frame.clip().startsWith(family)&&frame.clip().contains("_waist_"))poses++;
    }
}
