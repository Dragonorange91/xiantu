package org.lytharalab.gfbs.gltf.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderTypes;
import org.lytharalab.gfbs.gltf.api.model.AlphaMode;
import org.lytharalab.gfbs.gltf.api.model.GltfMaterial;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;
import org.lytharalab.gfbs.gltf.mixin.VertexBufferAccessor;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.ByteBuffer;

/** Static source vertices with per-instance GPU joint-palette skinning. */
final class GltfSkinnedGpuPrimitive implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final long MIN_UPLOAD_INTERVAL_NANOS = 1_000_000_000L / 60L;

    private final GltfPrimitive primitive;
    private final GltfMaterial material;
    private final GltfGeometryPipeline.MaterialPass pass;
    private final int jointCount;
    private final int[] indices;
    private final VertexBuffer buffer;
    private final VertexFormat.IndexType indexType;
    private final GltfSkinPaletteTexture palette;
    private final ByteBuffer vertexData;
    private final PackedVertexConsumer packedConsumer;
    private long uploadedPoseRevision = Long.MIN_VALUE;
    private long uploadedNodeRevision = Long.MIN_VALUE;
    private long lastUploadNanos;
    private boolean indexInstalled;

    GltfSkinnedGpuPrimitive(GltfPrimitive primitive, GltfMaterial material,
                            GltfGeometryPipeline.PassKind passKind, int jointCount) {
        this.primitive = primitive;
        this.material = material;
        this.pass = passKind == GltfGeometryPipeline.PassKind.BASE
            ? GltfGeometryPipeline.MaterialPass.base(material, 1.0f, 1.0f, 1.0f)
            : GltfGeometryPipeline.MaterialPass.emissiveGpu(material);
        this.jointCount = jointCount;
        this.indices = triangleIndices(primitive);
        this.indexType = VertexFormat.IndexType.least(primitive.vertexCount());
        this.vertexData = MemoryUtil.memAlloc(Math.multiplyExact(
            primitive.vertexCount(), PackedVertexConsumer.SKINNED_STRIDE));
        this.packedConsumer = new PackedVertexConsumer(vertexData, PackedVertexConsumer.SKINNED_STRIDE);
        VertexBuffer allocatedBuffer = null;
        try {
            allocatedBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            this.palette = new GltfSkinPaletteTexture(jointCount);
            this.buffer = allocatedBuffer;
        } catch (RuntimeException | Error failure) {
            if (allocatedBuffer != null) allocatedBuffer.close();
            MemoryUtil.memFree(vertexData);
            throw failure;
        }
    }

    boolean shouldUpload(long poseRevision, long nodeRevision, long nowNanos) {
        return shouldUpload(poseRevision, nodeRevision, uploadedPoseRevision,
            uploadedNodeRevision, lastUploadNanos, nowNanos);
    }

    void update(float[] skin, int jointCount, long poseRevision, long nodeRevision,
                long nowNanos) {
        RenderSystem.assertOnRenderThread();
        if (jointCount != this.jointCount) {
            throw new IllegalArgumentException("Skin joint count changed for a cached primitive");
        }
        if (!shouldUpload(poseRevision, nodeRevision, nowNanos)) return;

        if (!indexInstalled) {
            LOGGER.info("[GFBS GPU skin] First static upload instance={} vertices={} indices={} joints={} pass={}",
                Integer.toHexString(System.identityHashCode(this)), primitive.vertexCount(),
                indices.length, jointCount, pass.kind());
            vertexData.clear();
            packedConsumer.reset();
            GltfGeometryPipeline.emitGpuSkinnedVertices(
                packedConsumer, primitive, material, pass, jointCount);
            int expectedBytes = Math.multiplyExact(
                primitive.vertexCount(), PackedVertexConsumer.SKINNED_STRIDE);
            if (packedConsumer.finish() != expectedBytes) {
                throw new IllegalStateException("Packed GPU skin vertex count did not match the primitive");
            }
            vertexData.flip();
            buffer.bind();
            try {
                VertexBufferAccessor accessor = (VertexBufferAccessor) (Object) buffer;
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, accessor.gfbs$vertexBufferId());
                GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, vertexData, GL15C.GL_STATIC_DRAW);
                accessor.gfbs$format(GltfSkinnedVertexFormat.FORMAT);
                accessor.gfbs$mode(VertexFormat.Mode.TRIANGLES);
                GltfSkinnedVertexFormat.FORMAT.setupBufferState();
                GltfGpuPrimitive.disablePerInstanceAttributes();
                installStaticIndices();
            } finally {
                VertexBuffer.unbind();
            }
        }

        palette.upload(skin);
        uploadedPoseRevision = poseRevision;
        uploadedNodeRevision = nodeRevision;
        lastUploadNanos = nowNanos;
    }

    static boolean shouldUpload(long poseRevision, long nodeRevision,
                                long uploadedPoseRevision, long uploadedNodeRevision,
                                long lastUploadNanos, long nowNanos) {
        if (uploadedPoseRevision == Long.MIN_VALUE || uploadedNodeRevision == Long.MIN_VALUE) {
            return true;
        }
        if (poseRevision == uploadedPoseRevision && nodeRevision == uploadedNodeRevision) {
            return false;
        }
        return nowNanos - lastUploadNanos >= MIN_UPLOAD_INTERVAL_NANOS;
    }

    static int[] triangleIndices(GltfPrimitive primitive) {
        int[] source = primitive.indices();
        int count = source == null ? primitive.vertexCount() : source.length;
        return switch (primitive.mode()) {
            case TRIANGLES -> {
                int triangleCount = count / 3;
                int[] result = new int[triangleCount * 3];
                for (int i = 0; i < result.length; i++) result[i] = index(source, i);
                yield result;
            }
            case TRIANGLE_STRIP -> {
                int triangleCount = Math.max(0, count - 2);
                int[] result = new int[triangleCount * 3];
                for (int triangle = 0; triangle < triangleCount; triangle++) {
                    int out = triangle * 3;
                    int a = index(source, triangle);
                    int b = index(source, triangle + 1);
                    int c = index(source, triangle + 2);
                    if ((triangle & 1) != 0) {
                        result[out] = b;
                        result[out + 1] = a;
                    } else {
                        result[out] = a;
                        result[out + 1] = b;
                    }
                    result[out + 2] = c;
                }
                yield result;
            }
            case TRIANGLE_FAN -> {
                int triangleCount = Math.max(0, count - 2);
                int[] result = new int[triangleCount * 3];
                for (int triangle = 0; triangle < triangleCount; triangle++) {
                    int out = triangle * 3;
                    result[out] = index(source, 0);
                    result[out + 1] = index(source, triangle + 1);
                    result[out + 2] = index(source, triangle + 2);
                }
                yield result;
            }
            default -> throw new IllegalArgumentException(
                "Skinned GPU geometry requires triangle topology, got " + primitive.mode()
            );
        };
    }

    private static int index(int[] indices, int position) {
        return indices == null ? position : indices[position];
    }

    void draw(RenderType renderType, Matrix4f modelView, boolean transformDirectionalLights,
              int packedLight, int packedOverlay, float redScale, float greenScale,
              float blueScale, float alphaScale) {
        ShaderInstance shader = pass.kind() == GltfGeometryPipeline.PassKind.EMISSIVE
            ? GltfSkinShader.emissive()
            : material.alphaMode() == AlphaMode.MASK
                ? GltfSkinShader.cutout() : GltfSkinShader.solid();
        GltfSkinShader.setPaletteTexture(shader, palette.textureId(), jointCount);
        shader.safeGetUniform("DirectionalLighting").set(transformDirectionalLights ? 1 : 0);
        GltfGpuPrimitive.drawBuffer(buffer, renderType, modelView, transformDirectionalLights,
            packedLight, packedOverlay, redScale, greenScale, blueScale, alphaScale);
    }

    private void installStaticIndices() {
        VertexBufferAccessor accessor = (VertexBufferAccessor) (Object) buffer;
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, accessor.gfbs$indexBufferId());
        if (!indexInstalled) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (indexType == VertexFormat.IndexType.SHORT) {
                    ShortBuffer packed = stack.mallocShort(indices.length);
                    for (int index : indices) packed.put((short) index);
                    packed.flip();
                    GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, packed, GL15C.GL_STATIC_DRAW);
                } else {
                    IntBuffer packed = stack.mallocInt(indices.length);
                    packed.put(indices).flip();
                    GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, packed, GL15C.GL_STATIC_DRAW);
                }
            }
            indexInstalled = true;
        }
        accessor.gfbs$sequentialIndices(null);
        accessor.gfbs$indexType(indexType);
        accessor.gfbs$indexCount(indices.length);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        try {
            buffer.close();
        } finally {
            try {
                palette.close();
            } finally {
                MemoryUtil.memFree(vertexData);
            }
        }
    }
}
