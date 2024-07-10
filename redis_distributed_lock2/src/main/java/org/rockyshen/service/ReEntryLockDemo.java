package org.rockyshen.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁的实验，帮助我理解
 */
public class ReEntryLockDemo {
//    final Object obj = new Object();  //同步监视器，一个锁
//
//    public void entry01() {
//
//    }

    Lock lock = new ReentrantLock();

    public void entry03() {
        new Thread(() -> {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "外层调用lock");
                lock.lock();
                try{
                    System.out.println(Thread.currentThread().getName() + "内层调用lock");
                }finally {
                    lock.unlock();         // lock了，一定要unclock
                }
            }finally {
                lock.unlock();
            }
        },"t1").start();          // 启动t1线程

        try {
            TimeUnit.MILLISECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        new Thread(() -> {
            lock.lock();
            try{
                System.out.println(Thread.currentThread().getName() + "外层调用lock");
            }finally {
                lock.unlock();
            }
        },"tt2").start();

    }



    public static void main(String[] args){
        ReEntryLockDemo demo = new ReEntryLockDemo();
        demo.entry03();
    }
}