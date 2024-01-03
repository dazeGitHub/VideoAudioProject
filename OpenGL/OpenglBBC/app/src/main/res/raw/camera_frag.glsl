#version 120
//片元程序和顶点程序都有 main 函数
//片元程序的坐标值
varying vec2 aCoord;

//这个 main() 函数运行在 gpu, 而不是运行在 cpu
void main() {
    texture2D();
//  在这里上色就是上到 CameraView(继承自 GLSurfaceView) 里了, 最终渲染到 GLSurfaceView
}
