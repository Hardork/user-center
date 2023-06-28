package com.yupi.usercenter.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author:HWQ
 * @DateTime:2023/5/3 13:40
 * @Description:
 **/
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class  RedissonConfig {

    private String host;
    private String port;
    @Bean
    public RedissonClient RedissonClient(){
        // 1.创建配置
        Config config = new Config();
        String Address = String.format("redis://%s:%s",host,port);
        config.useSingleServer().setAddress(Address).setDatabase(3);
        //2. 返回实例
        return Redisson.create(config);
    }
}
