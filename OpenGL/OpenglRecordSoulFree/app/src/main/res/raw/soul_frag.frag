//纹理坐标系  1  世界坐标系  2
//   纹理   当前上色点的坐标  aCoord  对应起来
//GPU 中每个 ALU 运行一个 soul_frag.frag 程序, 很多个 soul_frag.frag 程序同时运行
varying highp vec2 aCoord;
uniform   sampler2D  vTexture; //有值 1  没有  2
//放大系数 scalePercent 不断的变化, 所以必须是 cpu  传进来, 如果 scalePercent > 1 那么增加
uniform highp float scalePercent;
//混合 透明度, 由大变小, 越大越透明
uniform lowp float mixturePercent;

void main() {
//采样器 vTexture 工具  颜色 [r  , g ,b  ,a ]
//    lowp  vec4 textureColor  =  texture2D(vTexture,aCoord);
//纹理坐标系的中心点
    highp vec2 center = vec2(0.5,0.5);
//临时变量   [0.6,0.6]  textureCoordinateToUse  会1  不会2
    highp vec2 textureCoordinateToUse = aCoord; //假设取的是该点
//  textureCoordinateToUse = [0.6,0.6], center = [0.5, 0.5], 结果就是 [0.1, 0.1]
//  [0.6,0.6]  -  [0.5, 0.5]   =    [0.1, 0.1]
    textureCoordinateToUse -= center;
//采样点 一定比     需要渲染 的坐标点要小     y 轴

//  假设 textureCoordinateToUse 是 [0.1, 0.1] , scalePercent 是 1.1
//  那么有 textureCoordinateToUse = [0.1 / 1.1, 0.1 / 1.1] 假设结果为 [0.09, 0.09]
    textureCoordinateToUse = textureCoordinateToUse / scalePercent;

//  [0.09, 0.09] + [0.5, 0.5] = [0.59, 0.9]  中心点是 [0.5, 0.5]
    textureCoordinateToUse += center;
//    [0.5,0.6]
    //    [0.5,0.6]
//   gl_FragColor= texture2D(vTexture,aCoord);
////[0.5,0.59]

//原来绘制颜色
    lowp vec4 textureColor = texture2D(vTexture, aCoord);
//      新采样颜色
    lowp vec4 textureColor2 = texture2D(vTexture,textureCoordinateToUse);

//  直接放大效果
//  gl_FragColor = textureColor2;

//  灵魂出窍效果
//  混合两个颜色 mixturePercent   1  --->0
    gl_FragColor = mix(textureColor, textureColor2, mixturePercent);//mixturePercent 是透明度, 例如 1f
}