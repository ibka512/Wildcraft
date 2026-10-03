#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 color = texture(InSampler, texCoord);
    float edge = smoothstep(0.65, 1.35, length(texCoord * 2.0 - 1.0));
    fragColor = vec4(mix(color.rgb, vec3(0.74, 0.53, 0.22), edge * 0.025), color.a);
}
