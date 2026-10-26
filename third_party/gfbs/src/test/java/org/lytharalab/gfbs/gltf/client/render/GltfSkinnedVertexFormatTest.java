package org.lytharalab.gfbs.gltf.client.render;

import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfSkinnedVertexFormatTest {
    @Test
    void preservesVanillaEntityFieldsAndAddsTwoFourComponentSkinChannels() {
        assertEquals(DefaultVertexFormat.NEW_ENTITY.getVertexSize() + 32,
            GltfSkinnedVertexFormat.FORMAT.getVertexSize());
        assertEquals(DefaultVertexFormat.NEW_ENTITY.getVertexSize(), GltfSkinnedVertexFormat.JOINTS_OFFSET);
        assertEquals(GltfSkinnedVertexFormat.JOINTS_OFFSET + 16, GltfSkinnedVertexFormat.WEIGHTS_OFFSET);
        assertEquals(68, GltfSkinnedVertexFormat.FORMAT.getVertexSize());
        assertEquals(4, GltfSkinnedVertexFormat.JOINTS.getCount());
        assertEquals(4, GltfSkinnedVertexFormat.WEIGHTS.getCount());
        assertTrue(GltfSkinnedVertexFormat.FORMAT.getElementAttributeNames()
            .contains("UV3"));
        assertTrue(GltfSkinnedVertexFormat.FORMAT.getElementAttributeNames()
            .contains("UV4"));
    }
}
