package org.lytharalab.gfbs.gltf.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexBuffer.class)
public interface VertexBufferAccessor {
    @Accessor("vertexBufferId")
    int gfbs$vertexBufferId();

    @Accessor("format")
    void gfbs$format(VertexFormat value);

    @Accessor("mode")
    void gfbs$mode(VertexFormat.Mode value);

    @Accessor("indexBufferId")
    int gfbs$indexBufferId();

    @Accessor("indexCount")
    void gfbs$indexCount(int value);

    @Accessor("indexType")
    void gfbs$indexType(VertexFormat.IndexType value);

    @Accessor("sequentialIndices")
    void gfbs$sequentialIndices(RenderSystem.AutoStorageIndexBuffer value);
}
