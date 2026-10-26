package org.lytharalab.gfbs.gltf.client.render;

import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;
import org.lytharalab.gfbs.gltf.api.model.MorphTarget;
import org.lytharalab.gfbs.gltf.api.model.PrimitiveMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GltfVertexTransformsTest {
    @Test
    void preparesPositionNormalAndTangentMorphsWithoutMutatingTheAsset() {
        float[] positions = {
            0, 0, 0,
            1, 0, 0,
            0, 1, 0
        };
        float[] normals = {
            0, 0, 1,
            0, 0, 1,
            0, 0, 1
        };
        float[] tangents = {
            1, 0, 0, 1,
            1, 0, 0, 1,
            1, 0, 0, 1
        };
        MorphTarget target = new MorphTarget(
            new float[]{0, 0, 2, 0, 0, 0, 0, 0, 0},
            new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0},
            new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0}
        );
        GltfPrimitive primitive = new GltfPrimitive(
            PrimitiveMode.TRIANGLES, 0, 3,
            positions, normals, tangents, null, null, null,
            null, null, new int[]{0, 1, 2}, List.of(target)
        );

        GltfVertexTransforms.PreparedGeometry prepared =
            GltfVertexTransforms.prepare(primitive, new float[]{0.5f});

        assertEquals(1.0f, prepared.positions()[2], 1.0e-6f);
        assertEquals((float) (1.0 / Math.sqrt(1.25)), prepared.normals()[2], 1.0e-6f);
        assertEquals((float) (0.5 / Math.sqrt(1.25)), prepared.tangents()[1], 1.0e-6f);
        assertArrayEquals(positions, primitive.positions());
        assertArrayEquals(normals, primitive.normals());
        assertArrayEquals(tangents, primitive.tangents());
    }

    @Test
    void zeroMorphPrepareStillReturnsWritableCopies() {
        GltfPrimitive primitive = new GltfPrimitive(
            PrimitiveMode.TRIANGLES, 0, 3,
            new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0},
            new float[]{0, 0, 1, 0, 0, 1, 0, 0, 1},
            null, null, null, null, null, null, new int[]{0, 1, 2}, List.of()
        );

        GltfVertexTransforms.PreparedGeometry prepared =
            GltfVertexTransforms.prepare(primitive, null);
        prepared.positions()[0] = 99.0f;
        prepared.normals()[0] = 99.0f;

        assertEquals(0.0f, primitive.positions()[0]);
        assertEquals(0.0f, primitive.normals()[0]);
    }

    @Test
    void zeroSkinWeightsPreserveTheOriginalVertex() {
        float[] output = new float[6];
        GltfVertexTransforms.skinVertex(
            0, 1, 2, 3, 0, 1, 0,
            new int[]{0, 0, 0, 0},
            new float[]{0, 0, 0, 0},
            new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
            },
            1,
            output
        );

        assertArrayEquals(new float[]{1, 2, 3, 0, 1, 0}, output);
    }

    @Test
    void sparseSkinWeightsIgnoreUnusedJointSlots() {
        float[] output = new float[6];
        GltfVertexTransforms.skinVertex(
            0, 0, 0, 0, 0, 0, 1,
            new int[]{0, 99, 1, -4},
            new float[]{0.25f, 0, 0.75f, 0},
            new float[]{
                1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 4, 0, 0, 1,
                1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 8, 0, 1
            },
            2,
            output
        );

        assertArrayEquals(new float[]{1, 6, 0, 0, 0, 1}, output, 1.0e-6f);
    }

    @Test
    void preparedSkinningMatchesTheGeneralPathAcrossInfluenceCounts() {
        float[] positions = {1, 2, 3, -2, 0.5f, 4, 0, -1, 2};
        float[] normals = {0, 1, 0, 1, 0, 0, 0, 0, 1};
        int[] joints = {
            0, 1, 1, 0,
            1, 99, 0, -4,
            0, 1, 0, 1
        };
        float[] weights = {
            0.25f, 0.75f, 0, 0,
            0.4f, 0, 0.6f, 0,
            0, 0, 0, 0
        };
        float[] palette = {
            0, -1, 0, 0,
            1, 0, 0, 0,
            0, 0, 1, 0,
            3, -2, 1, 1,
            1, 0, 0, 0,
            0, 0, -1, 0,
            0, 1, 0, 0,
            -4, 2, 6, 1
        };
        GltfVertexTransforms.SkinningData prepared =
            GltfVertexTransforms.prepareSkinning(joints, weights, 3, 2);

        for (int vertex = 0; vertex < 3; vertex++) {
            int position = vertex * 3;
            float[] expected = new float[6];
            float[] actual = new float[6];
            GltfVertexTransforms.skinVertex(vertex,
                positions[position], positions[position + 1], positions[position + 2],
                normals[position], normals[position + 1], normals[position + 2],
                joints, weights, palette, 2, expected);
            GltfVertexTransforms.skinPreparedVertex(vertex,
                positions[position], positions[position + 1], positions[position + 2],
                normals[position], normals[position + 1], normals[position + 2],
                palette, prepared, actual);
            assertArrayEquals(expected, actual, 1.0e-5f);
        }
    }

    @Test
    void fmaPreparedSkinningMatchesTheReferenceForOneToFourInfluences() {
        float[] positions = {
            1.25f, -2.5f, 0.75f,
            -3.0f, 0.125f, 4.5f,
            2.0f, 3.0f, -1.0f,
            -0.5f, 1.75f, 2.25f
        };
        float[] normals = {
            0.0f, 1.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            0.57735026f, 0.57735026f, 0.57735026f
        };
        int[] joints = {
            0, 99, -1, 99,
            0, 1, 99, -1,
            0, 1, 2, 99,
            0, 1, 2, 3
        };
        float[] weights = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.35f, 0.65f, 0.0f, 0.0f,
            0.15f, 0.25f, 0.60f, 0.0f,
            0.10f, 0.20f, 0.30f, 0.40f
        };
        float[] palette = {
            0.8f, -0.2f, 0.1f, 0.0f,
            0.3f, 1.1f, 0.4f, 0.0f,
            -0.1f, 0.2f, 0.9f, 0.0f,
            2.5f, -1.25f, 0.5f, 1.0f,
            0.7f, 0.1f, -0.4f, 0.0f,
            -0.3f, 1.2f, 0.2f, 0.0f,
            0.5f, -0.1f, 0.8f, 0.0f,
            -1.5f, 2.0f, 3.25f, 1.0f,
            1.0f, 0.25f, 0.1f, 0.0f,
            -0.2f, 0.85f, 0.3f, 0.0f,
            0.05f, 0.4f, 1.15f, 0.0f,
            0.75f, -2.25f, 1.5f, 1.0f,
            0.9f, -0.1f, 0.35f, 0.0f,
            0.2f, 1.05f, -0.15f, 0.0f,
            -0.25f, 0.3f, 0.95f, 0.0f,
            -0.5f, 0.25f, 1.75f, 1.0f
        };
        GltfVertexTransforms.SkinningData prepared =
            GltfVertexTransforms.prepareSkinning(joints, weights, 4, 4);

        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * 3;
            float[] expected = new float[6];
            float[] actual = new float[6];
            GltfVertexTransforms.skinVertex(vertex,
                positions[offset], positions[offset + 1], positions[offset + 2],
                normals[offset], normals[offset + 1], normals[offset + 2],
                joints, weights, palette, 4, expected);
            GltfVertexTransforms.skinPreparedVertex(vertex,
                positions[offset], positions[offset + 1], positions[offset + 2],
                normals[offset], normals[offset + 1], normals[offset + 2],
                palette, prepared, actual);
            assertArrayEquals(expected, actual, 2.0e-5f);
        }
    }

    @Test
    void singleInfluencePreparedSkinningUsesEquivalentTransform() {
        float[] palette = {
            0.8f, -0.2f, 0.1f, 0.0f,
            0.3f, 1.1f, 0.4f, 0.0f,
            -0.1f, 0.2f, 0.9f, 0.0f,
            2.5f, -1.25f, 0.5f, 1.0f
        };
        int[] joints = {
            0, 9, -1, 9,
            0, 1, 9, -1,
            0, 1, 2, 9
        };
        float[] weights = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.75f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 0.0f
        };
        GltfVertexTransforms.SkinningData prepared =
            GltfVertexTransforms.prepareSkinning(joints, weights, 3, 1);

        for (int vertex = 0; vertex < 3; vertex++) {
            float[] expected = new float[6];
            float[] actual = new float[6];
            GltfVertexTransforms.skinVertex(vertex, 1.25f, -2.5f, 0.75f,
                0.57735026f, 0.57735026f, 0.57735026f,
                joints, weights, palette, 1, expected);
            GltfVertexTransforms.skinPreparedVertex(vertex, 1.25f, -2.5f, 0.75f,
                0.57735026f, 0.57735026f, 0.57735026f,
                palette, prepared, actual);
            assertArrayEquals(expected, actual, 2.0e-5f);
        }
    }

    @Test
    void preparedSkinningUsesPerAssetJointCountsWithoutAFixedRigLimit() {
        for (int jointCount : new int[]{1, 2, 43, 96}) {
            int[] joints = {jointCount - 1, 0, 0, 0};
            float[] weights = {1.0f, 0.0f, 0.0f, 0.0f};
            float[] palette = new float[jointCount * 16];
            for (int joint = 0; joint < jointCount; joint++) {
                int matrix = joint * 16;
                palette[matrix] = 1.0f;
                palette[matrix + 5] = 1.0f;
                palette[matrix + 10] = 1.0f;
                palette[matrix + 15] = 1.0f;
                palette[matrix + 12] = joint;
            }

            GltfVertexTransforms.SkinningData prepared =
                GltfVertexTransforms.prepareSkinning(joints, weights, 1, jointCount);
            float[] actual = new float[6];
            float[] expected = new float[6];
            GltfVertexTransforms.skinPreparedVertex(0, 1, 2, 3, 0, 0, 1,
                palette, prepared, actual);
            GltfVertexTransforms.skinVertex(0, 1, 2, 3, 0, 0, 1,
                joints, weights, palette, jointCount, expected);

            assertArrayEquals(expected, actual, 1.0e-6f);
            assertEquals(jointCount, actual[0]);
        }
    }

    @Test
    void indexedSkinningTransformsRepeatedVerticesOnlyOnceAndMatchesReference() {
        float[] positions = {0, 0, 0, 1, 0, 0, 0, 1, 0};
        float[] normals = {0, 0, 1, 0, 0, 1, 0, 0, 1};
        int[] joints = {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
        float[] weights = {
            1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0
        };
        float[] palette = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            2, 3, 4, 1
        };
        int[] indices = {0, 1, 2, 0, 2, 1};
        float[] batch = new float[positions.length * 2];
        int[] stamps = new int[positions.length / 3];

        int transformed = GltfVertexTransforms.skinIndexedVertices(
            indices, positions, normals, joints, weights, palette, 1, batch, stamps, 1,
            new float[6]
        );

        assertEquals(3, transformed);
        for (int vertex = 0; vertex < positions.length / 3; vertex++) {
            float[] expected = new float[6];
            int position = vertex * 3;
            GltfVertexTransforms.skinVertex(vertex, positions[position], positions[position + 1],
                positions[position + 2], normals[position], normals[position + 1],
                normals[position + 2], joints, weights, palette, 1, expected);
            assertArrayEquals(expected, java.util.Arrays.copyOfRange(batch, vertex * 6, vertex * 6 + 6));
        }
    }

    @Test
    void computesAStableTriangleNormal() {
        assertArrayEquals(
            new float[]{0, 0, 1},
            GltfVertexTransforms.faceNormal(
                new float[]{0, 0, 0, 1, 0, 0, 0, 1, 0},
                0, 1, 2
            ),
            1.0e-6f
        );
    }
}
