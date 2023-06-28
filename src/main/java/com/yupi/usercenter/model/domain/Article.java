package com.yupi.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户文章表
 * @TableName article
 */
@TableName(value ="article")
@Data
public class Article implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 创建文章信息卡用户
     */
    private Long userId;

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

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 文章更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}