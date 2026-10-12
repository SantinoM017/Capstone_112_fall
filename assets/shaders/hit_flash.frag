#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform float u_flashAmount; // 0.0 = Normal texture, 1.0 = Pure Solid White

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords) * v_color;

    // Blend current pixel RGB toward solid white (vec3(1.0)), keeping original alpha mask
    vec3 blendedRgb = mix(texColor.rgb, vec3(1.0, 1.0, 1.0), u_flashAmount);

    gl_FragColor = vec4(blendedRgb, texColor.a);
}
