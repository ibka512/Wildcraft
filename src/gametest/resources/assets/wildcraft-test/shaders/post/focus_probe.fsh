#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 color = texture(InSampler, texCoord);
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 softened = mix(color.rgb, vec3(luminance), 0.15);
    fragColor = vec4(softened * vec3(0.985, 1.0, 1.015), color.a);
}
