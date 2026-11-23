package com.sherlock.framework.example.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper extends BaseMapper<OrderRow> {

    @Insert("""
            insert into orders (business_key, creator_id, department_id, name, created_at)
            values (#{businessKey}, #{creatorId}, #{departmentId}, #{name}, current_timestamp)
            """)
    int insertOrder(
            @Param("businessKey") String businessKey,
            @Param("creatorId") long creatorId,
            @Param("departmentId") long departmentId,
            @Param("name") String name);

    @Select("""
            select id, tenant_id as tenantId, business_key as businessKey, creator_id as creatorId,
                   department_id as departmentId, name, created_at as createdAt
            from orders
            where business_key = #{businessKey}
            """)
    OrderRow selectByBusinessKey(@Param("businessKey") String businessKey);
}
