package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 订单搜索
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 统计订单数据
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 根据id查询订单详情
     * @param id
     * @return
     */
    OrderVO detailById(Long id);

    /**
     * 商家接单
     * @param
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    void adminCancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * 订单派送
     * @param id
     */
    void delivery(Long id);

    /**
     * 订单完成
     * @param id
     */
    void complete(Long id);

    /**
     * 用户历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 用户取消订单
     * @param id
     */
    void userCancel(Long id);

    /**
     * 再来一单
     * @param id
     */
    void repetition(Long id);

    /**
     * 根据订单id查询订单信息
     * @param orderId
     * @return
     */
    Orders getByOrderId(Long orderId);

    /**
     * 客户催单
     * @param id
     */
    void reminder(Long id);
}
