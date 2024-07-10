package org.rockyshen.mylock;

import cn.hutool.core.util.IdUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * V7，基于HSET实现自研redis分布式锁
 */
public class RedisDistributedLock implements Lock {

    private StringRedisTemplate stringRedisTemplate;

    private String lockName;  //KEY[1]

    private String uuidValue;  //ARGV[1]

    private long expireTime;  //ARGV[2]

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        this.uuidValue = IdUtil.simpleUUID()+":"+Thread.currentThread().getId();
        this.expireTime = 50L;
    }

    @Override
    public void lock() {
        tryLock();    // 实际调用tryLock
    }

    @Override
    public boolean tryLock() {
        try {
            tryLock(-1L,TimeUnit.SECONDS);   // 实际调用带参数的tryLock
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if(time == -1L){
            String luaScript = "if redis.call('exists', KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1 then"+
                                "redis.call('hincrby', KEYS[1],ARGV[1],1) redis.call('expire', KEYS[1],ARGV[2]) return 1"+
                                "else return 0; end";
            while(!stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript,Boolean.class), Arrays.asList(lockName),uuidValue,String.valueOf(expireTime))){
                // 60毫秒后重试
                TimeUnit.MILLISECONDS.sleep(60);
            }
            return true;
        }
        return false;
    }
    @Override
    public void unlock() {
        String luaScript = "if redis.call('hexists',KEY[1],ARGV[1]) == 0 then" +
                "return nil" +
                "elseif redis.call('hincrby',KEY[1],ARGV[1],-1) == 0 then" +
                "return redis.call('del',KEY[1])" +
                "else" +
                "return 0" +
                "end";
        //nil=false  1=true  0=false
        Long flag = stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript,Long.class), Arrays.asList(lockName),uuidValue,String.valueOf(expireTime));
        if(null == flag){
            throw new RuntimeException("This lock does not exists");
        }
    }

    //  下面两个暂时用不到，不再重写
    //  空着就行
    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
    @Override
    public Condition newCondition() {
        return null;
    }
}

