package com.maniu.androidmutilvideo.encoder;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

//  File    input      mediacodec     编码好的
public class MediaCodecInputStream extends InputStream {

    private MediaCodec mMediaCodec = null;
    private ByteBuffer mBuffer = null;
    public MediaFormat mMediaFormat;
    private boolean mClosed = false;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private MediaFormatCallback mediaFormatCallback;

    @Override
    public int read() throws IOException {
        return 0;
    }

    public MediaCodecInputStream(MediaCodec mediaCodec, MediaFormatCallback mediaFormatCallback) {
        mMediaCodec = mediaCodec;
        this.mediaFormatCallback = mediaFormatCallback;
    }


    @SuppressLint("WrongConstant")
    @Override
    public int read(byte buffer[], int offset, int length) throws IOException {
        int readLength;
        int outIndex = -1;
//        android   opengl  最难
        if (mBuffer == null) {
            while (!Thread.interrupted() && !mClosed) {
                synchronized (mMediaCodec) {


                    if (mClosed) return 0;
                    outIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 110000);
                    if (outIndex >= 0) {
                        mBuffer = mMediaCodec.getOutputBuffer(outIndex);
                        mBuffer.position(0);
                        break;

                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mMediaFormat = mMediaCodec.getOutputFormat();
//                    宽高  配置发生了变化
                        if (mediaFormatCallback != null) {
                            mediaFormatCallback.onChangeMediaFormat(mMediaFormat);
                        }

                    } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        return 0;
                    } else {
                        return 0;
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        return 0;
                    }
                }
            }
        }

        if (mClosed) throw new IOException("This InputStream was closed");

        readLength = length < mBufferInfo.size - mBuffer.position() ? length : mBufferInfo.size - mBuffer.position();

        mBuffer.get(buffer, offset, readLength);

        if (mBuffer.position() >= mBufferInfo.size) {
            mMediaCodec.releaseOutputBuffer(outIndex, false);
            mBuffer = null;
        }
//读的长度 mBuffer --》 byte数组      送给应用层
        return readLength;
    }

    public interface MediaFormatCallback {
        void onChangeMediaFormat(MediaFormat mediaFormat);
    }

    @Override
    public int available() throws IOException {
        if (mBuffer != null)
            return mBufferInfo.size - mBuffer.position();
        else
            return 0;
    }

    @Override
    public void close() {
        mClosed = true;
    }

    public MediaCodec.BufferInfo getLastBufferInfo() {
        return mBufferInfo;
    }


    public interface OnReadAllCallback {
        void onReadOnce(byte[] buffer, int readSize, MediaCodec.BufferInfo mediaBufferSize);
    }


//    对外提供一个静态方法

    public static void readAll11(MediaCodecInputStream is, byte[] buffer, @NonNull OnReadAllCallback onReadAllCallback) {
        byte[] readBuf = buffer;
        int readSize = 0;
        do {
            try {
                readSize = is.read(readBuf, 0, readBuf.length);
                onReadAllCallback.onReadOnce(readBuf, readSize, copyBufferInfo(is.mBufferInfo));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } while (readSize > 0);
    }

    private static MediaCodec.BufferInfo copyBufferInfo(MediaCodec.BufferInfo lastBufferInfo) {
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        bufferInfo.presentationTimeUs = lastBufferInfo.presentationTimeUs;
        bufferInfo.flags = lastBufferInfo.flags;
        bufferInfo.offset = lastBufferInfo.offset;
        bufferInfo.size = lastBufferInfo.size;
        return bufferInfo;
    }

    public void readAll(byte[] buffer,
                        @NonNull OnReadAllCallback onReadAllCallback) {
        byte[] readBuf = buffer;
        int readSize = 0;
        do {
            try {
                readSize = read(readBuf, 0, readBuf.length);
                onReadAllCallback.onReadOnce(readBuf, readSize, copyBufferInfo(mBufferInfo));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } while (readSize > 0);
    }
}
