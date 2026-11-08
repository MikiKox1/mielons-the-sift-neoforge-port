#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    // The projected far plane may round differently across UV quads; write the
    // exact forward-Z clear depth. The pipeline tests EQUAL without depth writes.
    gl_FragDepth = 1.0;
    vec4 sky = texture(Sampler0, texCoord0);
    if (sky.a <= 0.001) {
        discard;
    }
    fragColor = sky * ColorModulator;
}
