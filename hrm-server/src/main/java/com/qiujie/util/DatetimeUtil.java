package com.qiujie.util;

import cn.hutool.core.date.DateUtil;
import com.qiujie.config.HolidayConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Date;

/**
 * @Author qiujie
 * @Date 2022/4/12
 * @Version 1.0
 */

@Component
public class DatetimeUtil {

    @Autowired
    private HolidayConfig holidayConfig;

    /**
     * 获取月份的日期字符串数据组如，['20220101','20220102','20220103',........]
     *
     * @param month yyyyMM
     * @return
     */
    public String[] getMonthDayList(String month) {
        int monthSize = DateUtil.lengthOfMonth(Integer.valueOf(month.substring(4)), DateUtil.isLeapYear(Integer.valueOf(month.substring(0, 4))));
        String[] monthDayList = new String[monthSize];
        for (int i = 0; i < monthSize; i++) {
            if (i < 9) {
                monthDayList[i] = month + "0" + (i + 1);
            } else {
                monthDayList[i] = month + (i + 1);
            }
        }
        return monthDayList;
    }


    /**
     * 判断当前日期是否是节假日
     *
     * @param date
     * @return
     */
    public boolean isHoliday(Date date) {
        String str = DateUtil.format(date, "yyyy-MM-dd");
        return holidayConfig.getHolidays().contains(str);
    }
}
