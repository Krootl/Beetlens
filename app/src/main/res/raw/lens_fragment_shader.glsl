// Kinda based on https://www.shadertoy.com/view/wt2GDW

// Important to include in order to use rendered Android View to GL texture
#extension GL_OES_EGL_image_external : require

precision highp float;

// Make sure to use samplerExternalOES instead of sampler2D
uniform samplerExternalOES u_Texture;// The input texture.

uniform vec2 u_Resolution;
uniform vec2 u_LensPosition;
uniform float u_LensRadius;
uniform float u_LensAlpha;
uniform float u_LensZoom;
uniform float u_LensBend;
uniform float u_LensBorderWidth;

void main() {
    vec2 uv = gl_FragCoord.xy / u_Resolution.xy;
    uv.y = 1.0 - uv.y;

    // Fix for aspect ratio difference
    float tex_aspect_ratio = u_Resolution.x / u_Resolution.y;
    float fix_aspect_ratio = (tex_aspect_ratio + 1.0) / 2.0;
    uv = vec2(uv.x, uv.y / fix_aspect_ratio);
    uv.y -= abs(tex_aspect_ratio - fix_aspect_ratio) / 2.0;


    vec2 lens_uv = gl_FragCoord.xy / u_Resolution.xx;
    lens_uv.y = 1.0 - lens_uv.y;

    vec2 lens_pos = u_LensPosition.xy / u_Resolution.xx;
    lens_pos.y -= (u_Resolution.y - u_Resolution.x) / u_Resolution.x;

    vec2 lens_delta = (lens_uv - lens_pos);
    float lens_dist = length(lens_delta);

    // Lens refraction calculation
    vec3 incident = normalize(vec3(0.0, 0.0, -1.0));
    vec3 lens_normal = normalize(vec3(lens_delta.x, 0.85 * lens_delta.y, u_LensZoom * sqrt(u_LensBend * u_LensRadius - lens_dist*lens_dist)));
    float eta = 1.0 / 1.11;
    vec2 refract = refract(incident, lens_normal, eta).xy;
    vec3 lensColor = texture2D(u_Texture, refract + uv).rgb;

    // Black border
    float borderWidth = u_LensBorderWidth / u_Resolution.x;
    float border = smoothstep(0.0, borderWidth / 2.0, abs(lens_dist - u_LensRadius) - borderWidth);
    lensColor *= border;

    gl_FragColor = mix(vec4(0.0), vec4(lensColor, u_LensAlpha), smoothstep(u_LensRadius, u_LensRadius - (borderWidth / 2.0), lens_dist));
}