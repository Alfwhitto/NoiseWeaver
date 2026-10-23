package dev.mak.noiseweaver.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.mak.noiseweaver.BlockWeights;
import dev.mak.noiseweaver.NoisePainter;
import dev.mak.noiseweaver.NoiseWeaverState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Effortless Building's bulk builder writes blocks directly with Level#setBlock,
 * bypassing BlockItem#place. Redirect those writes so the noise palette is applied
 * to each actual target position.
 */
@Pseudo
@Mixin(targets = "nl.requios.effortlessbuilding.network.PacketHandler", remap = false)
public abstract class EffortlessBuildingPlacementMixin {
    @Redirect(
            method = "handlePlaceBuildMode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 0
    )
    private static boolean noiseweaver$replaceEffortlessServerLevelSetBlock(
            ServerLevel level, BlockPos pos, BlockState original, int flags,
            @Local(argsOnly = true) ServerPlayer player) {
        return noiseweaver$setWeightedBlock(level, pos, original, flags, player);
    }

    @Redirect(
            method = "handlePlaceBuildMode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 0
    )
    private static boolean noiseweaver$replaceEffortlessLevelSetBlock(
            Level level, BlockPos pos, BlockState original, int flags,
            @Local(argsOnly = true) ServerPlayer player) {
        return noiseweaver$setWeightedBlock(level, pos, original, flags, player);
    }

    private static boolean noiseweaver$setWeightedBlock(
            Level level, BlockPos pos, BlockState original, int flags, ServerPlayer player) {
        if (!NoiseWeaverState.placementModeEnabled || !(level instanceof ServerLevel serverLevel)) {
            return level.setBlock(pos, original, flags);
        }

        var available = BlockWeights.inventoryBlockIds(player);
        // Effortless Building also calls setBlock for fluids and utility actions.
        // Only substitute calls whose original block is represented by an item in
        // the player's inventory (the main-hand block or a Randomizer Bag entry).
        String originalId = BuiltInRegistries.BLOCK.getKey(original.getBlock()).toString();
        if (!available.contains(originalId)) return level.setBlock(pos, original, flags);

        BlockState chosen = NoisePainter.chooseBlock(pos, serverLevel.getSeed(), available);
        if (chosen == null) return level.setBlock(pos, original, flags);

        // Keep shared placement properties such as facing, axis and half from the
        // state Effortless Building calculated for the original held block.
        for (Property<?> property : original.getProperties()) {
            if (chosen.hasProperty(property)) chosen = noiseweaver$copyProperty(original, chosen, property);
        }
        return level.setBlock(pos, chosen, flags);
    }

    private static <T extends Comparable<T>> BlockState noiseweaver$copyProperty(
            BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }
}
