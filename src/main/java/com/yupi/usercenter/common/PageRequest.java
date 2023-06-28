package com.yupi.usercenter.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author:HWQ
 * @DateTime:2023/5/3 18:08
 * @Description:
 **/
@Data
public class PageRequest implements Serializable {


    private static final long serialVersionUID = 7132556857756071421L;

    protected int pageSize;

    protected int pageNum;
}
