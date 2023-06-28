package com.yupi.usercenter.model.domain.request;

import lombok.Data;

/**
 * @Author:HWQ
 * @DateTime:2023/6/28 0:42
 * @Description:
 **/
@Data
public class ArticleAddRequest {
    /**
     * 文章作者
     */
    private String owner;

    /**
     * 文章介绍
     */
    private String description;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章链接
     */
    private String href;
}
