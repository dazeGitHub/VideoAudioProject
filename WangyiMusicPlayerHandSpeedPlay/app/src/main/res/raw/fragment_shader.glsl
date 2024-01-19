precision mediump float;
//从顶点程序 传过来的值
varying vec2 v_texPosition;
//gpu 中的三个采样器
uniform sampler2D sampler_y;
uniform sampler2D sampler_u;
uniform sampler2D sampler_v;

void main() {

    vec3 y,u,v;
//    rgb  采样得到 y, 传进来只有一个值, 所以都是 .r
    y = texture2D(sampler_y, v_texPosition).r;
    u = texture2D(sampler_u, v_texPosition).r;
    v = texture2D(sampler_v, v_texPosition).r;
//    yuv-->rgb

    //yuv 转 rgb     固定代码
    rgb.r = y + 1.403 * v;
    rgb.g = y - 0.344 * u - 0.714 * v;
    rgb.b = y + 1.770 * u;

    gl_FragColor = vec4(rgb,1);
}
