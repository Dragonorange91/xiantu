package com.longju.xiantu.client.equipment;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.*;
import java.util.function.Consumer;

/** Native HumanoidModel articulation with UV quads exported from the actual Blockbench scene. */
public final class CangmingArmorModel extends HumanoidModel<LivingEntity> {
    private static final Map<String, Map<EquipmentSlot, CangmingArmorModel>> CACHE = new HashMap<>();
    private static final Set<String> ATTEMPTED = new HashSet<>();
    private CangmingArmorModel(ModelPart root) { super(root); }
    public static void clear() { CACHE.clear(); ATTEMPTED.clear(); }
    public static void install(Consumer<IClientItemExtensions> consumer) {
        install(consumer, "cangming_zhenyue");
    }
    public static void install(Consumer<IClientItemExtensions> consumer, String modelName) {
        consumer.accept(new IClientItemExtensions() {
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                    EquipmentSlot slot, HumanoidModel<?> original) {
                ensureLoaded(modelName);
                // Forge copies the current renderer's properties/part poses before the armor draw.
                CangmingArmorModel model = CACHE.getOrDefault(modelName, Map.of()).get(slot);
                return model == null ? original : model;
            }
        });
    }
    private static void ensureLoaded(String modelName) {
        if (!ATTEMPTED.add(modelName)) return;
        ResourceLocation resource = new ResourceLocation("xiantu", "models/armor/" + modelName + ".json");
        Map<EquipmentSlot, CangmingArmorModel> models = new EnumMap<>(EquipmentSlot.class);
        try (var reader = Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                Map<String, ModelPart> parts = new LinkedHashMap<>();
                for (String name : List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg")) {
                    List<ModelPart.Cube> cubes = new ArrayList<>();
                    for (JsonElement e : data.getAsJsonArray("cubes")) {
                        JsonObject c = e.getAsJsonObject();
                        if (c.get("slot").getAsString().equals(slot.getName()) && c.get("part").getAsString().equals(name)) {
                            JsonArray a = c.getAsJsonArray("vertices");
                            if (a.size() == 0 || a.size() % 32 != 0) throw new IllegalArgumentException("Invalid armor quad list");
                            float[] vertices = new float[a.size()];
                            for (int i = 0; i < vertices.length; i++) vertices[i] = a.get(i).getAsFloat();
                            fitToPlayer(vertices,name);
                            cubes.add(new TexturedCube(vertices));
                        }
                    }
                    parts.put(name, new ModelPart(cubes, Map.of()));
                }
                models.put(slot, new CangmingArmorModel(new ModelPart(List.of(), parts)));
            }
            CACHE.put(modelName, models);
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("Unable to load Blockbench armor prototype " + resource, e);
        }
    }
    /** Baked once around each anatomical centre; articulated pivots and UVs stay intact. */
    private static void fitToPlayer(float[] v,String part){
        boolean arm=part.endsWith("arm"),head=part.equals("head")||part.equals("hat");
        float sx=head?1.16f:arm?1.22f:1.10f,sy=head?1.06f:arm?1.05f:1.02f,sz=head?1.16f:arm?1.22f:1.16f;
        float cx=part.equals("right_arm")?-.0625f:part.equals("left_arm")?.0625f:0,cy=head?-.25f:arm?.25f:part.equals("body")?.375f:.375f;
        for(int i=0;i<v.length;i+=8){
            v[i]=cx+(v[i]-cx)*sx;v[i+1]=cy+(v[i+1]-cy)*sy;v[i+2]*=sz;
            float nx=v[i+5]/sx,ny=v[i+6]/sy,nz=v[i+7]/sz,len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
            if(len>0){v[i+5]=nx/len;v[i+6]=ny/len;v[i+7]=nz/len;}
        }
    }
    /** Immutable native quads, no image/model parsing, GLB instance or per-vertex allocation during rendering. */
    private static final class TexturedCube extends ModelPart.Cube {
        private final float[] vertices;
        private TexturedCube(float[] vertices) {
            super(0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, false, 64, 64, EnumSet.noneOf(Direction.class));
            this.vertices = vertices;
        }
        @Override public void compile(PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay,
                float red, float green, float blue, float alpha) {
            for (int i = 0; i < vertices.length; i += 8) {
                buffer.vertex(pose.pose(), vertices[i], vertices[i+1], vertices[i+2])
                        .color(red, green, blue, alpha).uv(vertices[i+3], vertices[i+4])
                        .overlayCoords(overlay).uv2(light)
                        .normal(pose.normal(), vertices[i+5], vertices[i+6], vertices[i+7]).endVertex();
            }
        }
    }
}
