package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        log.info("营业额数据统计：{}到{}", begin, end);
        //当前集合用于存放从begin到end的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            //日期计算，获得指定日期的后一天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天营业额数据的集合
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //获取日期对应的营业额数据,营业额是指状态是“已完成”的订单
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            //select sum(amount) from orders where status = 5 and order_time > ? and order_time < ?
            //学习一种新方法：Map
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }
        //封装返回结果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //存放日期的集合
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天用户数量的集合
        //select count(id) from user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();

        //存放每天新增用户的集合
        //select count(id) from user where create_time > ? and create_time < ?
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap<>();
            map.put("end", endTime);
            totalUserList.add(userMapper.countByMap(map));
            map.put("begin", beginTime);
            newUserList.add(userMapper.countByMap(map));
        }
        //封装返回结果
        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //存放每天订单数量
        List<Integer> orderCountList = new ArrayList<>();
        //存放每天有效订单数量
        List<Integer> validOrderCountList = new ArrayList<>();

        /* 另一种方式使用stream流*/
//        Integer totalOrderCount = 0;
//        Integer validOrderCount = 0;
//        Double orderCompletionRate = 0.0;
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            //查询每天的订单数量
            Integer orderNumber = orderMapper.countByMap(map);
            orderNumber = orderNumber == null ? 0 : orderNumber;
            orderCountList.add(orderNumber);
            //totalOrderCount += orderNumber;另一种方式使用stream流
            //查询每天有效订单数量
            map.put("status", Orders.COMPLETED);
            Integer validOrderNumber = orderMapper.countByMap(map);
            validOrderNumber = validOrderNumber == null ? 0 : validOrderNumber;
            validOrderCountList.add(validOrderNumber);
            //validOrderCount += validOrderNumber;另一种方式使用stream流
        }
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount * 100 ;
            orderCompletionRate = Math.round(orderCompletionRate * 100) / 100.0;
        }
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        //方式一：通过遍历
//        List<String> names = new ArrayList<>();
//        List<Integer> numbers = new ArrayList<>();
//        for (GoodsSalesDTO dto : salesTop10) {
//            names.add(dto.getName());
//            numbers.add(dto.getNumber());
//        }

        //方式二：使用stream流:学习一下
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        //2.通过POI将数据写入到Excel文件中
        //2.1获取一个输入流，this.getClass().getClassLoader().getResourceAsStream()指向src/main/resources这个文件夹
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //3.基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //4.填充数据
            //4.1 概览数据
            //获取sheet1
            XSSFSheet sheet1 = excel.getSheet("sheet1");
            //填充数据 -- 时间
            sheet1.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);

            //填充数据 -- 营业额
            sheet1.getRow(3).getCell(2).setCellValue(businessData.getTurnover());

            //填充数据 -- 订单完成率
            sheet1.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());

            //填充数据 -- 新增用户数
            sheet1.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());

            //填充数据 -- 订单总数
            sheet1.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());

            //填充数据 -- 平均客单价
            sheet1.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            //4.2 明细数据
            for (int i = 0; i < 30; i++){
                LocalDate date = begin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData1 = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                //填充数据
                sheet1.getRow(7 + i).getCell(1).setCellValue(date.toString());
                sheet1.getRow(7 + i).getCell(2).setCellValue(businessData1.getTurnover());
                sheet1.getRow(7 + i).getCell(3).setCellValue(businessData1.getValidOrderCount());
                sheet1.getRow(7 + i).getCell(4).setCellValue(businessData1.getOrderCompletionRate());
                sheet1.getRow(7 + i).getCell(5).setCellValue(businessData1.getUnitPrice());
                sheet1.getRow(7 + i).getCell(6).setCellValue(businessData1.getNewUsers());
            }

            //5.通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //6.关闭流
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}
