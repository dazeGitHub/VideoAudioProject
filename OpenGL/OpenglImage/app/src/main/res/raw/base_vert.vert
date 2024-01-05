attribute vec4 vPosition;//世界坐标系
attribute vec4 vCoord;
varying vec2 textureCoordinate; //varying 表示传值的变量, 用来传递给片元 程序 base_frag.frag
//这是固定写法
void main(){
    //内置变量： 把坐标点赋值给gl_position 就Ok了。  4 个元素  gl_Position  世界坐标系 为基础
//    opengl  矩形
    gl_Position = vPosition;
    textureCoordinate = vCoord.xy;
}