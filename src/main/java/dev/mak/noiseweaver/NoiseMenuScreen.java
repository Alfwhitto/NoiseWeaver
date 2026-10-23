package dev.mak.noiseweaver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NoiseMenuScreen extends Screen {
    private final int page;
    private List<BlockRow> blocks = List.of();
    private int rowsPerPage;
    private int listTop;
    private int left;

    public NoiseMenuScreen() { this(0); }
    private NoiseMenuScreen(int page) {
        super(Component.translatable("screen.noiseweaver.title"));
        this.page = Math.max(0, page);
    }

    @Override protected void init() {
        blocks = getInventoryBlocks(minecraft.player);
        rowsPerPage = Math.max(1, Math.min(8, (height - 160) / 32));
        int pageCount = Math.max(1, (blocks.size() + rowsPerPage - 1) / rowsPerPage);
        int validPage = Math.min(page, pageCount - 1);
        left = (width - 380) / 2;
        listTop = 76;

        for (int row = 0; row < rowsPerPage; row++) {
            int index = validPage * rowsPerPage + row;
            if (index >= blocks.size()) break;
            BlockRow block = blocks.get(index);
            BlockWeights.Entry entry = BlockWeights.getOrCreate(block.id);
            int y = listTop + row * 32;
            addRenderableWidget(new WeightSlider(left + 112, y + 3, 116, 20, block.id, entry.weight));
            Button toggle = Button.builder(toggleText(entry.enabled), button -> {
                entry.enabled = !entry.enabled;
                BlockWeights.update(block.id, entry.weight, entry.enabled);
                button.setMessage(toggleText(entry.enabled));
            }).bounds(left + 235, y + 3, 66, 20).build();
            addRenderableWidget(toggle);
        }

        addRenderableWidget(Button.builder(Component.literal("‹"), button -> minecraft.setScreenAndShow(new NoiseMenuScreen(Math.max(0, validPage - 1))))
                .bounds(width / 2 - 78, height - 57, 32, 20).build());
        addRenderableWidget(Button.builder(Component.literal("›"), button -> minecraft.setScreenAndShow(new NoiseMenuScreen(Math.min(pageCount - 1, validPage + 1))))
                .bounds(width / 2 + 46, height - 57, 32, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.noiseweaver.close"), button -> onClose())
                .bounds(width / 2 - 48, height - 29, 96, 20).build());
    }

    private static Component toggleText(boolean enabled) {
        return Component.literal(enabled ? "ON" : "OFF");
    }

    private static List<BlockRow> getInventoryBlocks(Player player) {
        if (player == null) return List.of();
        Map<String, Block> unique = new LinkedHashMap<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString();
                unique.putIfAbsent(id, block);
            }
        }
        List<BlockRow> result = new ArrayList<>();
        unique.forEach((id, block) -> result.add(new BlockRow(id, block)));
        return result;
    }

    public static java.util.Set<String> inventoryBlockIds(Player player) {
        java.util.Set<String> result = new java.util.LinkedHashSet<>();
        for (BlockRow row : getInventoryBlocks(player)) result.add(row.id);
        return result;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.text(font, title, (width - font.width(title)) / 2, 20, 0xFFFFFF);
        Component help = Component.translatable("screen.noiseweaver.help");
        graphics.text(font, help, (width - font.width(help)) / 2, 42, 0xC8C8C8);

        double totalWeight = 0;
        for (BlockRow row : blocks) {
            BlockWeights.Entry entry = BlockWeights.getOrCreate(row.id);
            if (entry.enabled) totalWeight += entry.weight;
        }

        int pageCount = Math.max(1, (blocks.size() + rowsPerPage - 1) / rowsPerPage);
        int currentPage = Math.min(page, pageCount - 1);
        for (int row = 0; row < rowsPerPage; row++) {
            int index = currentPage * rowsPerPage + row;
            if (index >= blocks.size()) break;
            BlockRow block = blocks.get(index);
            BlockWeights.Entry entry = BlockWeights.getOrCreate(block.id);
            int y = listTop + row * 32;
            Component name = block.block.getName();
            graphics.item(new ItemStack(block.block.asItem()), left + 2, y + 4);
            graphics.text(font, font.plainSubstrByWidth(name.getString(), 88), left + 22, y + 8, entry.enabled ? 0xFFFFFF : 0x777777);
            double percent = entry.enabled && totalWeight > 0 ? entry.weight / totalWeight * 100 : 0;
            graphics.text(font, String.format(java.util.Locale.ROOT, "%.0f%%", percent), left + 307, y + 8, 0xD8D8D8);
        }

        int first = blocks.isEmpty() ? 0 : currentPage * rowsPerPage + 1;
        int last = Math.min(blocks.size(), currentPage * rowsPerPage + rowsPerPage);
        Component pageLabel = blocks.isEmpty()
                ? Component.translatable("screen.noiseweaver.empty_inventory")
                : Component.literal(first + "–" + last + " / " + blocks.size());
        graphics.text(font, pageLabel, (width - font.width(pageLabel)) / 2, height - 51, 0xB0B0B0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { minecraft.setScreenAndShow(null); }

    private record BlockRow(String id, Block block) {}

    private static final class WeightSlider extends AbstractSliderButton {
        private final String id;
        private boolean enabled;

        private WeightSlider(int x, int y, int width, int height, String id, double initial) {
            super(x, y, width, height, Component.empty(), Math.max(0, Math.min(1, initial)));
            this.id = id;
            this.enabled = enabled;
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Component.literal(String.format(java.util.Locale.ROOT, "%.2f", value)));
        }

        @Override protected void applyValue() {
            BlockWeights.Entry entry = BlockWeights.getOrCreate(id);
            BlockWeights.update(id, value, entry.enabled);
        }
    }
}





