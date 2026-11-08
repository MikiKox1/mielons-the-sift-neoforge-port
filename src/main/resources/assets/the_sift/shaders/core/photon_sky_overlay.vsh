#version 330

#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

in vec3 Position;
in vec2 UV0;
out vec2 texCoord0;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // MC26.1.2 is forward-Z. Only clear-depth background can pass EQUAL.
    clip.z = clip.w;
    gl_Position = clip;
    texCoord0 = UV0;
}
