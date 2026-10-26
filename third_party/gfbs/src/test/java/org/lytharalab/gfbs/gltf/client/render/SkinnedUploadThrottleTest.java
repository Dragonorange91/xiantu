package org.lytharalab.gfbs.gltf.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkinnedUploadThrottleTest {
    @Test
    void uploadsTheFirstPoseImmediately() {
        assertTrue(GltfSkinnedGpuPrimitive.shouldUpload(
            1, 0, Long.MIN_VALUE, Long.MIN_VALUE, 0, 0
        ));
    }

    @Test
    void skipsAnAlreadyUploadedPose() {
        assertFalse(GltfSkinnedGpuPrimitive.shouldUpload(
            7, 3, 7, 3, 10, 10 + GltfSkinnedGpuPrimitive.MIN_UPLOAD_INTERVAL_NANOS * 2
        ));
    }

    @Test
    void waitsForTheIntervalWhenOnlyNodeTransformsChange() {
        long lastUpload = 1_000_000_000L;
        assertFalse(GltfSkinnedGpuPrimitive.shouldUpload(
            7, 4, 7, 3, lastUpload,
            lastUpload + GltfSkinnedGpuPrimitive.MIN_UPLOAD_INTERVAL_NANOS - 1
        ));
        assertTrue(GltfSkinnedGpuPrimitive.shouldUpload(
            7, 4, 7, 3, lastUpload,
            lastUpload + GltfSkinnedGpuPrimitive.MIN_UPLOAD_INTERVAL_NANOS
        ));
    }

    @Test
    void waitsForTheSixtyHertzIntervalBeforeUploadingANewPose() {
        long lastUpload = 1_000_000_000L;
        assertFalse(GltfSkinnedGpuPrimitive.shouldUpload(
            8, 3, 7, 3, lastUpload,
            lastUpload + GltfSkinnedGpuPrimitive.MIN_UPLOAD_INTERVAL_NANOS - 1
        ));
        assertTrue(GltfSkinnedGpuPrimitive.shouldUpload(
            8, 3, 7, 3, lastUpload,
            lastUpload + GltfSkinnedGpuPrimitive.MIN_UPLOAD_INTERVAL_NANOS
        ));
    }
}
