#version 330
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
in vec3 Position;
in vec4 Color;
out vec4 vertexColor;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    // Reversed depth supports both OpenGL clip ranges and the newer GPU backends.
    gl_Position.z=ProjMat[2][2]>0.5 ? -gl_Position.w : 0.0;
    vertexColor=Color*ColorModulator;
}
