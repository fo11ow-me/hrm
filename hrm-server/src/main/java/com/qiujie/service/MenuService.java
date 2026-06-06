package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Menu;
import com.qiujie.mapper.MenuMapper;
import com.qiujie.util.HutoolExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-02-28
 */
@Service
public class MenuService extends ServiceImpl<MenuMapper, Menu> {

    @Autowired
    private MenuMapper menuMapper;


    public ResponseDTO add(Menu menu) {
        if (save(menu)) {
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


    public ResponseDTO edit(Menu menu) {
        if (updateById(menu)) {
            return Response.success();
        }
        return Response.error();
    }


    public ResponseDTO query(Integer id) {
        Menu menu = getById(id);
        if (menu != null) {
            return Response.success(menu);
        }
        return Response.error();
    }

    /**
     * 查找所有菜单
     *
     * @return
     */
    public ResponseDTO queryAll() {
        List<Menu> list = list(new QueryWrapper<Menu>().eq("status",1));
        // 为父级菜单设置子菜单
        // 一级菜单
        List<Menu> firstList = list.stream().parallel()
                .filter(menu -> menu.getLevel() == 0).toList();
        for (Menu firstMenu : firstList) {
            // 二级菜单
            List<Menu> secondList = list.stream().parallel()
                    .filter(menu -> Objects.equals(menu.getParentId(), firstMenu.getId())).toList();
            firstMenu.setChildren(secondList);
            for (Menu secondMenu : secondList) {
                // 权限点
                List<Menu> thirdList = list.stream().parallel()
                        .filter(menu -> Objects.equals(menu.getParentId(), secondMenu.getId())).toList();
                secondMenu.setChildren(thirdList);
            }
        }
        return Response.success(firstList);
    }


    public ResponseDTO list(Integer current, Integer size, String name) {
        IPage<Menu> config = new Page<>(current, size);
        QueryWrapper<Menu> wrapper = new QueryWrapper<>();
        if (name != "" && name != null) {
            wrapper.like("name", name);
        }
        wrapper.eq("level", 0);
        IPage<Menu> page = page(config, wrapper);
        List<Menu> list = list(new QueryWrapper<Menu>().ne("level", 0));
        // 菜单
        List<Menu> firstList = page.getRecords();
        for (Menu firstMenu : firstList) {
            // 设置子菜单
            List<Menu> secondList = list.stream().filter(menu -> Objects.equals(menu.getParentId(), firstMenu.getId())).toList();
            firstMenu.setChildren(secondList);
            for (Menu secondMenu : secondList) {
                // 设置子菜单
                List<Menu> thirdList = list.stream().filter(menu -> Objects.equals(menu.getParentId(), secondMenu.getId())).toList();
                secondMenu.setChildren(thirdList);
            }
        }
        // 将响应数据填充到map中
        Map map = new HashMap();
        map.put("pages", page.getPages());
        map.put("total", page.getTotal());
        map.put("list", firstList);
        return Response.success(map);
    }

    /**
     * 数据导出
     *
     * @param response
     * @return
     */
    public void export(HttpServletResponse response, String filename) throws IOException {
        List<Menu> list = list();
        HutoolExcelUtil.writeExcel(response, list, filename, Menu.class);
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
        List<Menu> list = HutoolExcelUtil.readExcel(inputStream, 1, Menu.class);
        // IService接口中的方法.批量插入数据
        if (saveBatch(list)) {
            return Response.success();
        }
        return Response.error();
    }

    public ResponseDTO queryByStaffId(Integer id) {
        List<Menu> list = this.menuMapper.queryByStaffId(id);
        // 一级菜单
        List<Menu> firstList = list.stream().parallel()
                .filter(menu -> menu.getLevel() == 0).toList();
        for (Menu firstMenu : firstList) {
            // 二级菜单
            List<Menu> secondList = list.stream().parallel()
                    .filter(menu -> Objects.equals(menu.getParentId(), firstMenu.getId())).toList();
            firstMenu.setChildren(secondList);
        }
        return Response.success(firstList);
    }


    public ResponseDTO queryPermission(Integer id){
        return Response.success(this.menuMapper.queryPermission(id));
    }
}




