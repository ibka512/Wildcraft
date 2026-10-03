package dev.wildcraft.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.traversal.Gliding;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

/** Three references to owned stacks. Neither the saved data nor the view is an inventory. */
public final class BackEquipment {
    public static final int MELEE = 0, SHIELD = 1, RANGED = 2;
    public record Reference(int slot, ItemStack signature) {
        static final Reference EMPTY = new Reference(-2, ItemStack.EMPTY);
        static final Codec<Reference> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(-2, 40).fieldOf("slot").forGetter(Reference::slot),
                ItemStack.OPTIONAL_CODEC.fieldOf("signature").forGetter(Reference::signature)
        ).apply(i, Reference::new));
    }
    public record Data(int schemaVersion, List<Reference> references) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 1).optionalFieldOf("schemaVersion", 1).forGetter(Data::schemaVersion),
                Reference.CODEC.listOf().validate(list -> list.size() == 3
                        ? com.mojang.serialization.DataResult.success(list)
                        : com.mojang.serialization.DataResult.error(() -> "Expected three equipment references"))
                        .fieldOf("references").forGetter(Data::references)
        ).apply(i, Data::new));
    }
    public record View(ItemStack melee, ItemStack shield, ItemStack ranged) {
        public static final View EMPTY = new View(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        public static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, View::melee, ItemStack.OPTIONAL_STREAM_CODEC, View::shield,
                ItemStack.OPTIONAL_STREAM_CODEC, View::ranged, View::new);
        public ItemStack item(int category) { return category == 0 ? melee : category == 1 ? shield : ranged; }
    }
    private static final class Session {
        final ItemStack[] stacks = new ItemStack[3];
        final ItemStack[] seen = new ItemStack[3];
        final long[] tokens = new long[3];
    }
    public record Transaction(List<ItemStack> signatures, List<Boolean> unique) { }
    public static final AttachmentType<Data> DATA = AttachmentRegistry.create(Wildcraft.id("back_equipment"), b -> b.persistent(Data.CODEC).copyOnDeath());
    public static final AttachmentType<View> VIEW = AttachmentRegistry.create(Wildcraft.id("back_equipment_view"), b -> b.syncWith(View.CODEC, AttachmentSyncPredicate.all()));
    private static final java.util.concurrent.atomic.AtomicLong TOKENS = new java.util.concurrent.atomic.AtomicLong();
    public static final AttachmentType<BackVisual> VISUAL = AttachmentRegistry.create(Wildcraft.id("back_equipment_visual"), b -> b.syncWith(BackVisual.CODEC, AttachmentSyncPredicate.all()));
    private static final AttachmentType<Session> SESSION = AttachmentRegistry.create(Wildcraft.id("back_equipment_session"));
    private BackEquipment() { }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
            if (taken > 0 && source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() instanceof ServerPlayer p && source.getEntity() == p
                    && category(p.getMainHandItem()) == MELEE) register(p, p.getMainHandItem());
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p && !p.level().getGameRules().get(GameRules.KEEP_INVENTORY)) clear(p);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER, (oldP, newP, alive) -> {
            newP.removeAttached(SESSION);
            tick(newP);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> tick(handler.player));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(BackEquipment::tick));
    }
    public static int category(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) return MELEE;
        if (stack.is(Items.SHIELD)) return SHIELD;
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) return RANGED;
        return -1;
    }
    public static Data data(Player p) {
        return p.getAttachedOrCreate(DATA, () -> new Data(1, List.of(Reference.EMPTY, Reference.EMPTY, Reference.EMPTY)));
    }
    public static View view(Player p) {
        View v = p.getAttached(VIEW);
        return v == null ? View.EMPTY : v;
    }
    public static void register(ServerPlayer p, ItemStack stack) {
        int c = category(stack), slot = identitySlot(p, stack);
        if (c < 0 || slot < 0 || !p.isAlive() || p.isSpectator()) return;
        Session s = session(p);
        s.stacks[c] = stack;
        updateData(p, c, slot, stack);
        publish(p, s);
    }
    private static Session session(ServerPlayer p) {
        return p.getAttachedOrCreate(SESSION, () -> {
            Session s = new Session();
            for (int c = 0; c < 3; c++) {
                Reference r = data(p).references().get(c);
                if (r.slot() >= 0 && r.slot() < p.getInventory().getContainerSize()) {
                    ItemStack actual = p.getInventory().getItem(r.slot());
                    if (category(actual) == c && ItemStack.matches(actual, r.signature())) s.stacks[c] = actual;
                }
            }
            return s;
        });
    }
    /** Cheap checks of three known references; search inventory only after a stack actually moves. */
    public static void tick(ServerPlayer p) {
        Session s = session(p);
        for (int c = 0; c < 3; c++) {
            ItemStack stack = s.stacks[c];
            Reference r = data(p).references().get(c);
            int slot = stack == null || stack.isEmpty() ? -2 : r.slot();
            if (slot >= 0 && p.getInventory().getItem(slot) != stack || slot == -1 && p.containerMenu.getCarried() != stack) slot = identitySlot(p, stack);
            if (stack == null || category(stack) != c || slot == -2) {
                s.stacks[c] = null;
                if (!r.signature().isEmpty()) updateData(p, c, -2, ItemStack.EMPTY);
            } else if (slot != r.slot() || !ItemStack.matches(stack, r.signature())) updateData(p, c, slot, stack);
        }
        publish(p, s);
    }
    private static int identitySlot(Player p, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -2;
        for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) if (p.getInventory().getItem(slot) == stack) return slot;
        return p.containerMenu.getCarried() == stack ? -1 : -2;
    }
    private static void updateData(Player p, int c, int slot, ItemStack stack) {
        List<Reference> refs = new ArrayList<>(data(p).references());
        refs.set(c, new Reference(slot, stack.copy()));
        p.setAttached(DATA, new Data(1, List.copyOf(refs)));
    }
    public static void clear(ServerPlayer p) {
        p.removeAttached(SESSION);
        p.setAttached(DATA, new Data(1, List.of(Reference.EMPTY, Reference.EMPTY, Reference.EMPTY)));
        p.setAttached(VIEW, View.EMPTY);
        p.setAttached(VISUAL, BackVisual.EMPTY);
    }
    /** Copying is allowed only inside a verified native inventory transaction with one unambiguous match. */
    public static Transaction beforeTransaction(ServerPlayer p) {
        tick(p);
        List<ItemStack> signatures = new ArrayList<>();
        List<Boolean> unique = new ArrayList<>();
        for (Reference r : data(p).references()) {
            signatures.add(r.signature().copy());
            unique.add(!r.signature().isEmpty() && matchingOwned(p, r.signature()).size() == 1);
        }
        return new Transaction(signatures, unique);
    }
    public static void afterTransaction(ServerPlayer p, Transaction t) {
        Session s = session(p);
        for (int c = 0; c < 3; c++) {
            if (identitySlot(p, s.stacks[c]) == -2 && t.unique().get(c)) {
                List<ItemStack> matches = matchingOwned(p, t.signatures().get(c));
                if (matches.size() == 1) {
                    s.stacks[c] = matches.getFirst();
                    updateData(p, c, identitySlot(p, s.stacks[c]), s.stacks[c]);
                }
            }
        }
        tick(p);
    }
    private static List<ItemStack> matchingOwned(Player p, ItemStack signature) {
        List<ItemStack> matches = new ArrayList<>();
        for (int slot = 0; slot < p.getInventory().getContainerSize(); slot++) {
            ItemStack stack = p.getInventory().getItem(slot);
            if (ItemStack.matches(stack, signature)) matches.add(stack);
        }
        if (ItemStack.matches(p.containerMenu.getCarried(), signature)) matches.add(p.containerMenu.getCarried());
        return matches;
    }
    public static boolean holstersShield(Player p) {
        return p.isUsingItem() && category(p.getUseItem()) == RANGED;
    }
    private static void publish(ServerPlayer p, Session s) {
        ItemStack[] visible = new ItemStack[3];
        BackVisual.Entry[] owners = new BackVisual.Entry[3];
        boolean glide = Gliding.active(p);
        boolean wings = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        boolean bowUse = holstersShield(p);
        for (int c = 0; c < 3; c++) {
            ItemStack stack = s.stacks[c];
            if (stack != s.seen[c]) { s.seen[c] = stack; s.tokens[c] = stack == null ? 0 : TOKENS.incrementAndGet(); }
            boolean held = (stack == p.getMainHandItem() || stack == p.getOffhandItem()) && !(bowUse && c == SHIELD);
            visible[c] = stack == null || !p.isAlive() || p.isSpectator() || held && !glide || wings && c != MELEE
                    ? ItemStack.EMPTY : displayStack(stack);
            int owner = stack == null || !p.isAlive() || p.isSpectator() ? 0
                    : !visible[c].isEmpty() ? 3
                    : held && !glide ? (stack == p.getMainHandItem() ? 1 : 2) : 0;
            owners[c] = new BackVisual.Entry(s.tokens[c], owner);
        }
        BackVisual visual = new BackVisual(owners[0], owners[1], owners[2]);
        if (!visual.equals(p.getAttached(VISUAL))) p.setAttached(VISUAL, visual);
        View next = new View(visible[0], visible[1], visible[2]), old = view(p);
        if (!ItemStack.matches(next.melee(), old.melee()) || !ItemStack.matches(next.shield(), old.shield()) || !ItemStack.matches(next.ranged(), old.ranged())) p.setAttached(VIEW, next);
    }
    /** Explicit allowlist: never expose private containers, arbitrary custom data, lore or nested projectiles. */
    public static ItemStack displayStack(ItemStack original) {
        if (original.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(original.getItem());
        for (DataComponentType<?> type : new ArrayList<>(result.getComponents().keySet())) result.remove(type);
        copy(original, result, DataComponents.ITEM_MODEL);
        copy(original, result, DataComponents.CUSTOM_MODEL_DATA);
        copy(original, result, DataComponents.DAMAGE);
        copy(original, result, DataComponents.MAX_DAMAGE);
        copy(original, result, DataComponents.DYED_COLOR);
        copy(original, result, DataComponents.BANNER_PATTERNS);
        copy(original, result, DataComponents.BASE_COLOR);
        copy(original, result, DataComponents.UNBREAKABLE);

        result.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, original.hasFoil());
        return result;
    }
    private static <T> void copy(ItemStack from, ItemStack to, DataComponentType<T> type) {
        T value = from.get(type);
        if (value != null) to.set(type, value);
    }
}
