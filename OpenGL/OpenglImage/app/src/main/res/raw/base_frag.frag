varying highp vec2 textureCoordinate;
uniform sampler2D inputImageTexture;//表示哪个图层
//片元      纹理坐标系  2
void main(){
//  texture2D 是采样器函数, 针对哪个图层进行采样, 其中 textureCoordinate 是坐标
    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
//    gl_FragColor = vec4(0,255,0,0);
}