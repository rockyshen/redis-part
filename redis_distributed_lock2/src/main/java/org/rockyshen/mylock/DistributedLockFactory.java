package org.rockyshen.mylock;

import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;

/**
 * 利用工厂模式，生存分布式锁
 * 你提供给我什么类型，我就生产什么类型的分布式锁
 * 提供redis，我生产redisDistributedLock
 * 提供mysql，我生产MysqlDistributedLock
 * 提供zookeeper，我生产ZookeeperDistributedLock
 */

@Component
public class DistributedLockFactory {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String lockName;

    private String uuid;

    // V7.2 一个线程，保持唯一uuid
    public DistributedLockFactory() {
        this.uuid = IdUtil.simpleUUID();
    }

    public Lock getDistributedLock(String lockType){
        if (lockType == null){
            return null;
        }

        if(lockType.equalsIgnoreCase("REDIS")){
            this.lockName = "shenRedisDistributedLock";
            return new RedisDistributedLock(stringRedisTemplate,lockName,uuid);
        } else if (lockType.equalsIgnoreCase("ZOOKEEPER")) {
            this.lockName = "shenZookeeperLockNode";
            //TODO zookerper版本的分布式锁
            return null;
        } else if (lockType.equalsIgnoreCase("MYSQL")) {
            this.lockName = "shenMysqlDistributedLock";
            //TODO MySQL版本的分布式锁
            return null;
        }
        return null;
    }

}
