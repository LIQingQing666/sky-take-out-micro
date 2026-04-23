package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping ("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO, HttpServletResponse response) {
        log.info("用户下单：{}", ordersSubmitDTO);
        try {
            OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);
            log.info("下单成功，返回：{}", orderSubmitVO);

            // 强制刷新响应，确保输出
            response.flushBuffer();

            return Result.success(orderSubmitVO);
        } catch (Exception e) {
            log.error("下单失败", e);
            return Result.error("下单失败：" + e.getMessage());
        }
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 用户历史订单查询
     * @param
     * @param
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("用户历史订单查询")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("历史订单查询");
        PageResult pageResult = orderService.historyOrders(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 用户订单详情查询
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("用户订单详情查询")
    public Result<OrderVO> detail(@PathVariable  Long id){
        log.info("用户订单详情查询");
        OrderVO orderVO = orderService.detailById(id);
        return Result.success(orderVO);
    }

    /**
     * 用户取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result cancel(@PathVariable Long id){
        log.info("用户取消订单");
        orderService.userCancel(id);
        return Result.success();
    }

    /**
     * 用户再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("用户再来一单")
    public Result repetition(@PathVariable Long id){
        log.info("用户重复下单");
        orderService.repetition(id);
        return Result.success();
    }

    /**
     * 用户催单
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("用户催单")
    public Result reminder(@PathVariable Long id){
        orderService.reminder(id);
        return null;
    }

}
