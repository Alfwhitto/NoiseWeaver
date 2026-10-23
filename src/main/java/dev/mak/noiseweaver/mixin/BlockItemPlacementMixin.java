package dev.mak.noiseweaver.mixin;

import dev.mak.noiseweaver.BlockWeights;
import dev.mak.noiseweaver.NoisePainter;
import dev.mak.noiseweaver.NoiseWeaverState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(BlockItem.class)
public abstract class BlockItemPlacementMixin {
    @Inject(method = "place", at = @At("TAIL"))
    private void noiseweaver$chooseBlockForPlacement(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!NoiseWeaverState.placementModeEnabled || cir.getReturnValue() == null || !cir.getReturnValue().consumesAction()) return;
        if (!(context.getLevel() instanceof ServerLevel level)) return;

        Player player = context.getPlayer();
        Set<String> available = BlockWeights.inventoryBlockIds(player);
        for (String id : available) BlockWeights.getOrCreate(id);
        BlockPos position = context.getClickedPos().immutable();
        BlockState chosen = NoisePainter.chooseBlock(position, level.getSeed(), available);
        if (chosen == null) return;
        BlockState placed = level.getBlockState(position);
        if (!placed.isAir() && !placed.equals(chosen)) level.setBlockAndUpdate(position, chosen);
    }
}
