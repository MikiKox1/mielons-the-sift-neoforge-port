#version 330
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
in vec3 Position;
in vec4 Color;
out vec4 vertexColor;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    // MC26.1.2 uses forward-Z: clear depth is 1.0, so restore only the far background.
    gl_Position.z=gl_Position.w;
    vertexColor=Color*ColorModulator;
}
