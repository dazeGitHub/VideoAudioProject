package com.maniu.test;

public class MyClass {
   static boolean flag = false;
    public static void main(String[] args) throws InterruptedException {

            new Thread() {
                @Override
                public void run() {
                    System.out.println("Thread1--start");
                    while (!flag) {
                    }
                    System.out.println("Thread1--end");
                }
            }.start();
            Thread.sleep(100);
            new Thread() {
                @Override
                public void run() {
                    System.out.println("Thread2--start");
                    flag = true;
                    System.out.println("Thread2--end");
                }
            }.start();
        }
}