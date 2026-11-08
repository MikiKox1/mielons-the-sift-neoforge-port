#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>
layout(location=0) in vec3 Position;
layout(location=1) in vec4 Color;
layout(location=0) out vec4 vertexColor;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    // Reversed depth supports both OpenGL clip ranges and the newer GPU backends.
    gl_Position.z=ProjMat[2][2]>0.5 ? -gl_Position.w : 0.0;
    vertexColor=Color*ColorModulator;
}
