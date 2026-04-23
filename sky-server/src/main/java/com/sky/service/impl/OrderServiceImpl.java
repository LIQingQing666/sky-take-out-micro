package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.WebSocket.WebSocketServer;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;
    /**
     * 用户下单:感觉很复杂，怎么实现？
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        // 1. 基础变量定义
        Long userId = BaseContext.getCurrentId();
        Orders order = new Orders();
        AddressBook usedAddress = null; // 最终使用的地址（前端传的/默认地址）

        // 2. 地址校验&获取最终使用的地址（核心修复：统一管理地址变量）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook != null) {
            // 前端传了有效地址ID，用前端的地址
            usedAddress = addressBook;
        } else {
            // 前端没传地址，查默认地址
            AddressBook defaultAddress = addressBookMapper.getDefaultAddress(userId, 1);
            if (defaultAddress == null) {
                throw new AddressBookBusinessException("地址块为空，不能下单");
            }
            usedAddress = defaultAddress;
            // 把默认地址ID回填到DTO，保证拷贝后order的addressBookId正确
            ordersSubmitDTO.setAddressBookId(defaultAddress.getId());
        }

        // 3. 购物车校验
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list == null || list.isEmpty()) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 4. 补全DTO默认值（修复payMethod判空逻辑）
        if (ordersSubmitDTO.getPackAmount() == null) {
            ordersSubmitDTO.setPackAmount(4); // 打包费默认4
        }
        if (ordersSubmitDTO.getTablewareNumber() == null) {
            ordersSubmitDTO.setTablewareNumber(1); // 餐具数量默认1
        }
        // 修复：payMethod是int类型，判0而不是null（0是无效值，1=微信，2=支付宝）
        if (ordersSubmitDTO.getPayMethod() <= 0) {
            ordersSubmitDTO.setPayMethod(1); // 默认为微信支付
        }

        // 5. 计算订单总金额（避免前端传错，后端兜底）
        if (ordersSubmitDTO.getAmount() == null) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (ShoppingCart cart : list) {
                // 防止cart.getAmount()为null，加空指针保护
                BigDecimal cartAmount = cart.getAmount() == null ? BigDecimal.ZERO : cart.getAmount();
                totalAmount = totalAmount.add(cartAmount.multiply(new BigDecimal(cart.getNumber())));
            }
            // 总金额 += 打包费
            totalAmount = totalAmount.add(new BigDecimal(ordersSubmitDTO.getPackAmount()));
            ordersSubmitDTO.setAmount(totalAmount);
        }

        //6. 检查前端是否将支付方式传过来，若为0，则设置为微信支付
        if (ordersSubmitDTO.getPayMethod() == 0) {
            ordersSubmitDTO.setPayMethod(1);
        }

        // 7. 拷贝DTO属性到order
        BeanUtils.copyProperties(ordersSubmitDTO, order);

        // 8. 手动赋值后端生成的字段
        order.setUserId(userId);
        order.setOrderTime(LocalDateTime.now());
        order.setPayStatus(Orders.UN_PAID);
        order.setStatus(Orders.PENDING_PAYMENT);
        // 优化：订单号=用户ID+时间戳+随机数，避免重复
        order.setNumber(userId + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000));
        // 核心修复：用统一的usedAddress赋值，避免空指针
        order.setPhone(usedAddress.getPhone());
        order.setConsignee(usedAddress.getConsignee());
        // 修复：拼接实际地址（省+市+区+详细地址）
        order.setAddress(
                usedAddress.getProvinceName() + usedAddress.getCityName() + usedAddress.getDistrictName()  + usedAddress.getDetail()
        );
        order.setDeliveryStatus(0);
        // 如果前端传了值就用前端的，没传就设默认值
        if (ordersSubmitDTO.getTablewareStatus() == null) {
            order.setTablewareStatus(1); // 设置默认值,依据餐具提供
        } else {
            order.setTablewareStatus(ordersSubmitDTO.getTablewareStatus());
        }

        // 如果tableware_number也可能为空，建议一起设置默认值
        if (ordersSubmitDTO.getTablewareNumber() == null) {
            order.setTablewareNumber(0); // 默认为0套餐具
        } else {
            order.setTablewareNumber(ordersSubmitDTO.getTablewareNumber());
        }

        // 9. 插入订单主表
        orderMapper.insert(order);

        // 10. 插入订单明细表
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 11. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 12. 封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderTime(order.getOrderTime())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
        /**
         * 这版代码是绕过微信支付接口，使用模拟数据
         */
        // 1. 获取当前登录用户ID（从ThreadLocal中）
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new OrderBusinessException("该用户没有登录");
        }
        log.info("处理用户{}的支付请求，自动查询最新待付款订单", userId);

        // 2. 查询该用户最新的待付款订单
        Orders ordersDB = orderMapper.getLatestPendingPaymentOrder(userId);
        if (ordersDB == null) {
            throw new OrderBusinessException("当前没有待付款的订单，请先下单");
        }
        String orderNumber = ordersDB.getNumber(); // 获取后端生成的订单号
        log.info("自动获取到用户{}的待付款订单号：{}", userId, orderNumber);

        // 3. 校验订单状态（双重校验，确保是待付款）
        if (!Orders.PENDING_PAYMENT.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException("当前订单非待付款状态，无法支付");
        }

        // 4. 模拟微信支付逻辑（和之前一致）
        log.info("模拟微信支付成功，订单号：{}", orderNumber);
        paySuccess(orderNumber); // 支付成功后更新订单状态

        // 5. 封装返回结果（和之前一致）
        OrderPaymentVO vo = new OrderPaymentVO();

        // 模拟微信支付参数
        vo.setNonceStr(UUID.randomUUID().toString().replace("-", ""));
        vo.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));
        vo.setSignType("MD5");
        vo.setPackageStr("prepay_id=wx202601262100001234567890");
        vo.setPaySign("模拟生成的支付签名");

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //通过websocket通知客户端 type orderId content
        Map map = new HashMap();
        map.put("type", 1);//1表示来单提醒 2表示客户催单
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号" + outTradeNo);
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 商家订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        Long total = page.getTotal();
        List<Orders> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 统计订单数据
     * @return
     */
    public OrderStatisticsVO statistics() {
        OrderStatisticsVO list = orderMapper.statistics();
        return list;
    }

    /**
     * 根据id查询订单详情
     * @param id
     * @return
     */
    public OrderVO detailById(Long id) {
        // 1. 查询订单主表信息
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 2. 查询该订单的所有明细（列表）
        List<OrderDetail> details = orderDetailMapper.getByOrderId(id);

        // 3. 构建订单菜品信息（如：鱼香肉丝x1、宫保鸡丁x2）
        String orderDishes = buildOrderDishes(details);

        // 4. 构建OrderVO（继承Orders的字段 + 明细列表 + 菜品描述）
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);

        // 5.设置自定义字段
        orderVO.setOrderDishes(orderDishes);
        orderVO.setOrderDetailList(details);
        return orderVO;
    }


    // 辅助方法：拼接订单菜品信息
    private String buildOrderDishes(List<OrderDetail> detailList) {
        if (CollectionUtils.isEmpty(detailList)) {
            return "";
        }
        // 拼接格式：菜品名x数量、菜品名x数量
        return detailList.stream()
                .map(detail -> detail.getName() + "x" + detail.getNumber())
                .collect(Collectors.joining("、"));
    }

    /**
     * 商家接单
     */
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //接单：前置条件
        //1.订单存在 2.仅待接单订单才可接单 3.订单没有被拒单以及取消
        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Integer status = orders.getStatus();
        if(!status.equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException("仅待接单订单才可接单");
        }

        if(status.equals(Orders.CANCELLED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //确定要更新的字段
        orders.setStatus(Orders.CONFIRMED);
        orders.setDeliveryTime(LocalDateTime.now().plusMinutes(30));
        orderMapper.confirm(orders);
    }

    /**
     * 拒单:搞清楚拒单与取消订单的区别
     * @param ordersRejectionDTO
     */
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //查询订单是否存在，不存在抛出异常
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //只有待接单状态的订单才能拒单
        Integer status = orders.getStatus();
        if(!status.equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException("待接单状态的订单才能拒单");
        }
        //确定要更新的字段
        String reason = ordersRejectionDTO.getRejectionReason();
        if(reason == null || reason.trim().isEmpty()){
            throw new OrderBusinessException("拒单原因不能为空");
        }
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());

        //支付状态：已支付则改为退款（2），未支付则保持0
        Integer payStatus = orders.getPayStatus();
        if(payStatus.equals(Orders.PAID)){
            orders.setPayStatus(Orders.REFUND);
            //TODO 如果支付状态是已支付，则触发退款流程-待完善
        }
        //调用Mapper接口update
        orderMapper.rejection(orders);

    }

    /**
     * 商家取消订单,在前端设计中
     * @param ordersCancelDTO
     */
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) {
        //判断是否订单存在
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //判断订单支付状态，如果已支付则改为退款中，待支付则保持0
        Integer payStatus = orders.getPayStatus();
        if(payStatus.equals(Orders.PAID)){
            orders.setPayStatus(Orders.REFUND);
            //TODO 如果已支付触发退款流程-待完善

        }
        //确定要更新的字段
        String cancelReason = ordersCancelDTO.getCancelReason();
        if(cancelReason == null || cancelReason.trim().isEmpty()){
            throw new OrderBusinessException("取消订单原因不能为空");
        }
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        //调用Mapper接口update
        orderMapper.cancel(orders);
    }

    /**
     * 商家派送订单
     * @param id
     */
    public void delivery(Long id) {
        //判断订单是否存在
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //判断订单状态，只能是待派送状态
        Integer status = orders.getStatus();
        if(!status.equals(Orders.CONFIRMED)){
            throw new OrderBusinessException("只有待派送状态才能派送");
        }
        //判断付款状态，只能是已支付
        Integer payStatus = orders.getPayStatus();
        if(!payStatus.equals(Orders.PAID)){
            throw new OrderBusinessException("已支付订单才能派送");
        }
        //确定要更新的字段
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        //调用Mapper接口update
        orderMapper.delivery(orders);
    }

    /**
     * 订单完成
     * @param id
     */
    public void complete(Long id) {
        //判断id的合法性
        if(id == null || id <= 0){
            throw new OrderBusinessException("订单号不合法");
        }

        //判断订单是否存在
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //判断订单状态，只能是派送中状态
        Integer status = orders.getStatus();
        if(status == null || !status.equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException("只有派送中订单才能完成");
        }
        //判断付款状态，只能是已支付
        Integer payStatus = orders.getPayStatus();
        if(payStatus == null || !payStatus.equals(Orders.PAID)){
            throw new OrderBusinessException("已支付订单才能完成");
        }
        //确定要更新的字段
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        //调用Mapper接口update
        orderMapper.complete(orders);
    }

    /**
     * 用户查询历史订单，这个地方分页查询与别的不同，需要查不同的表
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 1. 分页查询订单主表数据-订单表
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        Long total = page.getTotal();
        List<Orders> records = page.getResult();
        // 2. 根据订单id查询订单明细表数据-订单明细表
        if(records != null && !records.isEmpty()){
            for(Orders orders : records){
                Long id = orders.getId();
                //2.1 查询该订单的所有明细
                List<OrderDetail> details = orderDetailMapper.getByOrderId(id);
                //2.2 设置订单的订单明细
                orders.setOrderDetailList(details);
            }
        }

        return new PageResult(total, records);
    }

    /**
     * 用户取消订单
     * @param id
     */
    public void userCancel(Long id) {
        //判断id的合法性
        if(id == null || id <= 0){
            throw new OrderBusinessException("订单号不合法");
        }
        //判断是否订单存在
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //判断订单状态，只有待付款/待接单等状态能取消，已完成/已取消的不能再取消
        Integer status = orders.getStatus();
        if (status.equals(Orders.CANCELLED) || status.equals(Orders.COMPLETED)) {
            throw new OrderBusinessException("该订单已取消/已完成，无法再次取消");
        }
        //判断订单支付状态，如果已支付则改为退款中，待支付则保持0
        Integer payStatus = orders.getPayStatus();
        if(payStatus.equals(Orders.PAID)){
            orders.setPayStatus(Orders.REFUND);
            //TODO 如果已支付触发退款流程-待完善

        }
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        //调用Mapper接口update
        orderMapper.cancel(orders);
    }

    /**
     * 再来一单:
     * 校验订单 ID 合法性 + 订单是否存在；
     * 校验当前登录用户是否是该订单的所属用户（数据安全）；
     * 根据历史订单 ID，查询该订单的所有菜品明细（order_detail 表）；
     * 校验明细是否存在（避免空订单）；
     * 基于明细创建新订单，设置基础信息（用户 ID、订单状态、下单时间等）；
     * 保存新订单到 orders 表；
     * 保存新订单的菜品明细到 order_detail 表。
     * @param id
     */
    @Transactional
    public void repetition(Long id) {
        //判断id的合法性
        if(id == null || id <= 0){
            throw new OrderBusinessException("订单号不合法");
        }
        //判断是否订单存在
        Orders historyOrder = orderMapper.getById(id);
        if(historyOrder == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //判断该订单是否属于当前登录用户
        Long userId = BaseContext.getCurrentId();
        if(!historyOrder.getUserId().equals(userId)){
            throw new OrderBusinessException("无权复刻他人订单");
        }

        //获取该订单的菜品明细
        List<OrderDetail> historyOrderDetails = orderDetailMapper.getByOrderId(id);
        if(historyOrderDetails == null || historyOrderDetails.isEmpty()){
            throw new OrderBusinessException("该订单无菜品明细，无法再来一单");
        }

        //新建一个订单
        Orders newOrder = new Orders();
        // 5.1 基础信息（复用历史订单的有效信息）
        newOrder.setUserId(userId);
        newOrder.setAddressBookId(historyOrder.getAddressBookId());
        newOrder.setUserName(historyOrder.getUserName());
        newOrder.setPhone(historyOrder.getPhone());
        newOrder.setAddress(historyOrder.getAddress());
        newOrder.setConsignee(historyOrder.getConsignee());
        newOrder.setRemark(historyOrder.getRemark());
        newOrder.setPackAmount(historyOrder.getPackAmount()); // 打包费
        newOrder.setTablewareNumber(historyOrder.getTablewareNumber()); // 餐具数量
        newOrder.setTablewareStatus(historyOrder.getTablewareStatus()); // 餐具数量状态
        newOrder.setDeliveryStatus(historyOrder.getDeliveryStatus()); // 配送状态

        // 5.2 重置新订单状态（核心）
        newOrder.setStatus(Orders.PENDING_PAYMENT); // 1-待付款
        newOrder.setPayStatus(Orders.UN_PAID); // 0-未支付
        newOrder.setOrderTime(LocalDateTime.now()); // 新下单时间
        newOrder.setPayMethod(1); // 支付方式清空（用户重新选择）
        newOrder.setEstimatedDeliveryTime(null); // 预计送达时间清空
        newOrder.setDeliveryTime(null); // 送达时间清空
        // 5.3 生成唯一订单号
        newOrder.setNumber(userId + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000));

        // 5.4 计算新订单总金额（基于明细重新计算，避免历史金额错误）
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderDetail detail : historyOrderDetails) {
            totalAmount = totalAmount.add(detail.getAmount().multiply(new BigDecimal(detail.getNumber())));
        }
        // 加上打包费
        if (newOrder.getPackAmount() != null) {
            totalAmount = totalAmount.add(new BigDecimal(String.valueOf(newOrder.getPackAmount())));
        }
        newOrder.setAmount(totalAmount); // 实收金额

        // 5.5 清空无效字段（避免历史订单的取消/拒绝信息污染新订单）
        newOrder.setCancelReason(null);
        newOrder.setRejectionReason(null);
        newOrder.setCancelTime(null);
        newOrder.setCheckoutTime(null);
        // 插入订单主表
        orderMapper.insert(newOrder);

        // 7. 构建新订单明细并批量插入
        List<OrderDetail> newOrderDetails = new ArrayList<>();
        for (OrderDetail historyDetail : historyOrderDetails) {
            OrderDetail newDetail = new OrderDetail();
            newDetail.setOrderId(newOrder.getId()); // 关联新订单ID
            newDetail.setName(historyDetail.getName()); // 菜品/套餐名称
            newDetail.setDishId(historyDetail.getDishId()); // 菜品ID
            newDetail.setSetmealId(historyDetail.getSetmealId()); // 套餐ID
            newDetail.setDishFlavor(historyDetail.getDishFlavor()); // 口味
            newDetail.setNumber(historyDetail.getNumber()); // 数量
            newDetail.setAmount(historyDetail.getAmount()); // 单品金额
            newDetail.setImage(historyDetail.getImage()); // 图片
            newOrderDetails.add(newDetail);
        }
        orderDetailMapper.insertBatch(newOrderDetails);
    }

    /**
     * 根据订单 ID 查询订单
     * @param orderId
     * @return
     */
    public Orders getByOrderId(Long orderId) {
        return orderMapper.getById(orderId);
    }

    /**
     * 客户催单
     * @param id
     */
    public void reminder(Long id) {
        //根据id查询订单
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //通过WebSocket向客户端推送消息
        Map map = new HashMap();
        map.put("type", 2);//1表示来单提醒，2表示催单提醒
        map.put("orderId", id);
        map.put("content", "订单号：" + orders.getNumber());
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }


}
