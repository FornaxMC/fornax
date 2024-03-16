#version 460 core

const vec2 vertices[6] = vec2[6](
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0, -1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0,  1.0)
);

out vec2 fTexCoord;

void main() {
    vec2 pos = vertices[gl_VertexID];
    gl_Position = vec4(pos, 1.0, 1.0);
    fTexCoord = (pos + 1.0) * 0.5;
}
