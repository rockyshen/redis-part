package org.rockyshen.service;

import lombok.extern.slf4j.Slf4j;
import org.rockyshen.mylock.DistributedLockFactory;
import org.rockyshen.mylock.RedisDistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class InventoryService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private String port;

    @Autowired
    private DistributedLockFactory distributedLockFactory;

//    Lock redisDistributedLock = new RedisDistributedLock(stringRedisTemplate,"shenRedisLock");

    public String sale() {
        String retMessage = "";

//        redisDistributedLock.lock();
        // V7.1 不用new，改为工厂模式返回的获取
        // V7.2 uuid交给工厂来生成，然后传入实例化对象中
        Lock redisDistributedLock = distributedLockFactory.getDistributedLock("redis");
        redisDistributedLock.lock();
        try {
            //1 查询库存信息
            String result = stringRedisTemplate.opsForValue().get("inventory001");
            //2 判断库存是否足够
            Integer inventoryNumber = result == null ? 0 : Integer.parseInt(result);
            //3 扣减库存
            if(inventoryNumber > 0) {
                stringRedisTemplate.opsForValue().set("inventory001",String.valueOf(--inventoryNumber));
                retMessage = "成功卖出一个商品，库存剩余: "+inventoryNumber;
                System.out.println(retMessage);
                // 模拟业务暂停120秒
                try {
                    TimeUnit.SECONDS.sleep(120);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }

                //测试redis分布式锁的可重入性
//                testReEntry();
            }else{
                retMessage = "商品卖完了，o(╥﹏╥)o";
            }
        }finally {
            redisDistributedLock.unlock();
        }
        return retMessage+"\t"+"服务端口号："+port;
    }

    // 测试可重入锁，是否成功！
//    private void testReEntry() {
//        Lock redisDistributedLock = distributedLockFactory.getDistributedLock("redis");
//        //有Bug,同一个线程，两次生产lock，UUID随机数不同了，错误！应该同一个线程的UUID始终相同
//        redisDistributedLock.lock();
//        try {
//            System.out.println("========可重入锁测试===========");
//        }finally {
//            redisDistributedLock.unlock();
//        }
//    }
}
