package com.qiujie.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.City;
import com.qiujie.mapper.CityMapper;
import com.qiujie.util.HutoolExcelUtil;

import lombok.Data;


/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-03-23
 */
@Service
public class CityService extends ServiceImpl<CityMapper, City> {


    public ResponseDTO add(City city) {
        if (save(city)) {
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


    public ResponseDTO edit(City city) {
        if (updateById(city)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        City city = getById(id);
        if (city != null) {
            return Response.success(city);
        }
        return Response.error();
    }

    public ResponseDTO queryAll() {
        List<City> list = list();
        return Response.success(list);
    }


    public ResponseDTO list(Integer current, Integer size, String name) {
        IPage<City> config = new Page<>(current, size);
        QueryWrapper<City> wrapper = null;
        if (name != "" && name != null) {
            wrapper = new QueryWrapper<>();
            wrapper.like("name", name);
        }
        IPage<City> page = page(config, wrapper);
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
        List<City> list = list();
        HutoolExcelUtil.writeExcel(response, list,filename , City.class);
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
        List<City> list = HutoolExcelUtil.readExcel(inputStream, 1, City.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }


}




