package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Insurance;
import com.qiujie.entity.Staff;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.InsuranceMapper;
import com.qiujie.util.EasyExcelUtil;
import com.qiujie.vo.StaffInsuranceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-03-23
 */
@Service
public class InsuranceService extends ServiceImpl<InsuranceMapper, Insurance> {

    @Autowired
    private InsuranceMapper insuranceMapper;

    public ResponseDTO add(Insurance insurance) {
        if (save(insurance)) {
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

    @Transactional(rollbackFor = Exception.class)
    public ResponseDTO deleteBatch(List<Integer> ids) {
        if (removeBatchByIds(ids)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO edit(Insurance insurance) {
        if (updateById(insurance)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        Insurance insurance = getById(id);
        if (insurance != null) {
            return Response.success(insurance);
        }
        return Response.error();
    }


    public ResponseDTO list(Integer current, Integer size,  String name, Integer deptId) {
        if(name == null){
            name = "";
        }
        IPage<StaffInsuranceVO> config = new Page<>(current, size);
        IPage<StaffInsuranceVO> page;
        if(deptId == null) {
            page = this.insuranceMapper.listStaffInsuranceVO(config, name);
        }else{
            page = this.insuranceMapper.listStaffDeptInsuranceVO(config, name,deptId);
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", page.getRecords());
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response,String filename) throws IOException {
        List<StaffInsuranceVO> list = this.insuranceMapper.queryStaffInsuranceVO();
        EasyExcelUtil.write(response, list, filename, StaffInsuranceVO.class);
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
        List<Insurance> list = EasyExcelUtil.read(inputStream, 1, Insurance.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO setInsurance(Insurance insurance) {
        QueryWrapper<Insurance> queryWrapper = new QueryWrapper();
        queryWrapper.eq("staff_id", insurance.getStaffId());
        if (saveOrUpdate(insurance, queryWrapper)) {
            return Response.success(BusinessStatusEnum.SUCCESS);
        }
        return Response.error();
    }

    public ResponseDTO queryByStaffId(Integer id) {
        Insurance insurance = getOne(new QueryWrapper<Insurance>().eq("staff_id", id));
        if (insurance == null) {
            return Response.error();
        }
        return Response.success(insurance);
    }
}




