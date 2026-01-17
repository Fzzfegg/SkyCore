#version 120

varying vec2 textureCoords;

uniform sampler2D buffer_a;
uniform sampler2D buffer_b;
uniform sampler2D buffer_c;
uniform float intensive;
uniform float base;
uniform float threshold_up;
uniform float threshold_down;
uniform vec3 tint_color;
uniform float use_tint;
uniform float halo_weight;
uniform vec2 halo_offset;

void main(void){
    vec3 bloom = texture2D(buffer_b, textureCoords).rgb;
    vec3 background = texture2D(buffer_a, textureCoords).rgb;
    vec3 haloSum = vec3(0.0);
    haloSum += texture2D(buffer_c, textureCoords + vec2(halo_offset.x, 0.0)).rgb;
    haloSum += texture2D(buffer_c, textureCoords - vec2(halo_offset.x, 0.0)).rgb;
    haloSum += texture2D(buffer_c, textureCoords + vec2(0.0, halo_offset.y)).rgb;
    haloSum += texture2D(buffer_c, textureCoords - vec2(0.0, halo_offset.y)).rgb;
    haloSum += texture2D(buffer_c, textureCoords + halo_offset).rgb;
    haloSum += texture2D(buffer_c, textureCoords - halo_offset).rgb;
    haloSum += texture2D(buffer_c, textureCoords + vec2(-halo_offset.x, halo_offset.y)).rgb;
    haloSum += texture2D(buffer_c, textureCoords + vec2(halo_offset.x, -halo_offset.y)).rgb;
    vec3 halo = haloSum / 8.0;

    vec3 tintedBloom = bloom;
    if (use_tint > 0.5) {
        tintedBloom *= tint_color;
    }

    float factor = max(0.0, base + intensive);
    vec3 combined = background + tintedBloom * factor + halo * halo_weight;
    combined = clamp(combined, vec3(0.0), vec3(1.0));
    gl_FragColor = vec4(combined, 1.0);
}
