package com.timemachine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * What the save file remembers: which version the dial is set to, and everything the trip took
 * off you.
 *
 * <p>Confiscated items are not destroyed. Travelling to Beta 1.7.3 with a netherite pickaxe puts
 * the pickaxe in the vault; travelling back to anywhere from 1.16.5 onwards hands it back,
 * enchantments and all, because an {@link ItemStack} round-trips through its codec intact. Anything
 * else would make the dial a one-way trip and the mod a way to lose your gear.
 *
 * <p>Lives on the overworld's state manager, since the era is a property of the save and not of a
 * dimension — see {@link #get(ServerWorld)}.
 */
public class TimeState extends PersistentState {

    /** One stack, and who it will go back to. */
    public record Held(UUID owner, ItemStack stack) {
        public static final Codec<Held> CODEC = RecordCodecBuilder.create(i -> i.group(
                Uuids.CODEC.fieldOf("owner").forGetter(Held::owner),
                ItemStack.CODEC.fieldOf("stack").forGetter(Held::stack)
        ).apply(i, Held::new));
    }

    public static final Codec<TimeState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("version", "1.21.11").forGetter(s -> s.versionId),
            Held.CODEC.listOf().optionalFieldOf("vault", List.of()).forGetter(s -> s.vault)
    ).apply(i, TimeState::new));

    public static final PersistentStateType<TimeState> TYPE = new PersistentStateType<>(
            "timemachine", TimeState::new, CODEC, null);

    private int index = Timeline.PRESENT;
    private String versionId = Timeline.get(Timeline.PRESENT).id();
    private final List<Held> vault = new ArrayList<>();

    public TimeState() {
    }

    private TimeState(String versionId, List<Held> vault) {
        // The id, not the index, is what gets written down: inserting a version into the timeline
        // must not silently move every existing save to a different era.
        int found = Timeline.indexOf(versionId);
        this.index = found < 0 ? Timeline.PRESENT : found;
        this.versionId = Timeline.get(this.index).id();
        this.vault.addAll(vault);
    }

    public static TimeState get(ServerWorld world) {
        return world.getServer().getOverworld().getPersistentStateManager().getOrCreate(TYPE);
    }

    // ---------------------------------------------------------------- the dial

    public int index() {
        return index;
    }

    public void setIndex(int newIndex) {
        int clamped = Timeline.clamp(newIndex);
        if (clamped != index) {
            index = clamped;
            versionId = Timeline.get(clamped).id();
            markDirty();
        }
    }

    // ---------------------------------------------------------------- the vault

    /** Takes {@code stack} out of history until its own version comes round again. */
    public void store(UUID owner, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        vault.add(new Held(owner, stack.copy()));
        markDirty();
    }

    /**
     * Removes and returns everything of {@code owner}'s that exists again now that the dial reads
     * {@link Era#index()}.
     */
    public List<ItemStack> reclaim(UUID owner) {
        List<ItemStack> out = new ArrayList<>();
        Iterator<Held> it = vault.iterator();
        while (it.hasNext()) {
            Held held = it.next();
            if (!held.owner().equals(owner) || ContentCalendar.isAnachronistic(held.stack())) {
                continue;
            }
            out.add(held.stack());
            it.remove();
        }
        if (!out.isEmpty()) {
            markDirty();
        }
        return out;
    }

    /** How many stacks are being held back from {@code owner} right now. */
    public int heldCount(UUID owner) {
        return (int) vault.stream().filter(h -> h.owner().equals(owner)).count();
    }
}
