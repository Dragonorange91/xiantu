package org.lytharalab.gfbs.gltf.client.render;

import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.gltf.api.model.AlphaMode;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;
import org.lytharalab.gfbs.gltf.api.model.PrimitiveMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfSkinnedGpuPathTest {
    @Test
    void expandedFingerRigsStayOnGpuPath() {
        for (int count : new int[]{48, 52, 53, 57, 64}) {
            assertTrue(GltfSkinnedGpuPath.supports(true, false, false,
                PrimitiveMode.TRIANGLES, AlphaMode.OPAQUE, false, 3000, count, true));
        }
    }

    @Test
    void acceptsTriangleTopologiesWithoutModelSpecificLimits() {
        for (PrimitiveMode mode : new PrimitiveMode[]{
            PrimitiveMode.TRIANGLES, PrimitiveMode.TRIANGLE_STRIP, PrimitiveMode.TRIANGLE_FAN
        }) {
            assertTrue(GltfSkinnedGpuPath.supports(
                true, false, false, mode, AlphaMode.OPAQUE, false, 6, 43, true
            ));
        }
        assertTrue(GltfSkinnedGpuPath.supports(
            true, false, false, PrimitiveMode.TRIANGLES, AlphaMode.MASK, false, 6, 43, true
        ));
    }

    @Test
    void keepsUnsupportedRenderingFeaturesOnTheCompatibilityPath() {
        assertFalse(supported(false, false, false, PrimitiveMode.TRIANGLES,
            AlphaMode.OPAQUE, false, true));
        assertFalse(supported(true, true, false, PrimitiveMode.TRIANGLES,
            AlphaMode.OPAQUE, false, true));
        assertFalse(supported(true, false, true, PrimitiveMode.TRIANGLES,
            AlphaMode.OPAQUE, false, true));
        assertFalse(supported(true, false, false, PrimitiveMode.TRIANGLES,
            AlphaMode.BLEND, false, true));
        assertFalse(supported(true, false, false, PrimitiveMode.TRIANGLES,
            AlphaMode.OPAQUE, true, true));
        assertFalse(supported(true, false, false, PrimitiveMode.TRIANGLES,
            AlphaMode.OPAQUE, false, false));
        assertFalse(GltfSkinnedGpuPath.supports(
            true, false, false, PrimitiveMode.TRIANGLES, AlphaMode.OPAQUE,
            false, 2, 43, true
        ));
        assertFalse(supported(true, false, false, PrimitiveMode.LINES,
            AlphaMode.OPAQUE, false, true));
    }

    @Test
    void fallsBackWhenPackedJointIndicesWouldOverflow() {
        assertTrue(GltfSkinnedGpuPath.supports(true, false, false,
            PrimitiveMode.TRIANGLES, AlphaMode.OPAQUE, false, 3, 256, true));
        assertFalse(GltfSkinnedGpuPath.supports(true, false, false,
            PrimitiveMode.TRIANGLES, AlphaMode.OPAQUE, false, 3, 257, true));
    }

    @Test
    void convertsUnindexedTrianglesToAReusableIndexBuffer() {
        GltfPrimitive primitive = primitive(PrimitiveMode.TRIANGLES, null);

        assertArrayEquals(new int[]{0, 1, 2, 3, 4, 5},
            GltfSkinnedGpuPrimitive.triangleIndices(primitive));
    }

    @Test
    void convertsTriangleStripsWithAlternatingWinding() {
        GltfPrimitive primitive = primitive(PrimitiveMode.TRIANGLE_STRIP,
            new int[]{4, 2, 1, 3});

        assertArrayEquals(new int[]{4, 2, 1, 1, 2, 3},
            GltfSkinnedGpuPrimitive.triangleIndices(primitive));
    }

    @Test
    void convertsTriangleFansAroundTheirSharedFirstVertex() {
        GltfPrimitive primitive = primitive(PrimitiveMode.TRIANGLE_FAN,
            new int[]{4, 2, 1, 3});

        assertArrayEquals(new int[]{4, 2, 1, 4, 1, 3},
            GltfSkinnedGpuPrimitive.triangleIndices(primitive));
    }

    private static boolean supported(boolean skinned, boolean morph, boolean shaderPack,
                                    PrimitiveMode mode, AlphaMode alpha, boolean customType,
                                    boolean normals) {
        return GltfSkinnedGpuPath.supports(skinned, morph, shaderPack, mode, alpha,
            customType, 6, 43, normals);
    }

    private static GltfPrimitive primitive(PrimitiveMode mode, int[] indices) {
        int vertexCount = 6;
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        for (int vertex = 0; vertex < vertexCount; vertex++) normals[vertex * 3 + 2] = 1.0f;
        return new GltfPrimitive(mode, 0, vertexCount, positions, normals,
            null, null, null, null, null, null, indices, List.of());
    }
}
