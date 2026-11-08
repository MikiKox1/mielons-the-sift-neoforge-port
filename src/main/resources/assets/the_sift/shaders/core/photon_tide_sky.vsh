#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 0) out vec2 texCoord0;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // RenderPearl ZERO_TO_ONE reversed Z: clear/background depth is zero.
    clip.z = 0.0;
    gl_Position = clip;
    texCoord0 = UV0;
}
