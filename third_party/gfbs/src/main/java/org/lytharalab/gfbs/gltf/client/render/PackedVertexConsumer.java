package org.lytharalab.gfbs.gltf.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/** Packs the fixed Minecraft NEW_ENTITY vertex layout without BufferBuilder's per-field checks. */
final class PackedVertexConsumer implements VertexConsumer {
    static final int STRIDE = DefaultVertexFormat.NEW_ENTITY.getVertexSize();
    static final int SKINNED_STRIDE = GltfSkinnedVertexFormat.FORMAT.getVertexSize();

    private final ByteBuffer buffer;
    private final long baseAddress;
    private final int stride;
    private int bytesWritten;
    private float x;
    private float y;
    private float z;
    private float u;
    private float v;
    private float normalX;
    private float normalY;
    private float normalZ;
    private int red;
    private int green;
    private int blue;
    private int alpha;
    private int overlayU;
    private int overlayV;
    private int lightU;
    private int lightV;

    PackedVertexConsumer(ByteBuffer buffer) {
        this(buffer, STRIDE);
    }

    PackedVertexConsumer(ByteBuffer buffer, int stride) {
        this.buffer = buffer;
        this.baseAddress = MemoryUtil.memAddress(buffer);
        this.stride = stride;
    }

    void reset() {
        bytesWritten = 0;
    }

    int finish() {
        buffer.position(bytesWritten);
        return bytesWritten;
    }

    void writeVertex(float x, float y, float z, int red, int green, int blue, int alpha,
                     float u, float v, int overlay, int light,
                     float normalX, float normalY, float normalZ) {
        long address = baseAddress + bytesWritten;
        writeBase(address, x, y, z, red, green, blue, alpha, u, v, overlay, light,
            normalX, normalY, normalZ);
        bytesWritten += stride;
    }

    void writeSkinnedVertex(float x, float y, float z, int red, int green, int blue, int alpha,
                            float u, float v, int overlay, int light,
                            float normalX, float normalY, float normalZ,
                            int[] joints, float[] weights, int influenceOffset, int jointCount) {
        long address = baseAddress + bytesWritten;
        writeBase(address, x, y, z, red, green, blue, alpha, u, v, overlay, light,
            normalX, normalY, normalZ);
        float totalWeight = 0.0f;
        for (int influence = 0; influence < 4; influence++) {
            totalWeight += Math.max(0.0f, weights[influenceOffset + influence]);
        }
        for (int influence = 0; influence < 4; influence++) {
            int joint = Math.max(0, Math.min(jointCount - 1, joints[influenceOffset + influence]));
            float weight = totalWeight > 1.0e-8f
                ? Math.max(0.0f, weights[influenceOffset + influence]) / totalWeight : 0.0f;
            MemoryUtil.memPutFloat(address + GltfSkinnedVertexFormat.JOINTS_OFFSET + influence * 4L, joint);
            MemoryUtil.memPutFloat(address + GltfSkinnedVertexFormat.WEIGHTS_OFFSET + influence * 4L, weight);
        }
        bytesWritten += stride;
    }

    private static void writeBase(long address, float x, float y, float z,
                                  int red, int green, int blue, int alpha,
                                  float u, float v, int overlay, int light,
                                  float normalX, float normalY, float normalZ) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + 4, y);
        MemoryUtil.memPutFloat(address + 8, z);
        MemoryUtil.memPutByte(address + 12, (byte) red);
        MemoryUtil.memPutByte(address + 13, (byte) green);
        MemoryUtil.memPutByte(address + 14, (byte) blue);
        MemoryUtil.memPutByte(address + 15, (byte) alpha);
        MemoryUtil.memPutFloat(address + 16, u);
        MemoryUtil.memPutFloat(address + 20, v);
        MemoryUtil.memPutShort(address + 24, (short) (overlay & 0xffff));
        MemoryUtil.memPutShort(address + 26, (short) (overlay >>> 16));
        MemoryUtil.memPutShort(address + 28, (short) (light & 0xffff));
        MemoryUtil.memPutShort(address + 30, (short) (light >>> 16));
        MemoryUtil.memPutByte(address + 32, (byte) (normalX * 127.0f));
        MemoryUtil.memPutByte(address + 33, (byte) (normalY * 127.0f));
        MemoryUtil.memPutByte(address + 34, (byte) (normalZ * 127.0f));
        MemoryUtil.memPutByte(address + 35, (byte) 0);
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    @Override
    public void defaultColor(int red, int green, int blue, int alpha) {
        color(red, green, blue, alpha);
    }

    @Override
    public void unsetDefaultColor() {
    }

    @Override
    public VertexConsumer uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        this.overlayU = u;
        this.overlayV = v;
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v) {
        this.lightU = u;
        this.lightV = v;
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        this.normalX = x;
        this.normalY = y;
        this.normalZ = z;
        return this;
    }

    @Override
    public void endVertex() {
        writeVertex(x, y, z, red, green, blue, alpha, u, v,
            overlayU | overlayV << 16, lightU | lightV << 16, normalX, normalY, normalZ);
    }
}
