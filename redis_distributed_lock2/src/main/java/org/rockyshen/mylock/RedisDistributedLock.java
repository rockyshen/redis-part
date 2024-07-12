package org.rockyshen.mylock;

import cn.hutool.core.util.IdUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * V7，基于HSET实现自研redis分布式锁
 */

//@Component  不需要加入容器，因为由工厂提供，工厂类在容器中即可！
public class RedisDistributedLock implements Lock {
    private StringRedisTemplate stringRedisTemplate;

    private String lockName;  //KEY[1]

    private String uuidValue;  //ARGV[1]

    private long expireTime;  //ARGV[2]


//    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName) {
//        this.stringRedisTemplate = stringRedisTemplate;
//        this.lockName = lockName;
//        this.uuidValue = IdUtil.simpleUUID()+":"+Thread.currentThread().getId();
//        this.expireTime = 50L;
//    }

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName, String uuid) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        this.uuidValue = uuid +":"+Thread.currentThread().getId();
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

    /**
     * 实际干活的，lock和tryLock()复用，全盘通用
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if(time == -1L){
            String luaScript = "if redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1 then redis.call('hincrby',KEYS[1],ARGV[1],1) redis.call('expire',KEYS[1],ARGV[2]) return 1 else return 0 end";
            System.out.println("lockName:"+lockName+"\t"+"uuidValue:"+uuidValue);

            while(!stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript, Boolean.class), Arrays.asList(lockName), uuidValue, String.valueOf(expireTime))){
                // 60毫秒后重试
                TimeUnit.MILLISECONDS.sleep(60);
            }
            // 一旦枷锁成成，就开启重试机制，业务逻辑超过分布式锁过期时间，自动续期
            renewExpire();
            return true;
        }
        //几乎走不到这里
        return false;
    }

    @Override
    public void unlock() {
        // 测试可重入性，解锁时的状态
        System.out.println("unlock(): lockName:"+ lockName+"\t"+"uuidValue:"+uuidValue);

        String luaScript = "if redis.call('HEXISTS',KEYS[1],ARGV[1]) == 0 then return nil elseif redis.call('HINCRBY',KEYS[1],ARGV[1],-1) == 0 then return redis.call('del',KEYS[1]) else return 0 end";
        //nil=false  1=true  0=false
        Long flag = stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript,Long.class), Arrays.asList(lockName),uuidValue,String.valueOf(expireTime));
        if(flag == null){
            throw new RuntimeException("This lock does not exists");
        }
    }

    private void renewExpire() {
        /**
         * 这段lua脚本的意思是：如果redis分布式锁的键孩子啊，就给它续期，返回true
         * 如果不存在了，证明这个锁已经释放了，返回false
         */
        String luaScript = "if redis.call('HEXISTS',KEYS[1],ARGV[1]) == 1 then return redis.call('EXPIRE',KEYS[1],ARGV[2]) else return 0 end";
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript,Boolean.class),Arrays.asList(lockName),uuidValue,String.valueOf(expireTime))){
                    renewExpire();
                }
            }
        },(this.expireTime * 1000)/5);   //每隔10秒，触发一次定时任务
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

