package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 分页查询订单数据
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 统计订单数据
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 根据id查询订单详情
     * @param id
     */
    Orders getById(Long id);

    /**
     * 商家接单
     * @param
     */
    void confirm(Orders orders);

    /**
     * 拒单,只有接单状态才可拒单
     */
    void rejection(Orders orders);

    /**
     * 取消订单
     */
    void cancel(Orders orders);

    /**
     * 派送订单
     * @param orders
     */
    void delivery(Orders orders);

    /**
     * 完成订单
     * @param orders
     */
    void complete(Orders orders);

    /**
     * 根据订单状态和下单时间查询超时订单
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> processTimeoutOrder(Integer status, LocalDateTime orderTime);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 查询当前用户最新的待付款订单
     * @param userId 当前登录用户ID
     * @return 待付款订单对象
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status = 1 ORDER BY order_time DESC LIMIT 1")
    Orders getLatestPendingPaymentOrder(Long userId);

    /**
     * 查询指定时间区间的营业额数据
     * @return
     */
    Double sumByMap(Map map);

    /**
     * 查询指定时间区间内订单数量
     * @return
     */
    Integer countByMap(Map map);

    /**
     * 查询销量排名top10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
