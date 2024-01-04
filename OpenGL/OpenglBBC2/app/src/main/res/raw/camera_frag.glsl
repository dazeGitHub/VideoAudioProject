//如果用到图形采样器 samplerExternalOES, 那么需要添加如下代码导包
#extension GL_OES_EGL_image_external : require
//声明精度
precision lowp float;

//片元 main  坐标值
varying vec2 aCoord;
//定义一个采样器
uniform samplerExternalOES  vTexture;

void main() {
    float x = aCoord.x;
//  上色时需要使用纹理坐标系
//  texture2D 是自带的采样器, 它会根据 图层 采样对应的像素值
//  这个 rgba 就是像素
    vec4 rgba = texture2D(vTexture, vec2(x, aCoord.y));

//  4分屏   9 分屏 作业
//  gl_FragColor = vec4(0xff, 0, 0, 0);     //  例如赋值为红色, 那么画面就是静止的红色
//  gl_FragColor = vec4(255, 0, 0, rgba.a); //  同样是红色       rgb.a 表示不透明度
//  gl_FragColor = vec4(0, 255, 0, rgba.a); //  同样是红色
    gl_FragColor = rgba;

//  灰色就是每个通道都是平均值
//    float aveColor = (rgba.r + rgba.g + rgba.b) / 3.0;
//    gl_FragColor = vec4(aveColor, aveColor, aveColor, rgba.a);
}
