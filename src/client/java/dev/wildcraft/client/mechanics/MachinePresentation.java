package dev.wildcraft.client.mechanics;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;

public final class MachinePresentation {
    public static final KeyMapping TOGGLE = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.wildcraft.machine_toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_R,
            KeyMapping.Category.register(Wildcraft.id("mechanics"))));
    private MachinePresentation() { }
    public static int selectedNode(MachineEntity body) {
        var c = Minecraft.getInstance();
        if (c.player == null || c.player.isSpectator() || c.gui.screen() != null || c.gui.hud.isHidden()) return -1;
        if (!(c.hitResult instanceof EntityHitResult hit) || hit.getEntity() != body || c.player.distanceToSqr(body) > 25) return -1;
        var relative = hit.getLocation().subtract(body.position());
        if (MachineNodes.panel(relative, body.getYRot())) return -1;
        return MachineNodes.nearest(relative, body.getYRot());
    }
    public static boolean canPreview(MachineEntity body, int node) {
        var p = Minecraft.getInstance().player;
        return p != null && p.isAlive() && node >= 0 && !body.enabled() && body.kind(node) == 0 && MachineEntity.accepts(p.getMainHandItem()) && !p.getMainHandItem().is(MechanicsContent.SPENT_ROCKET)
                && (!p.getMainHandItem().is(EnergyContent.BATTERY) || body.batteryNode() < 0);
    }
    public static void initialize() {
        ClientTickEvents.START_CLIENT_TICK.register(c -> {
            while (TOGGLE.consumeClick()) if (c.player != null && c.player.getVehicle() instanceof MachineEntity
                    && c.gui.screen() == null && ClientPlayNetworking.canSend(MachineToggle.TYPE)) ClientPlayNetworking.send(new MachineToggle());
        });
        HudElementRegistry.attachElementAfter(Wildcraft.id("meals"), Wildcraft.id("machine"), (g, t) -> {
            var c = Minecraft.getInstance(); var p = c.player;
            if (p == null || !p.isAlive() || p.isSpectator() || c.gui.hud.isHidden() || c.gui.screen() != null) return;
            MachineEntity body = p.getVehicle() instanceof MachineEntity vehicle ? vehicle
                    : c.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof MachineEntity target ? target : null;
            if (body == null || p.distanceToSqr(body) > 25) return;
            int node = selectedNode(body);
            String help = p.getVehicle() == body ? "drive_hint"
                    : MachineEntity.accepts(p.getMainHandItem()) ? "install_hint" : "ground_hint";
            Component label = Component.translatable("machine.wildcraft.status", body.batteryNode()<0?"—":Integer.toString(body.energy()), body.enabled()
                    ? Component.translatable(body.working() ? "machine.wildcraft.running" : "machine.wildcraft.waiting")
                    : Component.translatable("machine.wildcraft.off"));
            if(node>=0 && body.kind(node)==4) label=label.copy().append(" · ").append(Component.translatable("machine.wildcraft.fuel",body.rocketFuel(node),RocketData.DURATION));
            if(node>=0 && body.kind(node)==5) label=label.copy().append(" · ").append(Component.translatable("machine.wildcraft.cooldown",body.springCooldown(node)));
            Component hint = Component.translatable("machine.wildcraft." + help, TOGGLE.getTranslatedKeyMessage());
            if (node >= 0) hint = Component.translatable("machine.wildcraft.node_hint", Component.translatable("machine.wildcraft.node." + node), hint);
            Component part=node<0||body.kind(node)==0?Component.translatable("machine.wildcraft.empty_node"):Component.translatable("item.wildcraft."+switch(body.kind(node)){case 1->"fan";case 2->"battery";case 3->"wing";case 4->body.rocketFuel(node)==0?"spent_rocket":"rocket";case 5->"spring";case 6->"wheel";case 7->"stabilizer";case 8->"buoyancy";default->"machine_body";});
            Component detail=node<0?Component.empty():Component.translatable("machine.wildcraft.node."+node).copy().append(" · ").append(part);
            boolean heldPart=MachineEntity.accepts(p.getMainHandItem());boolean ready=canPreview(body,node);
            if(node>=0&&heldPart){
                String reason=ready?"preview_ready":body.enabled()?"stop_first":body.kind(node)!=0?"node_occupied":p.getMainHandItem().is(MechanicsContent.SPENT_ROCKET)?"spent_part":p.getMainHandItem().is(EnergyContent.BATTERY)&&body.batteryNode()>=0?"battery_exists":"install_failed";
                hint=Component.translatable("machine.wildcraft."+reason);
            }
            int maxWidth=Math.max(40,Math.min(344,g.guiWidth()-24));
            var lines = new java.util.ArrayList<net.minecraft.util.FormattedCharSequence>();
            lines.addAll(c.font.split(label,maxWidth-24));if(node>=0)lines.addAll(c.font.split(detail,maxWidth-24));lines.addAll(c.font.split(hint,maxWidth-24));
            int width=Math.min(maxWidth,24+Math.max(c.font.width(label),Math.max(c.font.width(detail),c.font.width(hint))));
            int x = (g.guiWidth() - width) / 2, y = Math.max(4, g.guiHeight() - 78 - lines.size() * 10);
            g.fill(x - 5, y - 4, x + width + 5, y + lines.size() * 10, 0xB5223030);
            dev.wildcraft.client.art.ArtGui.battery(g,x,y,body.energy(),body.batteryNode()>=0);
            String glyph=!body.enabled()?"off":body.working()?"working":"waiting";
            drawGlyph(g,glyph,x+3,y+19,0xFFE7DEC3);
            if(node>=0&&heldPart)drawGlyph(g,ready?"ready":"blocked",x+3,y+lines.size()*10-10,ready?0xFF93E8C0:0xFFDEAB7A);
            for(var line:lines){g.text(c.font,line,x+24,y,0xFFE7DEC3);y+=10;}
        });
    }
    private static void drawGlyph(net.minecraft.client.gui.GuiGraphicsExtractor g,String kind,int x,int y,int colour){
        String[] rows=switch(kind){
            case "ready"->new String[]{"000000000","000000010","000000110","010001100","011011000","001110000","000100000","000000000","000000000"};
            case "blocked"->new String[]{"000000000","011000110","001101100","000111000","000010000","000111000","001101100","011000110","000000000"};
            case "working"->new String[]{"000000000","001000000","001100000","001110000","001111000","001110000","001100000","001000000","000000000"};
            case "waiting"->new String[]{"000000000","001101100","001101100","001101100","001101100","001101100","001101100","001101100","000000000"};
            default->new String[]{"000000000","011111110","010000010","010000010","010000010","010000010","010000010","011111110","000000000"};
        };
        for(int yy=0;yy<9;yy++)for(int xx=0;xx<9;xx++)if(rows[yy].charAt(xx)=='1')g.fill(x+xx,y+yy,x+xx+1,y+yy+1,colour);
    }
}
