package com.qiujie.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Dept;
import com.qiujie.entity.Staff;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.vo.StaffDeptVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-01-27
 */

@Service
public class StaffService extends ServiceImpl<StaffMapper, Staff> {


    @Autowired
    private DeptService deptService;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;


    /**
     * 新增
     *
     * @param staff
     * @return ResponseDTO
     */
    public ResponseDTO add(Staff staff) {
        if (save(staff)) {
            // 设置默认密码、工号
            staff.setPassword(passwordEncoder.encode("123")).setCode("staff_" + staff.getId());
            updateById(staff);
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 逻辑删除
     *
     * @param id
     * @return
     */
    public ResponseDTO delete(Integer id) {
        if (removeById(id)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 编辑
     *
     * @param staff
     * @return
     */
    public ResponseDTO edit(Staff staff) {
        if (updateById(staff)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 查找
     *
     * @param id
     * @return
     */
    public ResponseDTO query(Integer id) {
        Staff staff = getById(id);
        if (staff != null) {
            return Response.success(staff);
        }
        return Response.error();
    }


    /**
     * 多条件分页查询
     * @param current
     * @param size
     * @param name
     * @param birthday
     * @param deptId
     * @param status
     * @return
     */
    public ResponseDTO list(Integer current, Integer size, String name, String birthday,Integer deptId,Integer status) {
        // 分页构造
        IPage<Staff> pageConfig = new Page<>(current, size);
        QueryWrapper<Staff> wrapper = new QueryWrapper<>();
        if (name != "" && name != null) {
            wrapper.like("name", name);
        }
        if (birthday != null) {
            wrapper.ge("birthday",birthday);
        }
        if (deptId != null) {
            wrapper.eq("dept_id", deptId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        IPage<Staff> page = page(pageConfig, wrapper);
        List<Staff> records = page.getRecords();
        List<StaffDeptVO> staffDeptVOList = new ArrayList<>();
        for (Staff record : records) {
            QueryWrapper<Dept> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", record.getDeptId());
            Dept dept = this.deptService.getOne(queryWrapper);
            // 设置部门和年龄
            StaffDeptVO staffDeptVO = new StaffDeptVO();
            staffDeptVO.setDeptName(dept.getName());
            if (record.getBirthday() != null) {
                staffDeptVO.setAge(DateUtil.ageOfNow(record.getBirthday()));
            }
            BeanUtil.copyProperties(record, staffDeptVO);
            staffDeptVOList.add(staffDeptVO);
        }
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", staffDeptVOList);
        return Response.success(map);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }


    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response, String filename) throws IOException {
        List<StaffDeptVO> list = this.staffMapper.queryStaffDeptVO();
        // 设置员工年龄
        for (StaffDeptVO staffDeptVO : list) {
            if (staffDeptVO.getBirthday() != null) {
                staffDeptVO.setAge(DateUtil.ageOfNow(staffDeptVO.getBirthday()));
            }
        }
        EasyExcelUtil.write(response, list, filename, StaffDeptVO.class);
    }

    /**
     * 数据导入
     *
     * @param file
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO imp(MultipartFile file) throws IOException {
        InputStream inputStream = file.getInputStream();
        List<Staff> list = EasyExcelUtil.read(inputStream, 1, Staff.class);
        for (Staff staff : list) {
            if (!save(staff)) {
                return Response.error();
            }// 设置默认密码、工号、部门
            staff.setPassword(passwordEncoder.encode("123")).setCode("staff_" + staff.getId()).setDeptId(13);
            if (!updateById(staff)) {
                return Response.error();
            }
        }
        return Response.success();
    }

    // 检查员工的密码
    public ResponseDTO validate(String pwd, Integer id) {
        Staff staff = getById(id);
        if (passwordEncoder.matches(pwd, staff.getPassword())) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO reset(Staff staff) {
        // MD5加密
        staff.setPassword(passwordEncoder.encode(staff.getPassword()));
        if (updateById(staff)) {
            return Response.success();
        }
        return Response.error();
    }

    /**
     * 获取员工信息
     *
     * @param id
     * @return
     */
    public ResponseDTO queryInfo(Integer id) {
        StaffDeptVO staffInfo = this.staffMapper.queryInfo(id);
        if (staffInfo != null) {
            return Response.success(staffInfo);
        }
        return Response.error();
    }
}
