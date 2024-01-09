//必须 写的 固定的  意思   用采样器
//所有float类型数据的精度是lowp
//precision highp float;
varying  highp vec2 aCoord;
uniform sampler2D vTexture;
//gpu 写  1   不能 2
uniform highp float scalePercent;
uniform lowp float mixturePercent;
void main(){
// [0.6,0.6]   原本显示在  采样
    highp  vec2 center=vec2(0.5,0.5);
    //  假设  [0.6,0.6]   0.8  0.8
    highp vec2 textureCoordinateToUse = aCoord;

    //  [0.6,0.6]  -  [0.5, 0.5]   =    [0.1, 0.1]
    textureCoordinateToUse-=center;
//  放大系数 [0.1/1.1, 0.1/1.1]   假设  scalePercent   =  1.1
    textureCoordinateToUse=textureCoordinateToUse/scalePercent;
//[0.09, 0.09]+ [0.5,0.5]  = [ 0.59,0.59]
    textureCoordinateToUse+=center;

//[0.6,0.6]
    lowp vec4 textureColor = texture2D(vTexture, aCoord);


    //      新采样颜色
    lowp vec4 textureColor2= texture2D(vTexture,textureCoordinateToUse);
    gl_FragColor=mix(textureColor,textureColor2,mixturePercent);
}