#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform FocusConfig { vec4 Settings; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 color = texture(InSampler, texCoord);
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    float strength = Settings.x;
    vec3 softened = mix(color.rgb, vec3(luminance), strength);
    softened *= vec3(1.0 - strength * 0.10, 1.0, 1.0 + strength * 0.10);
    vec2 centered = texCoord * 2.0 - 1.0;
    float edge = smoothstep(0.42, 1.35, length(centered));
    softened *= 1.0 - edge * Settings.y * strength * 0.38;
    softened = mix(softened, vec3(0.32, 0.64, 0.62), edge * strength * 0.06);
    // Original fine contour lines, kept at the edge so aiming remains clear.
    float contour = pow(max(0.0, cos(length(centered) * 90.0)), 24.0);
    softened += vec3(0.38, 0.68, 0.64) * contour * edge * Settings.z * strength * 0.06;
    fragColor = vec4(softened, color.a);
}
