package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qiujie.entity.RoleMenu;
import com.qiujie.entity.StaffRole;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.RoleMenuMapper;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author qiujie
 * @since 2022-02-28
 */
@Service
public class RoleMenuService extends ServiceImpl<RoleMenuMapper, RoleMenu> {

    /**
     * 为角色设置菜单
     *
     * @param roleId
     * @param menuIds
     * @return
     */
    @Transactional
    public ResponseDTO setMenu(Integer roleId, List<Integer> menuIds) {
        remove(new QueryWrapper<RoleMenu>().eq("role_id", roleId));
        for (Integer menuId : menuIds) {
            save(new RoleMenu().setRoleId(roleId).setMenuId(menuId));
        }
        return Response.success();
    }

    /**
     * 获取角色的菜单
     *
     * @param roleId
     * @return
     */
    public ResponseDTO queryByRoleId(Integer roleId) {
        List<RoleMenu> list = list(new QueryWrapper<RoleMenu>().eq("role_id", roleId));
        return Response.success(list);
    }


}




