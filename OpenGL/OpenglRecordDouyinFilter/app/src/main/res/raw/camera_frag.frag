#extension GL_OES_EGL_image_external : require
//必须 写的 固定的  意思   用采样器
//所有float类型数据的精度是lowp
precision lowp float;
varying vec2 aCoord;
/*samplerExternalOES*/
//采样器 不是从android的surfaceTexure中的纹理 采数据了，所以不再需要android的扩展纹理采样器了
//使用正常的 sampler2D
//samplerExternalOES 代表外部设备的采样器 (除了 GPU 外的采样器)
//摄像头采样程序放到 CameraFiler 类中
uniform samplerExternalOES vTexture;
void main(){
    //Opengl 自带函数
    vec4 rgba = texture2D(vTexture,vec2(aCoord.x,aCoord.y));
    //给 gl_FragColor 渲染就会渲染到 fbo 环境中去, 而不是渲染到屏幕上
    gl_FragColor = vec4(rgba.r,rgba.g,rgba.b,rgba.a);
}