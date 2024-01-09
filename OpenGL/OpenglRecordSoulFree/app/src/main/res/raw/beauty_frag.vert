#extension GL_OES_EGL_image_external : require
precision lowp float;
varying vec2 aCoord;
uniform sampler2D vTexture;

//cpu传值  width 变量
uniform int width;//1000
uniform int height;//1000

vec2 blurCoordinates[20];
void main(){
//cpu--》 gpu
//     步骤没有先后   美颜本质  模糊痘痘 不模糊五官    核心
//    步长
//    1  高斯 模糊     cpu  核心 均值  width  heith  gpu
//[1000,1000]

//    vec1 singleStepOffsetX=1.0/width
//    vec1 singleStepOffsetY=1.0/height
//    x  y   [500,500]  //singleStepOffset  =1
    vec2 singleStepOffset=  vec2(1.0/float(width),1.0/float(height));

    //[490, 500]
    blurCoordinates[2] = aCoord.xy + singleStepOffset * vec2(-10.0, 0.0);
    //[510, 500]
    blurCoordinates[3] = aCoord.xy + singleStepOffset * vec2(10.0, 0.0);


    blurCoordinates[0] =aCoord.xy+singleStepOffset*vec2( 0.0,-10.0);
    //      [500,510]
    blurCoordinates[1] = aCoord.xy + singleStepOffset * vec2(0.0, 10.0);

    //GPUIMage   magiccaerma
//    三环
    blurCoordinates[4] = aCoord.xy + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5] = aCoord.xy + singleStepOffset * vec2(5.0, 8.0);
    blurCoordinates[6] = aCoord.xy + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[7] = aCoord.xy + singleStepOffset * vec2(-5.0, -8.0);
    blurCoordinates[8] = aCoord.xy + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9] = aCoord.xy + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = aCoord.xy + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = aCoord.xy + singleStepOffset * vec2(-8.0, -5.0);

//   二环
    blurCoordinates[12] = aCoord.xy + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = aCoord.xy + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = aCoord.xy + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = aCoord.xy + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = aCoord.xy + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = aCoord.xy + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = aCoord.xy + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = aCoord.xy + singleStepOffset * vec2(4.0, 4.0);
//    一环
    //    科学的取法   正态分布  一个像素   = 完整图片
    vec4 currentColor=texture2D(vTexture,aCoord);

    vec3 rgb=currentColor.rgb;
    for (int i = 0; i < 20; i++) {

        rgb+=texture2D(vTexture,blurCoordinates[i].xy).rgb;

    }

    vec4 blur = vec4(rgb*1.0/21.0,currentColor.a);
//细节 轮廓
//高反差图片
    vec4 highPassColor=currentColor-blur;

    ////    2.0 * highPassColor.r * highPassColor.r * 24.0       1  出现大于1     1   不可以 2     0  - 1
    ////     0 -1  取中间的值  r抛物线
    highPassColor.r=clamp(2.0 * highPassColor.r * highPassColor.r * 24.0,0.0,1.0);
    highPassColor.g = clamp(2.0 * highPassColor.g * highPassColor.g * 24.0, 0.0, 1.0);
    highPassColor.b = clamp(2.0 * highPassColor.b * highPassColor.b * 24.0, 0.0, 1.0);


    vec4 highPassBlur=vec4(highPassColor.rgb,1.0);
    //    20  1   21  2
//    gl_FragColor=highPassColor;

//[500,500]  +  *vec2(-10.0,0.0);
    ////    蓝色通道  作为    参考  叠加
    ////    两个颜色  原图颜色     高斯模糊的颜色
    float b =min(currentColor.b,blur.b);
    //    线性叠加  (b - 0.2) * 5.0
    float value = clamp((b - 0.2) * 5.0, 0.0, 1.0);
    //    取rgb的最大值      蓝色的值取出来         保留细节

    float maxChannelColor =max(max(highPassBlur.r, highPassBlur.g), highPassBlur.b);

    //    磨皮程度
    float intensity = 1.0; // 0.0 - 1.0f 再大会很模糊

    ////currentIntensity    细节的地方     值越小        黑色的地方 值 比较大
    float currentIntensity = (1.0 - maxChannelColor / (maxChannelColor + 0.2)) * value * intensity;


//     两个图     currentColor 原图     blur.rgb   高斯模糊图     currentIntensity
//
    vec3 r =mix(currentColor.rgb,blur.rgb,currentIntensity);
    //[500-10.0 ,500+0]
    gl_FragColor=vec4(r,1.0);
//    490  500
//singleStepOffset    [singleStepOffsetX,singleStepOffsetY]
    //Opengl 自带函数
}