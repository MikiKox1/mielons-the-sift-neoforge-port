#version 330
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;
in vec2 UV0;
out vec2 texCoord0;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // Blaze3D ZERO_TO_ONE reversed Z: clear/background depth is zero.
    clip.z = 0.0;
    gl_Position = clip;
    texCoord0 = UV0;
}
