#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord;
layout(location = 1) in vec4 vertexColor;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

void main() {
    vec4 color = texture(Sampler0, texCoord) * vertexColor;
    if (color.a <= 0.001) {
        discard;
    }
    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #elif defined(OIT_ACCUMULATE)
    fragColor = sampleColorForAccumulation(color);
    #else
    fragColor = color;
    #endif
}
