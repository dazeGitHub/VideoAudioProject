package com.maniu.webrtcmaniua;

import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class YuvUtils {

    //将摄像头的图像顺时针旋转, 即将 width 和 height 进行交换
    //参考 周五资料(1).pdf
    public static void portraitData2Raw(byte[] data, byte[] output,int width,int height) {
        int y_len = width * height;
        int uvHeight = height >> 1; // uv数据高为y数据高的一半
        int k = 0;
        //j 是列数, i 是行数, 该 for 循环的意思是 从横着的左下角到第一行依次便利(从下往上), 再从左往右
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                output[k++] = data[width * i + j];
            }
        }
        //uv 数据也要旋转
        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                output[k++] = data[y_len + width * i + j];
                output[k++] = data[y_len + width * i + j + 1];
            }
        }
    }

    static  byte[] nv12;
    public static byte[]  nv21toNV12(byte[] nv21) {
//        nv21   0----nv21.size
        int  size = nv21.length;
        nv12 = new byte[size];

        // 因为 y : u : v = 4 : 1 : 1, 所以 y 的长度 len = 1 / (1 + 1/4 + 1/4) = 1 / (3/2) = 2 / 3
        int len = size * 2 / 3;
        //先把 nv21 中的 y 复制给 nv12
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while(i < size - 1){
            //就是将奇偶换位置
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    public  static  void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + "/codec.h264", true);
            writer.write(array);
            writer.write('\n');


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public  static String writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        Log.i("david", "writeContent: " + sb.toString());
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(Environment.getExternalStorageDirectory() + "/codecH264.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }


}
