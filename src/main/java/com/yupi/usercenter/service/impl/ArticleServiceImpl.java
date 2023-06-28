package com.yupi.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.usercenter.common.ErrorCode;
import com.yupi.usercenter.exception.BusinessException;
import com.yupi.usercenter.model.domain.Article;
import com.yupi.usercenter.model.domain.Project;
import com.yupi.usercenter.model.domain.User;
import com.yupi.usercenter.model.domain.request.ArticleAddRequest;
import com.yupi.usercenter.service.ArticleService;
import com.yupi.usercenter.mapper.ArticleMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
* @author HWQ
* @description 针对表【article(用户文章表)】的数据库操作Service实现
* @createDate 2023-06-27 19:02:19
*/
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>
    implements ArticleService{

    @Override
    public List<Article> getUserArticleList(User loginUser) {
        Long userId = loginUser.getId();
        if(userId == null || userId <= 0){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        QueryWrapper<Article> QueryWrapper = new QueryWrapper<>();
        QueryWrapper.eq("userId",userId);
        List<Article> res = this.list(QueryWrapper);
        if(res.size() == 0){
            res = new ArrayList<>();
        }
        return res;
    }

    @Override
    public boolean addUserProject(ArticleAddRequest articleAddRequest, User loginUser) {
        Long userId = loginUser.getId();
        if(userId == null || userId <= 0){
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        //校验参数
        String title = articleAddRequest.getTitle();
        String description = articleAddRequest.getDescription();
        String href = articleAddRequest.getHref();
        String owner = articleAddRequest.getOwner();
        if(StringUtils.isAnyBlank(title,description,owner,href)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //添加数据
        Article article = new Article();
        article.setUserId(userId);
        article.setOwner(owner);
        article.setDescription(description);
        article.setTitle(title);
        article.setHref(href);
        boolean save = this.save(article);
        return save;
    }
}




