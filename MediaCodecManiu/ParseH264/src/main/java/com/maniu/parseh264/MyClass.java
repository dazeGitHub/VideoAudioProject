package com.maniu.parseh264;

public class MyClass {

    public static void main(String[] args) {
//      5 哥伦布编码位 00101 正好凑满一个字节位 000 00101,
//      目的: 将 5 哥伦布编码后的二进制位是 00101 还原为原数据 4
        byte data = 5 & 0xFF; //得到一个字节 假设上一个值 读取成功   000   00101
        //mStartBit 表示从第几位开始(即对准 data 哪一位), 例如 5 哥伦布编码为 00101, 8 - 哥伦布编码长度5 = 3, 所以 mStartBit = 3
        //因为前面都是无效数据, 所以 mStartBit 的起始长度 = 8 - 哥伦布编码长度
        int mStartBit = 3;    //不能从第1个0开始读, 否则 1 后面的数据长度就是 5 位数     //还需要记录 1 从第几位开始
        int mZeroNum = 0;     //前面出现 0 的个数

        //2 的哥伦布编码 010
        //byte data = 2 & 0xFF;
        //int mStartBit = 5;  //mStartBit = 8 - 哥伦布编码长度3 = 5
        //int mZeroNum = 0;

        //在 java 中只能操作字节, 不能一个 bit 一个 bit 的读取    一个字节 8位
        while (mStartBit < 8) {
            //data 二进制位 000 00101
            //0x80 二进制位 100 00000
            //每次 0x80 向右边移动 mStartBit 位, 什么时候 data & (0x80 >> (mStartBit)) 不为 0 说明
            //找到了 data 的第一个 1, 此时  mStartBit++ 得到的就是第一个 0 的位置
            if ((data & (0x80 >> (mStartBit))) != 0) {
                break;
            }
            mZeroNum ++;
            mStartBit ++;
        }
//1 -1  =0  1232   542 35 43  12321321     二进制 0 0000001
        mStartBit++; //是为了让 0x80 >> (mStartBit) 的 1 对准 data 中 1 后边的那一位
//        先统计  出现0 的个数
        System.out.println("mZeroNum :  " + mZeroNum);

//        左 右 1
//        右  左 2
//         往后面读取相应的多少位  还原对应  把知还原 001   01    n位
//        分两部分还原  哥伦布编码的最高位一定是 1,    00个数       后面尾数  1<<2  =4  +1=5

//1<<2  1*2*2 =4+1  =5 -1  -=4
        int dwRet = 0;  //这个表示 data 数据 000 00101 中间那个 1 后边的数据 (例如 01) 的十进制格式
        //从左到右读
        for (int i = 0; i < mZeroNum; i++) {
            dwRet <<= 1; //左移相当于乘 2, 将之前的值左移, 那么下边 dwRet 才能加 1
            //          data     二进制位 000 00101
            //          0x80     二进制位 100 00000
            //0x80 >> (mStartBit % 8) = 000 00010   首次进入循环
            //因为二进制是连续的, 跨字节, 所以 mStartBit % 8
            if ((data & (0x80 >> (mStartBit % 8)))!= 0) { //这里针对的是 data 是 000 00110 的情况
                dwRet += 1; //此时 mStartBit 对准 1 后边那个 1,
            }
            mStartBit++;
        }
        //最高位1 左移 mZeroNum 是为了将二进制换算成十进制, 然后再加 1 后面的数据 dwRet, 还原为原数据再减 1
        int value = (1 << mZeroNum) + dwRet - 1;
        System.out.println("value:  " + value); //value:  4
    }
}