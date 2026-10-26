package org.lytharalab.gfbs.gltf.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.lytharalab.gfbs.gltf.GFBSglTF;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.node.GltfPrimitiveKey;
import org.lytharalab.gfbs.gltf.api.model.GltfMaterial;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

/** Owns animated GPU buffers for each runtime instance and releases them with that instance. */
final class GltfSkinnedGpuCache {
    private static final Map<GltfInstance, Map<GltfPrimitiveKey,
        IdentityHashMap<GltfMaterial, EnumMap<GltfGeometryPipeline.PassKind,
            GltfSkinnedGpuPrimitive>>>> INSTANCES = new IdentityHashMap<>();

    private GltfSkinnedGpuCache() {}

    static GltfSkinnedGpuPrimitive get(GltfInstance instance, GltfPrimitiveKey key,
                                       GltfPrimitive primitive, GltfMaterial material,
                                       GltfGeometryPipeline.PassKind pass, int jointCount) {
        RenderSystem.assertOnRenderThread();
        Map<GltfPrimitiveKey, IdentityHashMap<GltfMaterial,
            EnumMap<GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive>>> meshes =
            INSTANCES.computeIfAbsent(instance, ignored -> new HashMap<>());
        IdentityHashMap<GltfMaterial, EnumMap<GltfGeometryPipeline.PassKind,
            GltfSkinnedGpuPrimitive>> materials = meshes.computeIfAbsent(key,
                ignored -> new IdentityHashMap<>());
        EnumMap<GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive> passes =
            materials.computeIfAbsent(material,
                ignored -> new EnumMap<>(GltfGeometryPipeline.PassKind.class));
        if (passes.containsKey(pass)) return passes.get(pass);

        try {
            GltfSkinnedGpuPrimitive compiled =
                new GltfSkinnedGpuPrimitive(primitive, material, pass, jointCount);
            passes.put(pass, compiled);
            return compiled;
        } catch (RuntimeException failure) {
            passes.put(pass, null);
            GFBSglTF.LOGGER.warn(
                "Falling back to streamed skinned glTF geometry for {} node {} mesh {} primitive {} ({})",
                instance.asset().id(), key.nodeIndex(), key.meshIndex(), key.primitiveIndex(), pass, failure
            );
            return null;
        }
    }

    static void disable(GltfInstance instance, GltfPrimitiveKey key, GltfMaterial material,
                        GltfGeometryPipeline.PassKind pass, RuntimeException failure) {
        RenderSystem.assertOnRenderThread();
        Map<GltfPrimitiveKey, IdentityHashMap<GltfMaterial,
            EnumMap<GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive>>> meshes =
            INSTANCES.get(instance);
        if (meshes == null) return;
        IdentityHashMap<GltfMaterial, EnumMap<GltfGeometryPipeline.PassKind,
            GltfSkinnedGpuPrimitive>> materials = meshes.get(key);
        if (materials == null) return;
        EnumMap<GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive> passes =
            materials.get(material);
        if (passes == null) return;
        GltfSkinnedGpuPrimitive stale = passes.put(pass, null);
        if (stale != null) stale.close();
        GFBSglTF.LOGGER.warn(
            "Disabling indexed skinned GPU path for {} node {} mesh {} primitive {} ({})",
            instance.asset().id(), key.nodeIndex(), key.meshIndex(), key.primitiveIndex(), pass,
            failure
        );
    }

    static void remove(GltfInstance instance) {
        runOnRenderThread(() -> close(INSTANCES.remove(instance)));
    }

    static void removeAsset(ResourceLocation id) {
        RenderSystem.assertOnRenderThread();
        Iterator<Map.Entry<GltfInstance, Map<GltfPrimitiveKey,
            IdentityHashMap<GltfMaterial, EnumMap<GltfGeometryPipeline.PassKind,
                GltfSkinnedGpuPrimitive>>>>> iterator = INSTANCES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<GltfInstance, Map<GltfPrimitiveKey,
                IdentityHashMap<GltfMaterial, EnumMap<GltfGeometryPipeline.PassKind,
                    GltfSkinnedGpuPrimitive>>>> entry = iterator.next();
            if (entry.getKey().asset().id().equals(id)) {
                iterator.remove();
                close(entry.getValue());
            }
        }
    }

    static void clear() {
        runOnRenderThread(() -> {
            for (Map<GltfPrimitiveKey, IdentityHashMap<GltfMaterial, EnumMap<
                GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive>>> meshes : INSTANCES.values()) {
                close(meshes);
            }
            INSTANCES.clear();
        });
    }

    private static void close(Map<GltfPrimitiveKey, IdentityHashMap<GltfMaterial, EnumMap<
        GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive>>> meshes) {
        if (meshes == null) return;
        for (IdentityHashMap<GltfMaterial, EnumMap<GltfGeometryPipeline.PassKind,
            GltfSkinnedGpuPrimitive>> materials : meshes.values()) {
            for (EnumMap<GltfGeometryPipeline.PassKind, GltfSkinnedGpuPrimitive> passes
                : materials.values()) {
                for (GltfSkinnedGpuPrimitive primitive : passes.values()) {
                    if (primitive != null) primitive.close();
                }
            }
        }
    }

    private static void runOnRenderThread(Runnable task) {
        if (RenderSystem.isOnRenderThread()) task.run();
        else RenderSystem.recordRenderCall(task::run);
    }
}
