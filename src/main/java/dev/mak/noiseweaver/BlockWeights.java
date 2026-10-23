package dev.mak.noiseweaver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BlockWeights {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Entry>>() {}.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("noiseweaver.json");
    private static final List<Entry> ENTRIES = new ArrayList<>();

    private BlockWeights() {}

    public static void load() {
        ENTRIES.clear();
        try {
            if (Files.exists(FILE)) {
                try (Reader reader = Files.newBufferedReader(FILE)) {
                    List<Entry> loaded = GSON.fromJson(reader, LIST_TYPE);
                    if (loaded != null) ENTRIES.addAll(loaded);
                }
            }
        } catch (Exception ignored) { }
        double max = ENTRIES.stream().mapToDouble(e -> e.weight).filter(Double::isFinite).max().orElse(0);
        for (Entry entry : ENTRIES) {
            if (entry.enabled == null) entry.enabled = true;
            if (entry.weight > 1 && max > 0) entry.weight = entry.weight / max;
            entry.weight = clamp(entry.weight);
        }
        save();
    }

    public static List<Entry> entries() { return ENTRIES; }

    public static Set<String> inventoryBlockIds(Player player) {
        Set<String> ids = new LinkedHashSet<>();
        if (player == null) return ids;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                ids.add(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
            }
        }
        return ids;
    }

    public static Entry getOrCreate(String id) {
        for (Entry entry : ENTRIES) if (entry.block.equals(id)) return entry;
        Entry entry = new Entry(id, 0.5, true);
        ENTRIES.add(entry);
        save();
        return entry;
    }

    public static void update(String id, double weight, boolean enabled) {
        Entry entry = getOrCreate(id);
        entry.weight = clamp(weight);
        entry.enabled = enabled;
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { GSON.toJson(ENTRIES, LIST_TYPE, writer); }
        } catch (Exception ignored) { }
    }

    public static Block block(Entry entry) {
        try { return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(entry.block)).orElse(null); }
        catch (Exception ignored) { return null; }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0;
        return Math.max(0, Math.min(1, value));
    }

    public static final class Entry {
        public String block;
        public double weight;
        public Boolean enabled;
        public Entry(String block, double weight, boolean enabled) {
            this.block = block; this.weight = weight; this.enabled = enabled;
        }
    }
}

