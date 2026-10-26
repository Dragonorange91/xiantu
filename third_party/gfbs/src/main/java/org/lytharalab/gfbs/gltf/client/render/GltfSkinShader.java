package org.lytharalab.gfbs.gltf.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.lytharalab.gfbs.gltf.GFBSglTF;

import java.io.IOException;

public final class GltfSkinShader {
    private static volatile ShaderInstance solid;
    private static volatile ShaderInstance cutout;
    private static volatile ShaderInstance emissive;

    private GltfSkinShader() {}

    public static void register(RegisterShadersEvent event) {
        try {
            register(event, "gfbs_skinned_solid", loaded -> solid = loaded);
            register(event, "gfbs_skinned_cutout", loaded -> cutout = loaded);
            register(event, "gfbs_skinned_emissive", loaded -> emissive = loaded);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the GFBS skinned entity shader", exception);
        }
    }

    private static void register(RegisterShadersEvent event, String name,
                                 java.util.function.Consumer<ShaderInstance> callback) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            new ResourceLocation(GFBSglTF.MODID, name), GltfSkinnedVertexFormat.FORMAT), callback);
    }

    public static ShaderInstance solid() {
        ShaderInstance current = solid;
        if (current == null) throw new IllegalStateException("GFBS skinned shader is not loaded");
        return current;
    }

    public static ShaderInstance cutout() {
        ShaderInstance current = cutout;
        if (current == null) throw new IllegalStateException("GFBS skinned shader is not loaded");
        return current;
    }

    public static ShaderInstance emissive() {
        ShaderInstance current = emissive;
        if (current == null) throw new IllegalStateException("GFBS skinned shader is not loaded");
        return current;
    }

    static void setPaletteTexture(ShaderInstance current, int textureId, int jointCount) {
        // Minecraft 1.20.1 accepts an Integer texture id, not an IntSupplier.
        current.setSampler("SkinPalette", textureId);
        Uniform uniform = current.getUniform("JointCount");
        if (uniform == null) throw new IllegalStateException("GFBS skin shader is missing JointCount");
        uniform.set(jointCount);
    }
}
