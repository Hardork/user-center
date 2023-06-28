package com.yupi.usercenter.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @Author:HWQ
 * @DateTime:2023/5/3 13:49
 * @Description:
 **/
@SpringBootTest
@RunWith(SpringRunner.class)

public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    public void test(){
        RList<String> list = redissonClient.getList("test-list");
        list.add("hello");
    }
}
