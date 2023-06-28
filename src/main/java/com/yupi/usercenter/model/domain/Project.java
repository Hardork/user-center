package com.yupi.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户项目表
 * @TableName project
 */
@TableName(value ="project")
@Data
public class Project implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建项目信息卡的用户
     */
    private Long userId;

    /**
     * 项目封面
     */
    private String cover;

    /**
     * 项目标题
     */
    private String title;

    /**
     * 项目介绍
     */
    private String description;

    /**
     * 项目进度（0-999）
     */
    private Integer percent;

    /**
     * 项目跳转链接
     */
    private String href;

    /**
     * 项目创建时间
     */
    private Date createTime;

    /**
     * 项目更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}