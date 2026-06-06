package com.qiujie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.StaffOvertime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * <p>
 * 员工加班表 Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2024-03-20
 */
public interface StaffOvertimeMapper extends BaseMapper<StaffOvertime> {

    @Select("select * from att_staff_overtime where is_deleted = 0 and staff_id = #{id} and date_format(overtime_date,'%Y%m%d') = #{day}")
    StaffOvertime queryByStaffIdAndDate(@Param("id") Integer id, @Param("day") String day);


    /**
     * 统计员工加班次数、获得的调休天数
     *
     * @param id     员工id
     * @param status 状态
     * @param month  月份
     */
    @Select("select count(*) from att_staff_overtime where is_deleted = 0 and staff_id = #{id} and status = #{status} and date_format(overtime_date,'%Y%m') = #{month} ")
    Integer countTimes(@Param("id") Integer id, @Param("status") Integer status, @Param("month") String month);


    /**
     * 合计员工当前月的加班工资
     */
    @Select("select sum(overtime_salary) from  att_staff_overtime WHERE is_deleted = 0 and staff_id = #{id} and date_format(overtime_date,'%Y%m') = #{month}")
    BigDecimal sumMonthOvertimeSalary(@Param("id") Integer id, @Param("month") String month);

}
