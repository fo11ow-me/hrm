package com.qiujie.listener;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.Response;
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
import com.qiujie.service.StaffLeaveService;
import com.qiujie.util.DatetimeUtil;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;

@Component
public class ManagerApproveListener implements ExecutionListener {

    @Autowired
    private StaffLeaveService staffLeaveService;

    @Autowired
    private StaffOvertimeMapper staffOvertimeMapper;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private DatetimeUtil datetimeUtil;

    @Override
    @Transactional
    public void notify(DelegateExecution execution) {
        StaffLeave staffLeave = this.staffLeaveService.getOne(new QueryWrapper<StaffLeave>().eq("id", Integer.valueOf(execution.getProcessInstanceBusinessKey())));
        for (int i = 0; i < staffLeave.getDays(); i++) {
            Date attendanceDate = DateUtil.offsetDay(staffLeave.getStartDate(), i).toSqlDate();
            // 因为周末本就要休息，所以只需记录在休假期间包括的工作日的考勤状态到数据库
            if (DateUtil.isWeekend(attendanceDate) || this.datetimeUtil.isHoliday(attendanceDate)) {
                continue;
            }
            Attendance attendance = new Attendance().setAttendanceDate(attendanceDate).setStaffId(staffLeave.getStaffId());
            // 如果请假类型是调休，考勤状态设为调休；其他类型的假期都设为休假
            if (staffLeave.getTypeNum() == LeaveEnum.TIME_OFF) {
                attendance.setStatus(AttendanceStatusEnum.TIME_OFF);
                // 删除员工的一条调休记录
                this.staffOvertimeMapper.delete(new QueryWrapper<StaffOvertime>()
                        .eq("staff_id", staffLeave.getStaffId())
                        .eq("status", OvertimeStatusEnum.TIME_OFF).orderByAsc("overtime_date").last("limit 1"));
            } else {
                attendance.setStatus(AttendanceStatusEnum.LEAVE);
            }
            QueryWrapper<Attendance> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("staff_id", attendance.getStaffId()).eq("attendance_date", attendance.getAttendanceDate());
            if (!this.attendanceService.saveOrUpdate(attendance, queryWrapper)) {
                throw new ServiceException(BusinessStatusEnum.ERROR);
            }
        }
    }
}
