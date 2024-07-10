package org.rockyshen.service;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class InventoryService
{
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value("${server.port}")
    private String port;
    private Lock lock = new ReentrantLock();

    public String sale()
    {
        String retMessage = "";

        /**
         * 加锁！
         */
        String key = "shenRedisLock";
        String uuidValue = IdUtil.simpleUUID()+":"+Thread.currentThread().getId();
        // 1.用自旋替代递归；2.用while替代if。
        // 抢不到的，while拉回去，自己重试，直到拿到为止
        while(!stringRedisTemplate.opsForValue().setIfAbsent(key, uuidValue, 30L, TimeUnit.SECONDS)) {
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 业务逻辑
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
            }else{
                retMessage = "商品卖完了，o(╥﹏╥)o";
            }
        }finally {
            // 释放锁，必须检查是自己的锁，只能删自己的
//            if(stringRedisTemplate.opsForValue().get(key).equalsIgnoreCase(uuidValue)){
//                stringRedisTemplate.delete(key);
//           }

            // 修改为lua脚本，原子性删除

            /**
             * 释放锁！
             * 利用lua脚本
             */
            String luaScript =
                    "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript, Boolean.class), Arrays.asList(key),uuidValue);
            }

        return retMessage+"\t"+"服务端口号："+port;
    }
}
