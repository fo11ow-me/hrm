package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.entity.Leave;
import com.qiujie.entity.Overtime;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.LeaveEnum;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.mapper.OvertimeMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.util.EnumUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 加班表 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-03-28
 */
@Service
public class OvertimeService extends ServiceImpl<OvertimeMapper, Overtime> {


    public ResponseDTO add(Overtime overtime) {
        if (save(overtime)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO delete(Integer id) {
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO edit(Overtime overtime) {
        if (updateById(overtime)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        Overtime overtime = getById(id);
        if (overtime != null) {
            return Response.success(overtime);
        }
        return Response.error();
    }

    public ResponseDTO queryByDeptIdAndTypeNum(Integer deptId,String typeNum) {
        QueryWrapper<Overtime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", deptId).eq("type_num", EnumUtil.fromValue(OvertimeEnum.class,typeNum));
        Overtime one = getOne(queryWrapper);
        if (one != null) {
            return Response.success(one);
        }
        return Response.error();
    }


    public ResponseDTO setOvertime(Overtime overtime) {
        QueryWrapper<Overtime> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dept_id", overtime.getDeptId()).eq("type_num", overtime.getTypeNum());
        if (saveOrUpdate(overtime, queryWrapper)) {
            return Response.success();
        }
        return Response.error();
    }


    /**
     * 获取所有加班类型
     * @return
     */
    public ResponseDTO queryAll() {
        List<Map<String, Object>> enumList = EnumUtil.getEnumList(OvertimeEnum.class);
        for (Map<String, Object> map : enumList) {
            for (OvertimeEnum overtimeEnum : OvertimeEnum.values()) {
                if (map.get("code") == overtimeEnum.getCode()) {
                    map.put("lowerLimit", overtimeEnum.getLowerLimit());
                }
            }
        }
        return Response.success(enumList);
    }


}




