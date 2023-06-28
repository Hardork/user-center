package com.yupi.usercenter.service;

import com.yupi.usercenter.model.domain.Article;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.ArticleAddRequest;

import java.util.List;

/**
* @author HWQ
* @description 针对表【article(用户文章表)】的数据库操作Service
* @createDate 2023-06-27 19:02:19
*/
public interface ArticleService extends IService<Article> {

    /**
     * 获取用户文章列表
     * @param loginUser
     * @return
     */
    List<Article> getUserArticleList(User loginUser);

    /**
     * 添加文章
     * @param articleAddRequest
     * @param loginUser
     * @return
     */
    boolean addUserProject(ArticleAddRequest articleAddRequest, User loginUser);
}
