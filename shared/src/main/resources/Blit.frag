#version 460 core
#extension GL_ARB_bindless_texture : enable

layout(bindless_sampler) uniform sampler2D uTex1;

in vec2 fTexCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(uTex1, fTexCoord);
}
