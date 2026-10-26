package org.lytharalab.gfbs.gltf.client.render;

import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackedSkinnedVertexTest {
    @Test
    void negligibleWeightsRetainTheCpuRestPoseContract() {
        ByteBuffer data = MemoryUtil.memAlloc(PackedVertexConsumer.SKINNED_STRIDE);
        try {
            PackedVertexConsumer consumer = new PackedVertexConsumer(data, PackedVertexConsumer.SKINNED_STRIDE);
            consumer.writeSkinnedVertex(1, 2, 3, 255, 255, 255, 255,
                0, 0, 0, 0, 0, 1, 0,
                new int[]{0, 1, 2, 3}, new float[]{1.0e-10f, 0, 0, 0}, 0, 4);
            long address = MemoryUtil.memAddress(data, 0) + GltfSkinnedVertexFormat.WEIGHTS_OFFSET;
            for (int i = 0; i < 4; i++) assertEquals(0.0f, MemoryUtil.memGetFloat(address + i * 4L));
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    @Test
    void packsFourJointIndicesAndNormalizedWeightsAfterVanillaEntityData() {
        ByteBuffer data = MemoryUtil.memAlloc(PackedVertexConsumer.SKINNED_STRIDE);
        try {
            PackedVertexConsumer consumer = new PackedVertexConsumer(
                data, PackedVertexConsumer.SKINNED_STRIDE);
            consumer.writeSkinnedVertex(1, 2, 3, 255, 255, 255, 255,
                0.25f, 0.5f, 0, 0, 0, 1, 0,
                new int[]{0, 1, 2, 99}, new float[]{0.25f, 0.25f, 0.5f, 0}, 0, 43);
            assertEquals(PackedVertexConsumer.SKINNED_STRIDE, consumer.finish());

            // finish() advances position to the written byte count; inspect from byte zero.
            long address = MemoryUtil.memAddress(data, 0);
            int joints = GltfSkinnedVertexFormat.JOINTS_OFFSET;
            int weights = GltfSkinnedVertexFormat.WEIGHTS_OFFSET;
            assertEquals(0.0f, MemoryUtil.memGetFloat(address + joints));
            assertEquals(1.0f, MemoryUtil.memGetFloat(address + joints + 4));
            assertEquals(2.0f, MemoryUtil.memGetFloat(address + joints + 8));
            assertEquals(42.0f, MemoryUtil.memGetFloat(address + joints + 12));
            assertEquals(0.25f, MemoryUtil.memGetFloat(address + weights));
            assertEquals(0.25f, MemoryUtil.memGetFloat(address + weights + 4));
            assertEquals(0.5f, MemoryUtil.memGetFloat(address + weights + 8));
            assertEquals(0.0f, MemoryUtil.memGetFloat(address + weights + 12));
        } finally {
            MemoryUtil.memFree(data);
        }
    }
}
