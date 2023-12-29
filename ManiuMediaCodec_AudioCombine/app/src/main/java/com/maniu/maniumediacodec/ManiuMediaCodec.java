package com.maniu.maniumediacodec;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//自己定义一个 ManiuMediaCodec, 用来模拟 MediaCodec 调用 dsp 芯片解析 H264 码流
public class ManiuMediaCodec {
    private  String path;

    public ManiuMediaCodec(String path) {
        this.path = path;
    }

    //二进制位 的 值, 表示当前读到哪里了
    private static int nStartBit = 0;

    //读取哥伦布编码, 每次执行 nStartBit 都会 ++
    private static int Ue(byte[] pBuff)
    {
        int nZeroNum = 0;
        while (nStartBit < pBuff.length * 8)
        {
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                break;
            }
            nZeroNum++;
            nStartBit++;
        }
        nStartBit ++;

        int dwRet = 0;
        for (int i=0; i < nZeroNum; i++)
        {
            dwRet <<= 1;
            if ((pBuff[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                dwRet += 1;
            }
            nStartBit++;
        }
        return (1 << nZeroNum) - 1 + dwRet;
    }

    //对应官方文档的 u 函数, 表示从 h264 字节数组中读取 bitIndex 位, 返回十进制结果
    //每次执行 nStartBit 都会 ++
    private static int u(int bitIndex, byte[] h264)
    {
        int dwRet = 0;
        for (int i = 0; i < bitIndex; i++)
        {
            dwRet <<= 1;
            if ((h264[nStartBit / 8] & (0x80 >> (nStartBit % 8))) != 0)
            {
                dwRet += 1;
            }
            nStartBit++;
        }
        return dwRet;
    }
// MediaCodec ---->   dsp芯片     模拟dsp   怎么解析h264码流
    public void startCodec() {
        byte[] h264 = null;
        try {
            h264 = getH264Bytes(path);
        } catch ( Exception e) {
            e.printStackTrace();
        }

        int totalSize = h264.length;
        int startIndex = 0;
        while (true) {
            if (totalSize == 0 || startIndex >= totalSize) {
                break;
            }
            int nextFrameStart = findByFrame(h264, startIndex + 2 , totalSize);

            nStartBit = 4 * 8;//nStartBit 是给 u 函数使用的

            //Nalu 单元第一个字节包含 forbidden_zero_bit(禁止位), nal_ref_idc, nal_unit_type
            int forbidden_zero_bit = u(1, h264);
            if (forbidden_zero_bit != 0) {//0 代表该帧正常, 否则该帧错误
                continue;
            }
            int nal_ref_idc = u(2, h264); //优先级
//            排列
            int nal_unit_type = u(5, h264); //之前是 nalu 单元第一个字节 & 1f, 但是正宗写法是 u(5, h264)
            switch (nal_unit_type) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    break;
                case 6:
                    break;
                case 7:
                    //将 h264 编码解码为 yuv 很复杂, 这里直接解码 sps 即可
                    parseSps(h264);
                    break;
            }

        }

    }

    private void parseSps(byte[] h264) {
        // profile_idc 是编码等级, 有几种类型:   Baseline 直播   Main Extended High   High 10   High 4:2:2
        // 从左到右越来越清晰
        int profile_idc = u(8, h264); //如果 00 00 00 01 67 64 那么, profile_idc 就是 64
//            当constrained_set0_flag值为1的时候, 就说明码流应该遵循基线 profile(Baseline profile)的所有约束.constrained_set0_flag值为0时, 说明码流不一定要遵循基线profile的所有约束。
        int constraint_set0_flag = u(1, h264);//(h264[1] & 0x80)>>7;
        //            当constrained_set1_flag值为1的时候, 就说明码流应该遵循主 profile(Main profile)的所有约束.constrained_set1_flag值为0时, 说明码流不一定要遵
        int constraint_set1_flag = u(1, h264);//(h264[1] & 0x40)>>6;
        //当 constrained_set2_flag 值为1的时候, 就说明码流应该遵循扩展 profile(Extended profile)的所有约束.constrained_set2_flag值为0时, 说明码流不一定要遵循扩展profile的所有约束。
        int constraint_set2_flag = u(1, h264);//(h264[1] & 0x20)>>5;
        //  注意：当 constraint_set0_flag, constraint_set1_flag 或 constraint_set2_flag 中不只一个值为1的话, 那么码流必须满足所有相应指明的 profile 约束。
        int constraint_set3_flag = u(1, h264);//(h264[1] & 0x10)>>4;

        // 4个零位
        int reserved_zero_4bits = u(4, h264);
        // 它指的是码流对应的level级, 最大支持码流范围
        int level_idc = u(8, h264);
        // 是否是哥伦布编码  0 是 1 不是
        int seq_parameter_set_id = Ue(h264);

        //如果 profile_idc 是 Baseline 66, 77, 88 就没有颜色位深
        if (profile_idc == 100 || profile_idc == 110 || profile_idc == 122 || profile_idc == 144) {
            int chroma_format_idc = Ue(h264);       //颜色位深
            int bit_depth_luma_minus8   = Ue(h264);
            int bit_depth_chroma_minus8  = Ue(h264);
            int qpprime_y_zero_transform_bypass_flag = u(1, h264);
            int seq_scaling_matrix_present_flag      = u(1, h264);
        }

        int log2_max_frame_num_minus4 = Ue(h264);
        int pic_order_cnt_type        = Ue(h264);
        if (pic_order_cnt_type == 0) {
            int log2_max_pic_order_cnt_lsb_minus4 = Ue(h264);
        } else if(pic_order_cnt_type == 1){
            //可以读取
            //delta_pic_order_always_zero_flag  u(1)
            //offset_for_non_ref_pic            se(v)
            //offset_for_top_to_bottom_field    se(v)
            //num_ref_frames_in_pic_order_cnt_cycle     ue(v)
            //offset_for_ref_frames[i]          se(v)
        }
        int num_ref_frames                      = Ue(h264);
        int gaps_in_frame_num_value_allowed_flag = u(1,     h264);
        //帧的宽高
        int pic_width_in_mbs_minus1             = Ue(h264);
        int pic_height_in_map_units_minus1      = Ue(h264);

//      pic_width_in_mbs_minus1 和 pic_height_in_map_units_minus1 是  16的整数倍
//      而且如果视频 0*0 代表 16*16, 视频 1 * 1 代表 32 * 32
//      因为视频的 width 和 height 不可能为 0, 所以 0*0 代表最小的 16 * 16
        int width = (pic_width_in_mbs_minus1       + 1) * 16;
        int height = (pic_height_in_map_units_minus1 + 1 ) * 16;
        Log.i("David", "width: " + width + " height: " + height);
        //1840 x 1008 实际宽度是 1832, 1840 是 16 的倍数, 所以 1840 还需要减去偏移量
        //因为没法直接表示 1832 (1832 不是 16 的倍数), 只能用 1840 再通过偏移量得到 1832

        int frame_mbs_only_flag = u(1, h264);
        Log.i("David", "parseSps: frame_mbs_only_flag" + frame_mbs_only_flag); //1
        if (frame_mbs_only_flag != 0) {
            int mb_adaptive_frame_field_flag = u(1, h264);
        }
//1840
        int direct_8x8_inference_flag = u(1, h264);

//      H264 默认是使用 16 x 16 大小的区域作为一个宏块, 所以默认 width 和 height 是 16 的倍数
//      如果是 1832 x 1008, 而 1832 不是 16 的整数倍, 那么可以使用余数 1832 % 16 = 8
//      frame_cropping_flag 为 0 就代表 宽高是16的整数倍     1 代表有偏移, 宽高不是 16 的整数倍
//      存 16 的倍数是为了减少二进制位, 每个二进制都很宝贵
        int frame_cropping_flag = u(1, h264);
        if (frame_cropping_flag != 0) {
            //哪个方向余
            //如果 frame_crop_left_offset = 0, 那么 frame_crop_right_offset = 8
            int frame_crop_left_offset = Ue(h264);
            int frame_crop_right_offset = Ue(h264);
            int frame_crop_top_offset = Ue(h264);
            int frame_crop_bottom_offset = Ue(h264);

            Log.i("David", "frame_crop_left_offset: " + frame_crop_left_offset
                    + "  frame_crop_right_offset: " + frame_crop_right_offset);
            //frame_crop_left_offset: 4         frame_crop_right_offset: 0

            //宏块只能是 4  8 16, 所以 余数 = 宽度 % 16 不可能是奇数, 所以 offset 需要乘 2, 这样 offset 更能缩小空间
            width = (pic_width_in_mbs_minus1 + 1) * 16 -
                    frame_crop_left_offset * 2 - frame_crop_right_offset * 2;
            //height 和 width 的不同是, 需要乘 (2 - frame_mbs_only_flag)
            //2 - frame_mbs_only_flag 是因为有 y:u:v = 4:2:1 或 4:2:0 等, 需要区分
            height = ((2 - frame_mbs_only_flag) * (pic_height_in_map_units_minus1 + 1) * 16) -
                    (frame_crop_top_offset * 2) - (frame_crop_bottom_offset * 2);
            Log.i("David", "width: " + width + "  height: " + height); //width: 1832  height: 1008
        }
    }

    private int findByFrame( byte[] h264, int start, int totalSize) {
        int j = 0;  // Number of chars matched in pattern
        for (int i = start; i < totalSize; i++) {
            if ((h264[i] == 0x00 && h264[i + 1] == 0x00 && h264[i + 2] == 0x00
                    && h264[i + 3] == 0x01)||(h264[i] == 0x00 && h264[i + 1] == 0x00
                    && h264[i + 2] == 0x01)) {
                return i;
            }
        }
        return -1;  // Not found
    }

    public  byte[] getH264Bytes(String path) throws IOException {
        InputStream is = new DataInputStream(new FileInputStream(new File(path)));
        int len;
        int size = 1024 * 1024; //直接读取 1M 数据, 反正包含 sps 和 pps
        byte[] buf;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        buf = new byte[size];
        len=is.read(buf, 0, size);
        bos.write(buf, 0, len);
        buf = bos.toByteArray();
        bos.close();
        return buf;
    }
}
