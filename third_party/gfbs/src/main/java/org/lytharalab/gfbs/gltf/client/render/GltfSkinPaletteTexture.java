package org.lytharalab.gfbs.gltf.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;

/** Per-instance RGBA32F texture storing one column of each joint matrix per texel. */
final class GltfSkinPaletteTexture implements AutoCloseable {
    private final int jointCount;
    private final int textureId;
    private final FloatBuffer upload;
    private boolean closed;

    GltfSkinPaletteTexture(int jointCount) {
        RenderSystem.assertOnRenderThread();
        if (jointCount <= 0 || jointCount > 256) {
            throw new IllegalArgumentException("GPU skinning supports 1..256 joints");
        }
        this.jointCount = jointCount;
        this.upload = MemoryUtil.memAllocFloat(jointCount * 16);
        this.textureId = GL11C.glGenTextures();
        int active = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13C.GL_TEXTURE3);
        int previous = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        int unpackBuffer = GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING);
        RenderSystem.bindTexture(textureId);
        try {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL30C.GL_RGBA32F,
                1, jointCount * 4, 0, GL11C.GL_RGBA, GL11C.GL_FLOAT, (ByteBuffer) null);
        } catch (RuntimeException | Error failure) {
            RenderSystem.deleteTexture(textureId);
            MemoryUtil.memFree(upload);
            throw failure;
        } finally {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
            RenderSystem.bindTexture(previous);
            RenderSystem.activeTexture(active);
        }
    }

    int textureId() {
        return textureId;
    }

    void upload(float[] matrices) {
        RenderSystem.assertOnRenderThread();
        if (matrices == null || matrices.length < jointCount * 16) {
            throw new IllegalArgumentException("Joint palette does not match the GPU skin texture");
        }
        upload.clear();
        upload.put(matrices, 0, jointCount * 16).flip();
        int active = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13C.GL_TEXTURE3);
        int previous = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        RenderSystem.bindTexture(textureId);
        // NativeImage uploads leave these global unpack fields set to atlas subregions.
        // A stale row length or skip would make the driver read past our small palette buffer.
        int rowLength = GL11C.glGetInteger(GL11C.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11C.glGetInteger(GL11C.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11C.glGetInteger(GL11C.GL_UNPACK_SKIP_PIXELS);
        int alignment = GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT);
        int swapBytes = GL11C.glGetInteger(GL11C.GL_UNPACK_SWAP_BYTES);
        int unpackBuffer = GL11C.glGetInteger(GL21C.GL_PIXEL_UNPACK_BUFFER_BINDING);
        try {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, 0);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, 0);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, 0);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 4);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SWAP_BYTES, 0);
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0,
                1, jointCount * 4, GL11C.GL_RGBA, GL11C.GL_FLOAT, upload);
        } finally {
            GL15C.glBindBuffer(GL21C.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, skipPixels);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, alignment);
            GL11C.glPixelStorei(GL11C.GL_UNPACK_SWAP_BYTES, swapBytes);
            RenderSystem.bindTexture(previous);
            RenderSystem.activeTexture(active);
        }
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        if (closed) return;
        closed = true;
        // Keep Minecraft's per-texture-unit binding cache in sync with GL deletion.
        RenderSystem.deleteTexture(textureId);
        MemoryUtil.memFree(upload);
    }
}
