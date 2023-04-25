-- auto-generated definition
create table user
(
    id           bigint auto_increment
        primary key,
    username     varchar(256)                       null,
    userAccount  varchar(256)                       null comment '账号',
    avatarUrl    varchar(1024)                      null comment '头像',
    gender       tinyint                            null comment '性别',
    userPassword varchar(512)                       not null comment '密码',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    userStatus   int      default 0                 not null comment '状态 0正常 ',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除 0正常 1删除',
    userRole     int      default 0                 not null comment '用户角色 0普通用户 1管理员 2VIP用户',
    planetCode   varchar(512)                       not null comment '星球编号'
)
    comment '用户表';

