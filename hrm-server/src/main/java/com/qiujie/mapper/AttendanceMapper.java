package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.entity.Attendance;
import com.qiujie.vo.AttendanceMonthSummaryVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Date;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-03-29
 */

public interface AttendanceMapper extends BaseMapper<Attendance> {


    @Select("select * from att_attendance where is_deleted = 0 and staff_id = #{id} and date_format(attendance_date,'%Y%m%d') = #{day}")
    Attendance queryByStaffIdAndDate(@Param("id") Integer id, @Param("day") String day);

    /**
     * 统计员工迟到、早退、旷工的次数
     *
     * @param id     员工id
     * @param status
     * @param month  月份
     * @return
     */
    @Select("select count(*) from att_attendance where is_deleted = 0 and staff_id = #{id} and status = #{status} and date_format(attendance_date,'%Y%m') = #{month} ")
    Integer countTimes(@Param("id") Integer id, @Param("status") Integer status, @Param("month") String month);


    /**
     * 查找员工休假的日期
     *
     * @param id
     * @param month
     * @return
     */
    @Select("select attendance_date from att_attendance where is_deleted = 0 and staff_id = #{id} and status=#{status} and date_format(attendance_date,'%Y%m') = #{month} ")
    List<Date> queryLeaveDate(@Param("id") Integer id, @Param("status") Integer status, @Param("month") String month);

    @Select({
            "<script>",
            "select * from att_attendance",
            "where is_deleted = 0",
            "and staff_id in",
            "<foreach collection='staffIds' item='staffId' open='(' separator=',' close=')'>",
            "#{staffId}",
            "</foreach>",
            "and attendance_date in",
            "<foreach collection='dates' item='date' open='(' separator=',' close=')'>",
            "#{date}",
            "</foreach>",
            "</script>"
    })
    List<Attendance> queryByStaffIdsAndDates(@Param("staffIds") Collection<Integer> staffIds,
                                             @Param("dates") Collection<Date> dates);

    @Select("select staff_id as staffId, " +
            "sum(case when status = 1 then 1 else 0 end) as lateTimes, " +
            "sum(case when status = 2 then 1 else 0 end) as leaveEarlyTimes, " +
            "sum(case when status = 3 then 1 else 0 end) as absenteeismTimes, " +
            "sum(case when status = 4 and dayofweek(attendance_date) not in (1,7) then 1 else 0 end) as leaveDays, " +
            "sum(case when status = 5 then 1 else 0 end) as timeOffDays " +
            "from att_attendance " +
            "where is_deleted = 0 and date_format(attendance_date,'%Y%m') = #{month} " +
            "group by staff_id")
    List<AttendanceMonthSummaryVO> queryMonthSummary(@Param("month") String month);

    @Select({
            "<script>",
            "select staff_id as staffId, " +
            "sum(case when status = 1 then 1 else 0 end) as lateTimes, " +
            "sum(case when status = 2 then 1 else 0 end) as leaveEarlyTimes, " +
            "sum(case when status = 3 then 1 else 0 end) as absenteeismTimes, " +
            "sum(case when status = 4 and dayofweek(attendance_date) not in (1,7) then 1 else 0 end) as leaveDays, " +
            "sum(case when status = 5 then 1 else 0 end) as timeOffDays " +
            "from att_attendance " +
            "where is_deleted = 0 and date_format(attendance_date,'%Y%m') = #{month} " +
            "and staff_id in " +
            "<foreach collection='staffIds' item='staffId' open='(' separator=',' close=')'>",
            "#{staffId}",
            "</foreach> " +
            "group by staff_id",
            "</script>"
    })
    List<AttendanceMonthSummaryVO> queryMonthSummaryByStaffIds(@Param("month") String month, @Param("staffIds") Collection<Integer> staffIds);

}
