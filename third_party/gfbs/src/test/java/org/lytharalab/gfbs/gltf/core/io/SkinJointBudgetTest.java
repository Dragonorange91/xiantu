package org.lytharalab.gfbs.gltf.core.io;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.gltf.api.model.GltfSkin;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class SkinJointBudgetTest {
    @Test
    void acceptsFingerRigsAndExactBudgetWithoutPadding() throws Exception {
        for (int count : new int[]{1, 22, 43, 48, 52, 53, 57, 64}) {
            GltfSkin skin = new GltfSkin("rig", -1,
                IntStream.range(0, count).toArray(), new float[count * 16]);
            assertEquals(count * 16, skin.inverseBindMatrices().length);
            assertEquals(count, load(count).joints().length);
        }
    }

    @Test
    void rejectsOverBudgetInBothApiAndImporter() {
        assertEquals(64, GltfSkin.MAX_JOINTS);
        assertThrows(IllegalArgumentException.class, () -> new GltfSkin("rig", -1,
            IntStream.range(0, 65).toArray(), new float[65 * 16]));
        assertTrue(assertThrows(GltfLoadException.class, () -> load(65)).getMessage().contains("at most 64"));
    }

    private GltfSkin load(int count) throws Exception {
        String nodes = IntStream.range(0, count).mapToObj(i -> "{}").collect(Collectors.joining(","));
        String joints = IntStream.range(0, count).mapToObj(Integer::toString).collect(Collectors.joining(","));
        ByteBuffer binary = ByteBuffer.allocate(count * 64).order(ByteOrder.LITTLE_ENDIAN);
        // Identity matrices, one per joint.
        for (int joint = 0; joint < count; joint++) {
            for (int element = 0; element < 16; element++)
                binary.putFloat((joint * 16 + element) * 4, element % 5 == 0 ? 1f : 0f);
        }
        String json = "{\"asset\":{\"version\":\"2.0\"},\"nodes\":[" + nodes
            + "],\"buffers\":[{\"uri\":\"data:application/octet-stream;base64,"
            + Base64.getEncoder().encodeToString(binary.array()) + "\",\"byteLength\":" + binary.capacity()
            + "}],\"bufferViews\":[{\"buffer\":0,\"byteLength\":" + binary.capacity()
            + "}],\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":" + count
            + ",\"type\":\"MAT4\"}],\"skins\":[{\"inverseBindMatrices\":0,\"joints\":[" + joints + "]}]}";
        return new JgltfAssetLoader().load(
            ResourceLocation.fromNamespaceAndPath("test", "finger-rig.gltf"), ignored ->
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))).skins().get(0);
    }
}
