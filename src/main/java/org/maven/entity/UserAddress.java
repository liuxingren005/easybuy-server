package org.maven.entity;

import lombok.Data;

import java.util.Date;

/**
 * 用户地址实体类
 */
@Data
public class UserAddress {

    /**
     * 主键id
     */
    private Integer id;

    /**
     * 用户主键
     */
    private Integer userId;

    /**
     * 地址
     */
    private String address;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否是默认地址(1:是 0:否)
     */
    private Integer isDefault;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否删除(1:删除 0:未删除)
     */
    private Integer isDelete;
}
