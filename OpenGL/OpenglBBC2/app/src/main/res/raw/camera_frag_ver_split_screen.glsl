#extension GL_OES_EGL_image_external : require
precision lowp float;
//片元 main  坐标值
varying vec2 aCoord;
//变量  第几个   0
uniform samplerExternalOES  vTexture;
//竖着分屏

void main() {
    float x = aCoord.x;

//  1. 竖着分两屏
//  y = 0 和 y = 0.5 都采集 y = 0.25 的画面, 由于摄像头本身是旋转的, y 值对应 x 值, 所以下边竖着分屏用 x 判断
//    if(x < 0.5)
//    {
//        x += 0.25;
//    }else{
//        x -= 0.25;
//    }

//  2. 竖着分三屏
    float a = 1.0 / 3.0;
    if(x < a){
        x += a;
    }else if(x > 2.0 * a){
        x -= a;
    }

    //    自带的采样器      图层 采样对应的像素值  坐标  （0,0）
    vec4 rgba = texture2D(vTexture,vec2(x, aCoord.y));
//    静止红色 gl_FragColor 输出的
    gl_FragColor = rgba;
}
