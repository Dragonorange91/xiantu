package org.lytharalab.gfbs.gltf.client.render;

import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackedVertexConsumerTest {
    @Test
    void packsTheMinecraftNewEntityVertexLayout() {
        ByteBuffer data = MemoryUtil.memAlloc(PackedVertexConsumer.STRIDE)
            .order(ByteOrder.nativeOrder());
        try {
            PackedVertexConsumer consumer = new PackedVertexConsumer(data);
            consumer.reset();
            consumer.vertex(1.25, -2.5, 3.75)
                .color(11, 22, 33, 44)
                .uv(0.25f, 0.75f)
                .overlayCoords(0x1234, 0x5678)
                .uv2(0x2345, 0x6789)
                .normal(1.0f, -1.0f, 0.5f)
                .endVertex();

            assertEquals(PackedVertexConsumer.STRIDE, consumer.finish());
            assertEquals(1.25f, data.getFloat(0));
            assertEquals(-2.5f, data.getFloat(4));
            assertEquals(3.75f, data.getFloat(8));
            assertEquals(11, Byte.toUnsignedInt(data.get(12)));
            assertEquals(22, Byte.toUnsignedInt(data.get(13)));
            assertEquals(33, Byte.toUnsignedInt(data.get(14)));
            assertEquals(44, Byte.toUnsignedInt(data.get(15)));
            assertEquals(0.25f, data.getFloat(16));
            assertEquals(0.75f, data.getFloat(20));
            assertEquals((short) 0x1234, data.getShort(24));
            assertEquals((short) 0x5678, data.getShort(26));
            assertEquals((short) 0x2345, data.getShort(28));
            assertEquals((short) 0x6789, data.getShort(30));
            assertEquals(127, data.get(32));
            assertEquals(-127, data.get(33));
            assertEquals(63, data.get(34));
            assertEquals(0, data.get(35));
        } finally {
            MemoryUtil.memFree(data);
        }
    }
}
