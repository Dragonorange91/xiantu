package org.lytharalab.gfbs.gltf.client.render;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

public final class GltfSkinnedVertexFormat {
    static final VertexFormatElement JOINTS = new VertexFormatElement(
        3, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4
    );
    static final VertexFormatElement WEIGHTS = new VertexFormatElement(
        4, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 4
    );
    private static final VertexFormatElement PADDING = new VertexFormatElement(
        0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.PADDING, 1
    );
    public static final VertexFormat FORMAT = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
        .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
        .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
        .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
        .put("UV1", DefaultVertexFormat.ELEMENT_UV1)
        .put("UV2", DefaultVertexFormat.ELEMENT_UV2)
        .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
        .put("Padding", PADDING)
        .put("UV3", JOINTS)
        .put("UV4", WEIGHTS)
        .build());
    static final int JOINTS_OFFSET = FORMAT.getOffset(FORMAT.getElements().indexOf(JOINTS));
    static final int WEIGHTS_OFFSET = FORMAT.getOffset(FORMAT.getElements().indexOf(WEIGHTS));

    private GltfSkinnedVertexFormat() {}
}
