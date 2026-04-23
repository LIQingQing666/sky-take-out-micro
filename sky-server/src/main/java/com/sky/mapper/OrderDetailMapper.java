package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;


@Mapper
public interface OrderDetailMapper {

    /**
     * 批量插入订单明细数据
     * @param orderDetailList
     */
    void insertBatch(List<OrderDetail> orderDetailList);

    /**
     * 根据订单id查询订单明细
     * @param id
     */
    List<OrderDetail> getByOrderId(Long id);
}
