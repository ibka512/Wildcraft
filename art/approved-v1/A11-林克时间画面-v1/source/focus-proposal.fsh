#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform FocusConfig { vec4 Settings; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
vec3 focusShade(vec3 color, vec2 uv, float strength, float vignette, float pattern) {
    if (strength <= 0.0) return color;
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    vec3 softened = mix(color, vec3(luminance), strength);
    softened *= vec3(1.0 - strength * 0.10, 1.0, 1.0 + strength * 0.10);
    // Elliptic central region receives no edge decoration or darkening.
    float radius = length(uv * 2.0 - 1.0);
    float edge = smoothstep(0.55, 1.38, radius);
    // Reduce vignette in deep shadows; this never lifts scene brightness.
    float shadowGate = mix(0.35, 1.0, smoothstep(0.04, 0.28, luminance));
    softened *= 1.0 - edge * vignette * strength * 0.38 * shadowGate;
    // Multiplicative edge color: black stays black, no luminous overlay.
    softened *= mix(vec3(1.0), vec3(0.93, 1.01, 1.03), edge * strength * 0.35);
    // Optional original static contour. Only far peripheral area can show it.
    float peripheral = smoothstep(0.90, 1.18, radius);
    float contour = pow(max(0.0, cos(radius * 90.0)), 24.0);
    softened += vec3(0.38, 0.68, 0.64) * contour * peripheral * pattern * strength * 0.035 * shadowGate;
    return clamp(softened, 0.0, 1.0);
}
void main() {
    vec4 color = texture(InSampler, texCoord);
    fragColor = vec4(focusShade(color.rgb, texCoord, Settings.x, Settings.y, Settings.z), color.a);
}
