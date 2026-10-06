package dev.wildcraft.client.datagen;

import java.util.concurrent.CompletableFuture;

import dev.wildcraft.registry.WildcraftItems;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

public final class WildcraftLanguageProvider extends FabricLanguageProvider {
    private final String language;

    public WildcraftLanguageProvider(FabricPackOutput output, String language,
                                    CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, language, lookup);
        this.language = language;
    }

    @Override
    public void generateTranslations(HolderLookup.Provider lookup, TranslationBuilder translations) {
        translations.add(WildcraftItems.TEST_CORE,
                language.equals("zh_cn") ? "Wildcraft 测试核心" : "Wildcraft Test Core");
        boolean chinese = language.equals("zh_cn");
        try (var input=getClass().getResourceAsStream("/wildcraft-datagen-labels.json")) {
            if(input==null)throw new IllegalStateException("Missing adopted translations");
            var source=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject(language);
            source.entrySet().forEach(entry->translations.add(entry.getKey(),entry.getValue().getAsString()));
        } catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}

        translations.add("key.wildcraft.fuse", chinese ? "融合 / 按住蹲下拆分" : "Fuse / sneak to split");
        translations.add("key.categories.wildcraft.fusion", chinese ? "Wildcraft 融合" : "Wildcraft Fusion");
        translations.add("fuse.wildcraft.kind.6", chinese ? "材料不可用：效果和拆卸已停用，原始数据保留" : "Material unavailable: effects and splitting disabled; data retained");
        translations.add("machine.wildcraft.empty_node",chinese?"空节点":"Empty node");
        translations.add("machine.wildcraft.preview_ready",chinese?"位置可用，右键尝试安装":"Position available · right-click to install");
        translations.add("machine.wildcraft.node_occupied",chinese?"此接口已有部件":"This node is occupied");
        translations.add("machine.wildcraft.battery_exists",chinese?"主体已有电池":"A battery is already installed");
        translations.add("machine.wildcraft.spent_part",chinese?"空火箭壳不能安装":"A spent rocket cannot be installed");
        translations.add("fuse.wildcraft.material_uses",chinese?"融合材料剩余 %s 次":"Fusion material: %s uses left");
        translations.add("fuse.wildcraft.done", chinese ? "融合完成" : "Fusion attached");
        translations.add("fuse.wildcraft.split_done", chinese ? "已拆分，材料保留剩余次数" : "Split; material keeps its remaining uses");
        translations.add("fuse.wildcraft.refused", chinese ? "请检查两手、材料限制和库存空位" : "Check hands, material limits and free inventory space");
        translations.add("fuse.wildcraft.tooltip", chinese ? "融合材料：%s · 剩余 %s 次" : "Fused: %s \u00b7 %s uses left");
        translations.add("fuse.wildcraft.controls", chinese ? "主手宿主 / 副手材料 · %s融合 · 蹲下＋该键拆分" : "Host in main hand / material offhand \u00b7 %s to fuse \u00b7 sneak + key to split");
        translations.add("fuse.wildcraft.kind.0", chinese ? "通用：近战 / 箭 +1；盾牌近身反推" : "Generic: melee / arrow +1; shield pushes nearby attacker");
        translations.add("fuse.wildcraft.kind.1", chinese ? "石类：近战 / 箭 +2；盾牌近身反推" : "Rock: melee / arrow +2; shield pushes nearby attacker");
        translations.add("fuse.wildcraft.kind.2", chinese ? "金属类：近战 / 箭 +3；盾牌近身反推" : "Metal: melee / arrow +3; shield pushes nearby attacker");
        translations.add("fuse.wildcraft.kind.3", chinese ? "烈焰：+1；命中 / 完整格挡点燃3秒" : "Flame: +1; hit / full block ignites for 3 seconds");
        translations.add("fuse.wildcraft.kind.4", chinese ? "寒冰：+1；命中 / 完整格挡减速3秒" : "Ice: +1; hit / full block slows for 3 seconds");
        translations.add("fuse.wildcraft.kind.5", chinese ? "弹性：+1；命中 / 完整格挡增加击退" : "Elastic: +1; hit / full block adds knockback");
        translations.add("block.wildcraft.fabricator",chinese?"古代装置制造机":"Ancient Device Fabricator");
        translations.add("fabrication.wildcraft.start",chinese?"制造":"Make");
        translations.add("fabrication.wildcraft.cost",chinese?"铜锭×4 + 红石×2":"4 copper + 2 redstone");
        translations.add("fabrication.wildcraft.pool",chinese?"八类部件 · 各%s%%":"8 part types · %s%% each");
        String[] factoryZh={"材料就绪","放入铜锭和红石","制造中 · 可关闭界面","产出格占用 · 等待取走","制造完成 · 取走后可再制造"};
        String[] factoryEn={"Ready","Add copper and redstone","Working · safe to close","Output blocked","Ready to collect"};
        for(int n=0;n<5;n++)translations.add("fabrication.wildcraft.status."+n,chinese?factoryZh[n]:factoryEn[n]);
        translations.add("item.wildcraft.wing", chinese ? "机械翼" : "Machine wing");
        translations.add("item.wildcraft.wheel", chinese ? "机械轮" : "Machine wheel");
        translations.add("item.wildcraft.rocket",chinese?"机械火箭":"Machine rocket");
        translations.add("item.wildcraft.spent_rocket",chinese?"火箭空壳":"Spent rocket casing");
        translations.add("item.wildcraft.spring",chinese?"机械弹簧":"Machine spring");
        translations.add("item.wildcraft.stabilizer",chinese?"稳定器":"Stabilizer");
        translations.add("item.wildcraft.buoyancy",chinese?"浮力装置":"Buoyancy device");
        translations.add("machine.wildcraft.fuel",chinese?"燃料 %s / %s 世界刻":"Fuel %s / %s world ticks");
        translations.add("machine.wildcraft.cooldown",chinese?"复位冷却 %s 世界刻":"Rearm cooldown %s world ticks");
        translations.add("machine.wildcraft.help.wing",chinese?"被动滑翔，需要水平速度；载重越大下降越快":"Passive glide requires speed; greater load descends faster");
        translations.add("machine.wildcraft.help.wheel",chinese?"接地供电驱动；乘坐时W/S前后、A/D转向":"Powered ground drive; ride with W/S and steer with A/D");
        translations.add("machine.wildcraft.help.rocket",chinese?"一次性燃料；点燃后关机不能熄火，燃烧中不能拆下":"One-shot fuel; keeps burning after power off and cannot be removed while lit");
        translations.add("machine.wildcraft.help.spent_rocket",chinese?"空壳无推力、不能安装；合成回收1铜锭":"No thrust; cannot install; craft to recover one copper ingot");
        translations.add("machine.wildcraft.help.spring",chinese?"接地启用弹射；每个消耗20电量，关机落地冷却后复位":"Ground launch costs 20 per spring; power off, land and cool down to rearm");
        translations.add("machine.wildcraft.help.stabilizer",chinese?"每刻消耗2电量，有限抑制侧漂并柔化转向，不抗重力":"Costs 2 per tick; limits side drift and smooths steering, without cancelling gravity");
        translations.add("machine.wildcraft.help.buoyancy",chinese?"仅在水中提供有限被动浮力；单件载重上限1.75":"Passive water buoyancy; supports up to 1.75 load per device");
        translations.add("entity.wildcraft.machine", chinese ? "机械主体" : "Machine");
        translations.add("item.wildcraft.machine_body", chinese ? "机械主体" : "Machine Body");
        translations.add("item.wildcraft.fan", chinese ? "风扇" : "Fan");
        translations.add("machine.wildcraft.owner", chinese ? "仅创建者可以操作这台机械" : "Only its creator can modify this machine");
        translations.add("machine.wildcraft.aim", chinese ? "请靠近并指向机械的可见安装点" : "Move closer and aim at a visible machine node");
        translations.add("machine.wildcraft.installed", chinese ? "部件已安装" : "Part installed");
        translations.add("machine.wildcraft.install_failed", chinese ? "指向空节点；每台最多一个电池" : "Aim at an empty node; one battery per machine");
        translations.add("machine.wildcraft.empty_hand", chinese ? "装拆或乘坐请按提示切换手持物品" : "Use a part or empty hand as shown");
        translations.add("machine.wildcraft.stop_first", chinese ? "请先停机，再装拆部件" : "Switch off before installing or removing parts");
        translations.add("machine.wildcraft.recovered", chinese ? "部件已回收，电量保持" : "Part recovered with remaining charge");
        translations.add("machine.wildcraft.recover_failed", chinese ? "指向已安装节点，并留出库存空间" : "Aim at an installed node and make inventory room");
        translations.add("machine.wildcraft.on", chinese ? "已开启机械" : "Machine switched on");
        translations.add("machine.wildcraft.off", chinese ? "已关闭机械" : "Machine switched off");
        translations.add("machine.wildcraft.riding", chinese ? "已乘坐：A/D转向，机械启停键控制，蹲下离开" : "Riding: A/D steer, machine key toggles power, sneak to dismount");
        translations.add("machine.wildcraft.occupied", chinese ? "乘坐位置已占用" : "Seat occupied");
        translations.add("machine.wildcraft.body_recovered", chinese ? "空机械主体已回收" : "Empty body recovered");
        translations.add("machine.wildcraft.body_failed", chinese ? "停机、拆空、离开座位，并留出库存空间" : "Switch off, remove parts, leave the seat and make inventory room");
        translations.add("machine.wildcraft.status", chinese ? "机械 · 电量 %s / 1000 · %s" : "Machine \u00b7 charge %s / 1000 \u00b7 %s");
        translations.add("machine.wildcraft.running", chinese ? "运行中" : "Running");
        translations.add("machine.wildcraft.waiting", chinese ? "开启 · 无动力" : "On \u00b7 no power");
        translations.add("machine.wildcraft.drive_hint", chinese ? "W/S前后 · A/D转向 · %s启停 · 蹲下离开" : "W/S drive · A/D steer · %s toggle · sneak to dismount");
        translations.add("machine.wildcraft.install_hint", chinese ? "右键安装 · 仅创建者可操作" : "Right-click to install \u00b7 creator only");
        translations.add("machine.wildcraft.ground_hint", chinese ? "空手右键乘坐 · 蹲下右键装拆/控制板启停 · 拆空后蹲下攻击回收" : "Empty hand: right-click to ride \u00b7 sneak-click node/panel to remove/toggle \u00b7 sneak-attack empty body to recover");
        translations.add("machine.wildcraft.node_hint", chinese ? "%s · %s" : "%s \u00b7 %s");
        translations.add("machine.wildcraft.node.0", chinese ? "上方" : "Top");
        translations.add("machine.wildcraft.node.1", chinese ? "下方" : "Bottom");
        translations.add("machine.wildcraft.node.2", chinese ? "后方" : "Rear");
        translations.add("machine.wildcraft.node.3", chinese ? "前方" : "Front");
        translations.add("machine.wildcraft.node.4", chinese ? "左侧" : "Left");
        translations.add("machine.wildcraft.node.5", chinese ? "右侧" : "Right");
        translations.add("key.wildcraft.machine_toggle", chinese ? "机械启停（乘坐时）" : "Toggle machine while riding");
        translations.add("key.category.wildcraft.mechanics", chinese ? "Wildcraft · 机械" : "Wildcraft · Mechanics");
        translations.add("block.wildcraft.charger", chinese ? "充电器" : "Charger");
        translations.add("item.wildcraft.battery", chinese ? "有限电池" : "Battery");
        translations.add("energy.wildcraft.charge", chinese ? "电量 %s / %s" : "Charge %s / %s");
        translations.add("energy.wildcraft.battery_help", chinese ? "放入紧邻固定红石块的充电器" : "Charge beside a fixed redstone block");
        translations.add("energy.wildcraft.status.0", chinese ? "放入有限电池" : "Insert a battery");
        translations.add("energy.wildcraft.status.1", chinese ? "需要紧邻固定红石块" : "Place beside a redstone block");
        translations.add("energy.wildcraft.status.2", chinese ? "充电中" : "Charging");
        translations.add("energy.wildcraft.status.3", chinese ? "电量已满" : "Fully charged");
        translations.add("energy.wildcraft.status.4", chinese ? "等候共享电源" : "Waiting for shared supply");
        translations.add("block.wildcraft.cooking_pot", chinese ? "料理锅" : "Cooking Pot");
        translations.add("item.wildcraft.meal", chinese ? "料理" : "Cooked Meal");
        translations.add("item.wildcraft.meal.vegetable", chinese ? "蔬菜炖煮" : "Vegetable Stew");
        translations.add("item.wildcraft.meal.warming", chinese ? "保暖料理" : "Warming Stew");
        translations.add("item.wildcraft.meal.cooling", chinese ? "耐热料理" : "Cooling Fruit Bowl");
        translations.add("item.wildcraft.meal.recovery", chinese ? "精力料理" : "Restorative Mushroom Stew");
        translations.add("meal.wildcraft.details", chinese ? "效果 %s 级 · %s 秒" : "Level %s \u00b7 %s seconds");
        translations.add("meal.wildcraft.help.1", chinese ? "减缓细雪冻结；不会阻止完全冻结后的伤害" : "Slows powder-snow freezing; does not prevent freezing damage");
        translations.add("meal.wildcraft.help.2", chinese ? "减弱热色调；不提供抗火或岩浆免疫" : "Softens heat tint; no fire or lava protection");
        translations.add("meal.wildcraft.help.3", chinese ? "在原有条件下加快精力恢复；不提高上限" : "Faster eligible stamina recovery; capacity unchanged");
        translations.add("cooking.wildcraft.ingredients", chinese ? "食材" : "Ingredients");
        translations.add("cooking.wildcraft.bowl", chinese ? "空碗" : "Bowl");
        translations.add("cooking.wildcraft.status.0", chinese ? "放入匹配食材" : "Add matching ingredients");
        translations.add("cooking.wildcraft.status.1", chinese ? "锅下需要点燃的营火" : "Light a campfire below");
        translations.add("cooking.wildcraft.status.2", chinese ? "烹饪中" : "Cooking");
        translations.add("cooking.wildcraft.status.3", chinese ? "放入空碗" : "Add an empty bowl");
        translations.add("cooking.wildcraft.status.4", chinese ? "请先取走成品" : "Take the finished meal");
        translations.add("hud.wildcraft.meal.1", chinese ? "保暖 %s · %s秒" : "Warmth %s \u00b7 %ss");
        translations.add("hud.wildcraft.meal.2", chinese ? "耐热 %s · %s秒" : "Cooling %s \u00b7 %ss");
        translations.add("hud.wildcraft.meal.3", chinese ? "恢复 %s · %s秒" : "Recovery %s \u00b7 %ss");
        translations.add("subtitles.wildcraft.focus.enter", chinese ? "进入专注" : "Enter focus");
        translations.add("subtitles.wildcraft.focus.exit", chinese ? "退出专注" : "Leave focus");
        translations.add(WildcraftItems.PARAGLIDER, chinese ? "滑翔伞" : "Paraglider");
        translations.add("key.wildcraft.focus_settings", chinese ? "Wildcraft 画面设置" : "Wildcraft presentation settings");
        translations.add("key.category.wildcraft.focus_settings", chinese ? "Wildcraft · 画面" : "Wildcraft \u00b7 Presentation");
        translations.add("focus.wildcraft.settings", chinese ? "Wildcraft · 画面设置" : "Wildcraft \u00b7 Presentation");
        translations.add("focus.wildcraft.settings.hint", chinese ? "仅调整表现，技能、精力与环境读数保持生效" : "Presentation only; skill, stamina and environment readings stay active");
        translations.add("focus.wildcraft.strength", chinese ? "效果强度：%s" : "Effect strength: %s");
        translations.add("focus.wildcraft.strength.0", chinese ? "关闭" : "Off");
        translations.add("focus.wildcraft.strength.1", chinese ? "弱" : "Weak");
        translations.add("focus.wildcraft.strength.2", chinese ? "标准" : "Standard");
        translations.add("focus.wildcraft.strength.3", chinese ? "强" : "Strong");
        translations.add("focus.wildcraft.vignette", chinese ? "暗角：%s" : "Vignette: %s");
        translations.add("focus.wildcraft.reticle", chinese ? "准星增强：%s" : "Reticle: %s");
        translations.add("focus.wildcraft.pattern", chinese ? "边缘纹理：%s" : "Edge pattern: %s");
        translations.add("focus.wildcraft.pulse", chinese ? "低精力轻闪：%s" : "Low stamina pulse: %s");
        translations.add("focus.wildcraft.fov", chinese ? "轻微视野收窄：%s" : "Slight FOV: %s");
        translations.add("focus.wildcraft.help.strength", chinese ? "关闭/弱/标准/强；各开关偏好独立保留。" : "Off/weak/standard/strong; toggle preferences are retained.");
        translations.add("focus.wildcraft.help.vignette", chinese ? "轻微压暗画面边缘，保持瞄准中心清晰。" : "Gently shades the edges, keeping the aiming centre clear.");
        translations.add("focus.wildcraft.help.reticle", chinese ? "专注时增强准星，低精力用颜色与刻度提示。" : "Focus reticle; low stamina changes both colour and ticks.");
        translations.add("focus.wildcraft.help.pattern", chinese ? "仅在画面外围显示静态细纹。" : "Static contour lines at the periphery only.");
        translations.add("focus.wildcraft.help.pulse", chinese ? "可选低精力轻闪；默认关闭。" : "Optional gentle low-stamina pulse; off by default.");
        translations.add("focus.wildcraft.help.fov", chinese ? "可选轻微视野收窄；默认关闭。" : "Optional slight FOV reduction; off by default.");
        String[] thermalNames = chinese ? new String[]{"极冷", "寒冷", "偏冷", "舒适", "偏暖", "炎热", "极热"}
                : new String[]{"Very cold", "Cold", "Cool", "Comfortable", "Warm", "Hot", "Very hot"};
        for (int i = 0; i < thermalNames.length; i++) translations.add("hud.wildcraft.temperature." + dev.wildcraft.temperature.TemperatureRules.STATES[i], thermalNames[i]);
        translations.add("command.wildcraft.temperature.status", chinese ? "环境 %s；指数 %s（非摄氏度）；群系 %s / 天气 %s / 水 %s / 热源 %s；读取 %s / 射线 %s" : "Environment %s; index %s (not Celsius); biome %s / weather %s / water %s / heat %s; reads %s / rays %s");
        translations.add("temperature.wildcraft.tint", chinese ? "温度边缘色调：%s" : "Temperature edge tint: %s");
        translations.add("temperature.wildcraft.help.tint", chinese ? "轻微冷热边缘提示；专注时暂停，关闭后温度读数仍保留。" : "Subtle warm/cold edges; suppressed during focus. Readout remains when disabled.");
        translations.add("slot.wildcraft.paraglider", chinese ? "滑翔伞装备位 · 仅放滑翔伞" : "Paraglider slot · paraglider only");
        translations.add("hud.wildcraft.gliding", chinese ? "滑翔中 · 再按%s收伞" : "Gliding · press %s to close");
        translations.add("key.wildcraft.climb", chinese ? "按住攀爬 / 松开放手" : "Hold to climb / release to let go");
        translations.add("key.category.wildcraft.controls", "Wildcraft");
        translations.add("hud.wildcraft.climbing", chinese ? "攀爬中 · 松开%s放手" : "Climbing · release %s");
        translations.add("hud.wildcraft.stamina", chinese ? "精力 %s / %s" : "Stamina %s / %s");
        translations.add("hud.wildcraft.peak_level", chinese ? "历史最高等级 %s" : "Peak level %s");
        translations.add("command.wildcraft.stamina.status", chinese ? "精力 %s / %s；历史最高等级 %s" : "Stamina %s / %s; peak level %s");
        translations.add("command.wildcraft.stamina.consumed", chinese ? "已消耗 %s 精力" : "Consumed %s stamina");
        translations.add("command.wildcraft.stamina.filled", chinese ? "精力已补满" : "Stamina refilled");
    }
}
