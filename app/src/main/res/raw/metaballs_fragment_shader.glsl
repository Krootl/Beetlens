precision highp float;

uniform vec2 u_Resolution;

uniform vec2 u_FooPosition;
uniform float u_FooSize;
uniform vec3 u_FooColor;

uniform vec2 u_BarPosition;
uniform float u_BarSize;
uniform vec3 u_BarColor;

vec4 gimmeMetaball(vec2 uv, vec2 position, float radius, vec3 color)
{
    float distance = pow(radius, 2.) / (pow(uv.x - position.x, 2.) + pow(uv.y - position.y, 2.));
    return vec4(color * distance, distance);
}

vec3 SRGB(vec3 color) {
    return color / 255.0;
}

// FooBar metaballs
void main()
{
    vec2 uv = gl_FragCoord.xy/u_Resolution.xx;

    float fooRadius = u_FooSize / u_Resolution.x / 2.;
    vec2 fooPosition = u_FooPosition / u_Resolution.xx;
    fooPosition.y = u_Resolution.y / u_Resolution.x - fooPosition.y;
    vec4 fooColor = gimmeMetaball(uv, fooPosition, fooRadius, SRGB(u_FooColor));

    float barRadius = u_BarSize / u_Resolution.x / 2.;
    vec2 barPosition = u_BarPosition / u_Resolution.xx;
    barPosition.y = u_Resolution.y / u_Resolution.x - barPosition.y;
    vec4 barColor = gimmeMetaball(uv, barPosition, barRadius, SRGB(u_BarColor));

    float res = (fooColor.a + barColor.a);
    float threshold = step(1.0, res);

    vec3 color = (fooColor.rgb + barColor.rgb) / res;
    color *= threshold;
    color = clamp(color, 0., 1.);

    float alpha = 1.0 - step(0.0, - length(color));

    gl_FragColor = vec4(color, alpha);
}