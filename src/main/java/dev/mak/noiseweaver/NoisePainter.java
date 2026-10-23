package dev.mak.noiseweaver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.Set;

public final class NoisePainter {
    private static final double FREQUENCY = 0.22;
    private static final int CDF_SAMPLES = 16_384;
    private static final double[] NOISE_CDF = buildNoiseCdf();
    private NoisePainter() {}

    public static BlockState chooseBlock(BlockPos pos, long seed, Set<String> availableBlocks) {
        List<Weighted> palette = new ArrayList<>();
        double total = 0;
        for (BlockWeights.Entry entry : BlockWeights.entries()) {
            if (!availableBlocks.contains(entry.block) || !Boolean.TRUE.equals(entry.enabled) || entry.weight <= 0 || !Double.isFinite(entry.weight)) continue;
            Block block = BlockWeights.block(entry);
            if (block == null) continue;
            total += entry.weight;
            palette.add(new Weighted(block.defaultBlockState(), total));
        }
        if (palette.isEmpty() || total <= 0) return null;
        double noise = fractalNoise(pos.getX() * FREQUENCY, pos.getY() * FREQUENCY, pos.getZ() * FREQUENCY, seed);
        double sample = uniformize(noise) * total;
        for (Weighted weighted : palette) if (sample < weighted.cumulative) return weighted.state;
        return palette.get(palette.size() - 1).state;
    }

    private static double[] buildNoiseCdf() {
        double[] samples = new double[CDF_SAMPLES];
        Random random = new Random(0x4E6F697365576561L);
        for (int i = 0; i < samples.length; i++) {
            double x = random.nextDouble() * 4096 - 2048;
            double y = random.nextDouble() * 4096 - 2048;
            double z = random.nextDouble() * 4096 - 2048;
            samples[i] = fractalNoise(x, y, z, 0x574569676874734CL);
        }
        Arrays.sort(samples);
        return samples;
    }

    private static double uniformize(double value) {
        int low = 0, high = NOISE_CDF.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (NOISE_CDF[middle] < value) low = middle + 1;
            else high = middle;
        }
        if (low == 0) return 0;
        if (low == NOISE_CDF.length) return 1;
        double before = NOISE_CDF[low - 1];
        double after = NOISE_CDF[low];
        double fraction = after > before ? (value - before) / (after - before) : 0;
        return (low - 1 + Math.max(0, Math.min(1, fraction))) / (NOISE_CDF.length - 1.0);
    }
    private static double fractalNoise(double x, double y, double z, long seed) {
        double sum = 0, amplitude = 1, norm = 0;
        for (int octave = 0; octave < 3; octave++) {
            sum += valueNoise(x, y, z, seed + octave * 0x9E3779B97F4A7C15L) * amplitude;
            norm += amplitude;
            x *= 2.0; y *= 2.0; z *= 2.0; amplitude *= 0.5;
        }
        return sum / norm;
    }

    private static double valueNoise(double x, double y, double z, long seed) {
        int ix = fastFloor(x), iy = fastFloor(y), iz = fastFloor(z);
        double fx = smooth(x - ix), fy = smooth(y - iy), fz = smooth(z - iz);
        double c000 = random(ix, iy, iz, seed), c100 = random(ix + 1, iy, iz, seed);
        double c010 = random(ix, iy + 1, iz, seed), c110 = random(ix + 1, iy + 1, iz, seed);
        double c001 = random(ix, iy, iz + 1, seed), c101 = random(ix + 1, iy, iz + 1, seed);
        double c011 = random(ix, iy + 1, iz + 1, seed), c111 = random(ix + 1, iy + 1, iz + 1, seed);
        return lerp(lerp(lerp(c000,c100,fx),lerp(c010,c110,fx),fy),lerp(lerp(c001,c101,fx),lerp(c011,c111,fx),fy),fz);
    }

    private static double random(int x, int y, int z, long seed) {
        long h = seed ^ (x * 0x632BE59BD9B4E019L) ^ (y * 0x9E3779B185EBCA87L) ^ (z * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }
    private static int fastFloor(double v) { int i = (int)v; return v < i ? i - 1 : i; }
    private static double smooth(double t) { return t * t * (3 - 2 * t); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private record Weighted(BlockState state, double cumulative) {}
}


