#version 120
//顶点程序
//形状在 cpu 已经定义好了
//  cpu  --- 传值过来


//接受变量

//声明 gpu 的变量 vPosition, 声明变量关键字使用 attribute, 变量类型是 vec4
//vec 坐标的意思     可以是立体坐标  xyz, 也可以是屏幕坐标 xy
attribute vec4 vPosition;

//可以将 VERTEX 赋值给 vPosition
//    float[] VERTEX = {
//        -1.0f, -1.0f,
//        1.0f, -1.0f,
//        -1.0f, 1.0f,
//        1.0f, 1.0f
//    };

//纹理坐标系
attribute vec4 vCoord;

//    float[] TEXTURE = {
//            0.0f, 0.0f,
//            1.0f, 0.0f,
//            0.0f, 1.0f,
//            1.0f, 1.0f
//    };

//将 顶点的坐标 传值给 片元程序 camera_frag.glsl, 片元程序 找到对应的像素颜色进行渲染
//只需要 片元程序 camera_frag.glsl 声明同样的变量即可
varying vec2 aCoord;

void main() {
//  在顶点程序中有内置变量, 比如 gl_Position
    gl_Position = vPosition; //opengl  形状确定, 此时还是世界坐标系
    aCoord = vCoord.xy;
}
