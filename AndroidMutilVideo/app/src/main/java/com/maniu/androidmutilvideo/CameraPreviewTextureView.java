package com.maniu.androidmutilvideo;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;

import com.chillingvan.canvasgl.ICanvasGL;
import com.chillingvan.canvasgl.androidCanvas.IAndroidCanvasHelper;
import com.chillingvan.canvasgl.glview.texture.GLMultiTexProducerView;
import com.chillingvan.canvasgl.glview.texture.GLTexture;
import com.chillingvan.canvasgl.glview.texture.gles.EglContextWrapper;
import com.chillingvan.canvasgl.textureFilter.BasicTextureFilter;
import com.chillingvan.canvasgl.textureFilter.TextureFilter;

import java.util.List;
//等于GLSurafceView      可以实现多屏幕 的渲染, 像自定义控件那样方便
//MultiTex 表示多纹理
public class CameraPreviewTextureView    extends GLMultiTexProducerView {
    private IAndroidCanvasHelper androidCanvasHelper =
            IAndroidCanvasHelper.Factory.createAndroidCanvasHelper( IAndroidCanvasHelper.MODE.MODE_ASYNC);

    public CameraPreviewTextureView(Context context) {
        super(context);
    }

    public CameraPreviewTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        super.onSurfaceChanged(width, height);
        androidCanvasHelper.init(width, height);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        super.onSurfaceTextureAvailable(surface, width, height);
        if (mSharedEglContext == null) {
            setSharedEglContext(EglContextWrapper.EGL_NO_CONTEXT_WRAPPER);
        }
    }

    // 2  个纹理      画面两次
    @Override
    protected int getInitialTexCount() {
        return 2;
    }

//  producedTextures 的长度就是 getInitialTexCount() 2
    @Override
    protected void onGLDraw(ICanvasGL canvas, List<GLTexture> producedTextures, List<GLTexture> consumedTextures) {
         Log.d("david", "--------->onGLDraw ");
    //        producedTextures.get(0).getRawTexture().getTarget();
    //     绘制摄像头
         GLTexture texture = producedTextures.get(0);
    //   绘制赛事, 视频源有多个 (1)文件  (2)直播中的数据流
    //   mediaTexture 视频 画面    有1  没有2
         GLTexture mediaTexture = producedTextures.get(1);
         drawVideoFrame(canvas, texture,  mediaTexture);
    }

    private void drawVideoFrame(ICanvasGL canvas, GLTexture texture, GLTexture mediaTexture) {
//       宽 高
        int width = texture.getRawTexture().getWidth();
        int height = texture.getRawTexture().getHeight();
//    bo播放
//      拿到  视频帧  这个视频 帧 就是  mediaPlayer  渲染到了  Surface   ---》 mediaSurfaceTexture一定是有画面的
        SurfaceTexture mediaSurfaceTexture = mediaTexture.getSurfaceTexture();
        mediaTexture.getRawTexture().setIsFlippedVertically(true);
//视频流渲染上去了 requerend
        canvas.drawSurfaceTexture(mediaTexture.getRawTexture(), mediaSurfaceTexture, 0, 0, width, height);
//      摄像头也写下
        TextureFilter textureFilterLT = new BasicTextureFilter();
//      在 left = width *2/3, top = height *2/3 即 右下角 处显示摄像头画面
        canvas.drawSurfaceTexture(texture.getRawTexture(), texture.getSurfaceTexture(),
                width *2/3, height *2/3, width, height, textureFilterLT);
    }
}
