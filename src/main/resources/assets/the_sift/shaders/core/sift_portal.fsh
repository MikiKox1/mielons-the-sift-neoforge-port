#version 330
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <the_sift:portal_fields.glsl>
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
out vec4 fragColor;
void main() {
    vec2 viewport = max(ScreenSize, vec2(1.0));
    vec2 screenUv = gl_FragCoord.xy / viewport;
    vec2 uv = screenUv;
    uv.x = (uv.x - 0.5) * viewport.x / viewport.y * 0.65 + 0.5;
    vec2 ndc = screenUv * 2.0 - 1.0;
    float projectionX = max(abs(ProjMat[0][0]), 0.0001);
    float projectionY = max(abs(ProjMat[1][1]), 0.0001);
    vec2 ray = vec2(ndc.x / projectionX, ndc.y / projectionY);

    vec3 color = siftPortalField(uv, ray, GameTime * 1200.0);
    fragColor = vec4(color, 1.0);
}
