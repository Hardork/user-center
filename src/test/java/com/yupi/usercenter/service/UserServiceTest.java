package com.yupi.usercenter.service;

import com.yupi.usercenter.model.domain.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author:HWQ
 * @DateTime:2023/4/26 21:03
 * @Description:
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    public void testSearchUserByTags(){
        List<String> strings = Arrays.asList("java", "python");
        List<User> users = userService.searchUserByTags(strings);
        Assert.assertNotNull(users);
    }
}
