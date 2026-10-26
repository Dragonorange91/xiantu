#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
in vec4 UV3;
in vec4 UV4;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D SkinPalette;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform int JointCount;
uniform int DirectionalLighting;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

mat4 jointMatrix(int joint) {
    int row = joint * 4;
    return mat4(
        texelFetch(SkinPalette, ivec2(0, row), 0),
        texelFetch(SkinPalette, ivec2(0, row + 1), 0),
        texelFetch(SkinPalette, ivec2(0, row + 2), 0),
        texelFetch(SkinPalette, ivec2(0, row + 3), 0)
    );
}

void main() {
    vec4 weights = max(UV4, vec4(0.0));
    float weightSum = dot(weights, vec4(1.0));
    vec4 sourcePosition = vec4(Position, 1.0);
    vec3 sourceNormal = Normal;
    if (weightSum > 0.00000001 && JointCount > 0) {
        weights /= weightSum;
        ivec4 joints = clamp(ivec4(UV3 + 0.5), ivec4(0), ivec4(JointCount - 1));
        mat4 skin = jointMatrix(joints.x) * weights.x
            + jointMatrix(joints.y) * weights.y
            + jointMatrix(joints.z) * weights.z
            + jointMatrix(joints.w) * weights.w;
        sourcePosition = skin * sourcePosition;
        vec3 skinnedNormal = mat3(skin) * sourceNormal;
        float normalLength = length(skinnedNormal);
        if (normalLength > 0.00000001) sourceNormal = skinnedNormal / normalLength;
    }

    gl_Position = ProjMat * ModelViewMat * sourcePosition;
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * sourcePosition.xyz, FogShape);
    vertexColor = DirectionalLighting == 0 ? Color : minecraft_mix_light(
        Light0_Direction, Light1_Direction, sourceNormal, Color
    );
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, UV1, 0);
    texCoord0 = UV0;
    normal = ProjMat * ModelViewMat * vec4(sourceNormal, 0.0);
}
