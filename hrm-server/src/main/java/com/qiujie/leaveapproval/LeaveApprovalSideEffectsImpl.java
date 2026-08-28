package com.qiujie.leaveapproval;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.StaffLeave;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.enums.LeaveEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.service.AttendanceService;
import com.qiujie.util.DatetimeUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;

/**
 * 请假审批副作用生产实现——收敛 {@code ManagerApproveListener} 的考勤/调休同步逻辑。
 * <p>
 * 规则（与历史实现保持一致）：
 * <ul>
 *   <li>逐日遍历请假天数，跳过周末与节假日（本应休息的工作日不写考勤）</li>
 *   <li>{@code TIME_OFF}：考勤状态为调休，并删除该员工最早的一条调休记录</li>
 *   <li>其他类型：考勤状态为休假</li>
 *   <li>已有考勤记录则更新，否则新增</li>
 * </ul>
 * 监听器仅调用 {@link #onLeaveApproved(StaffLeave)}，不再内联业务逻辑。
 * </p>
 */
@Component
public class LeaveApprovalSideEffectsImpl implements LeaveApprovalSideEffects {

    private final AttendanceService attendanceService;
    private final StaffOvertimeMapper staffOvertimeMapper;
    private final DatetimeUtil datetimeUtil;

    public LeaveApprovalSideEffectsImpl(AttendanceService attendanceService,
                                        StaffOvertimeMapper staffOvertimeMapper,
                                        DatetimeUtil datetimeUtil) {
        this.attendanceService = attendanceService;
        this.staffOvertimeMapper = staffOvertimeMapper;
        this.datetimeUtil = datetimeUtil;
    }

    @Override
    @Transactional
    public void onLeaveApproved(StaffLeave staffLeave) {
        for (int i = 0; i < staffLeave.getDays(); i++) {
            Date attendanceDate = DateUtil.offsetDay(staffLeave.getStartDate(), i).toSqlDate();
            // 周末本就要休息，跳过；节假日同样无需记录考勤
            if (DateUtil.isWeekend(attendanceDate) || datetimeUtil.isHoliday(attendanceDate)) {
                continue;
            }
            Attendance attendance = new Attendance()
                    .setAttendanceDate(attendanceDate)
                    .setStaffId(staffLeave.getStaffId());
            if (staffLeave.getTypeNum() == LeaveEnum.TIME_OFF) {
                // 调休：考勤状态为调休，并扣减员工一条调休记录
                attendance.setStatus(AttendanceStatusEnum.TIME_OFF);
                staffOvertimeMapper.delete(new QueryWrapper<StaffOvertime>()
                        .eq("staff_id", staffLeave.getStaffId())
                        .eq("status", OvertimeStatusEnum.TIME_OFF)
                        .orderByAsc("overtime_date").last("limit 1"));
            } else {
                attendance.setStatus(AttendanceStatusEnum.LEAVE);
            }
            Attendance existing = attendanceService.getOne(new QueryWrapper<Attendance>()
                    .eq("staff_id", attendance.getStaffId())
                    .eq("attendance_date", attendance.getAttendanceDate()));
            if (existing != null) {
                attendance.setId(existing.getId());
                if (!attendanceService.updateById(attendance)) {
                    throw new ServiceException(BusinessStatusEnum.ERROR);
                }
            } else {
                if (!attendanceService.save(attendance)) {
                    throw new ServiceException(BusinessStatusEnum.ERROR);
                }
            }
        }
    }
}