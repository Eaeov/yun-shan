package com.zyj.userservice.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.vo.*;
import com.zyj.userservice.mapper.UserMapper;
import com.zyj.userservice.service.ReportService;
import com.zyj.userservice.service.WorkSpaceService;
import com.zyj.yunapi.client.OrderClient;
import com.zyj.yunapi.client.UserClient;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.glassfish.jaxb.core.v2.TODO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author：zyj
 * @Package：com.zyj.userservice.service.impl
 * @Project：yun-shan
 * @name：ReportServiceImpl
 * @Date：18 12月 2025  16:07
 * @Filename：ReportServiceImpl
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    // private final OrderMapper orderMapper;
    private final OrderClient orderClient;
    private final UserMapper userMapper;
    private final WorkSpaceService workSpaceService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();//存放begin到end范围内得每天日期

        dateList.add(begin);

        while(!begin.equals(end)){ // 日期计算
            begin = begin.plusDays(1);// + 1天
            dateList.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            //查询date对应得营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//一天的开始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//一天的最后
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            // 调用远程服务，查询订单金额
            Double turnover = (Double) orderClient.sumByMap(map).getData();// 订单金额
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);

        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 用户统计(新老用户)
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();//存放begin到end范围内得每天日期

        dateList.add(begin);
        //begin - end 的每天
        while(!begin.equals(end)){ // 日期计算
            begin = begin.plusDays(1);// + 1天
            dateList.add(begin);
        }
        //select count(id) form user where create_time < ?
        List<Integer> totalUserList = new ArrayList<>();//每天总用户数量
        //select count(id) form user where create_time  < ? and create_time > ?
        List<Integer> newUserList = new ArrayList<>();//新用户数量


        for (LocalDate date : dateList) {
            //查询date对应得营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//一天的开始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//一天的最后
            Map map = new HashMap();

            map.put("end",endTime);
            Integer totalUser = userMapper.countByMap(map);
            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        log.info("订单统计 :{}{}",begin,end);
        List<LocalDate> dateList = new ArrayList<>();//存放begin到end范围内得每天日期

        dateList.add(begin);
        //begin - end 的每天
        while(!begin.equals(end)){ // 日期计算
            begin = begin.plusDays(1);// + 1天
            dateList.add(begin);
        }
        //select count(id) from orders where order_time < ? and order_time > ?
        List<Integer> orderCountList = new ArrayList<>();//每日订单数
        List<Integer> validOrderCountList = new ArrayList<>(); //每日有效订单数
        //Integer totalOrderCount = orderMapper.countByMap(null); //订单总数
        //Integer validOrderCount = orderMapper.countByMap((Map) new HashMap<>().put("status", Orders.COMPLETED)); //有效订单数
        Integer totalOrderCount = orderClient.countByMap(new HashMap<>()).getData(); //订单总数
        Map<String, Object> validOrderMap = new HashMap<>();
        validOrderMap.put("status", Orders.COMPLETED);
        Integer validOrderCount = (Integer) orderClient.countByMap(validOrderMap).getData(); //有效订单数
        for (LocalDate date : dateList) {
            //查询date对应得营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//一天的开始
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//一天的最后
            Map map = new HashMap();
            map.put("end",endTime);
            map.put("begin",beginTime);
            //Integer orderCount = orderMapper.countByMap(map);//每天
            Integer orderCount = (Integer) orderClient.countByMap(map).getData();
            map.put("status", Orders.COMPLETED); //完成的订单
            orderCountList.add(orderCount);
            validOrderCountList.add(orderCount); // 有效订单数
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .orderCompletionRate(validOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount)//订单完成率
                .build();
    }

    /**
     * 销量排名
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        log.info("销量排名 :{}{}",begin,end);
        //开始时间到结束时间的菜品销量排名
        //select od.name,sum(od.number) from orders o,order_detail od
        // where o.id = od.order_id and o.order_time < ? and o.order_time > ?
        // group by od.name order by number desc limit 0,10;
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        //List<GoodsSalesDTO> list = orderMapper.getSalesTop10(beginTime,endTime);
        // 格式化为标准字符串（保留时间精度）
        String beginStr = beginTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);//"yyyy-MM-dd HH:mm:ss"
        String endStr = endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<GoodsSalesDTO> list = orderClient.getSalesTop10(beginStr, endStr).getData();

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(list.stream().map(GoodsSalesDTO::getName).toArray(),","))
                .numberList(StringUtils.join(list.stream().map(GoodsSalesDTO::getNumber).toArray(),","))
                .build();
    }

    /**
     * 导出近30天的运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        // 提前将资料中的 运营数据报表模板.xlsx 拷贝到项目的resources/template目录中
        // 拿到 前30天 - 前1天 的数据
        LocalDate begin = LocalDate.now().minusDays(30);
        // 日期 转 日期加时间，转的时候要指定时间字段
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // 调用service方法来获取工作台数据（注意是service而不是mapper，因为这个功能之前实现过，直接拿来用就行）
        BusinessDataVO businessData = workSpaceService.getBusinessData(beginTime, endTime);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheetAt(0);
            // 第2行写入时间字段
            sheet.getRow(1).getCell(1).setCellValue(begin + "至" + end);
            // 第4、5行写入概览数据
            XSSFRow row4 = sheet.getRow(3);
            // 获取单元格，填入营业额、订单完成率、新增用户数量
            row4.getCell(2).setCellValue(businessData.getTurnover());
            row4.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row4.getCell(6).setCellValue(businessData.getNewUsers());
            XSSFRow row5 = sheet.getRow(4);
            // 获取单元格，填入有效订单数、订单平均价格
            row5.getCell(2).setCellValue(businessData.getValidOrderCount());
            row5.getCell(4).setCellValue(businessData.getUnitPrice());
            // 插入30行明细数据，每行6个单元格的值对应一天的数据概览
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                // 准备每天的明细数据
                businessData = workSpaceService.getBusinessData(LocalDateTime.of(date,LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                XSSFRow row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            // 创建输出流，excel数据放进流里，通过输出流将文件下载到客户端浏览器中
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            // 关闭资源
            out.flush();
            out.close();
            excel.close();
        } catch (IOException e) {
            // 打印错误就行，不要抛异常使程序中断
            e.printStackTrace();
        }
    }
}

