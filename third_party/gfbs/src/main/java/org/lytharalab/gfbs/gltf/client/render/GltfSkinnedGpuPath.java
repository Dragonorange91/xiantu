package org.lytharalab.gfbs.gltf.client.render;

import org.lytharalab.gfbs.gltf.api.model.AlphaMode;
import org.lytharalab.gfbs.gltf.api.model.PrimitiveMode;

/** Capability-based routing for the shared animated GLB GPU path. */
final class GltfSkinnedGpuPath {
    private GltfSkinnedGpuPath() {}

    static boolean supports(boolean skinnedGeometry, boolean activeMorph, boolean shaderPack,
                            PrimitiveMode mode, AlphaMode alphaMode, boolean customRenderType,
                            int vertexCount, int jointCount, boolean hasNormals) {
        return skinnedGeometry && !activeMorph && !shaderPack
            && GltfGeometryPipeline.isTriangleMode(mode)
            && alphaMode != AlphaMode.BLEND
            && !customRenderType
            && vertexCount >= 3
            && jointCount > 0 && jointCount <= 256
            && hasNormals;
    }
}
