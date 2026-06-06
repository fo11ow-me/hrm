package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiujie.entity.Menu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-02-28
 */
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("""
            select distinct pm.* from per_menu pm\s
            left join per_role_menu prm on pm.id = prm.menu_id\s
            left join per_role pr on prm.role_id = pr.id\s
            left join per_staff_role psr on pr.id = psr.role_id where psr.staff_id = #{id} and pm.status = 1 and pm.level < 2 and pm.is_deleted = 0""")
    List<Menu> queryByStaffId(@Param("id") Integer id);


    /**
     * 查询当前员工的权限
     * @param id 员工id
     */
    @Select("""
            select distinct pm.* from per_menu pm\s
            left join per_role_menu prm on pm.id = prm.menu_id\s
            left join per_role pr on prm.role_id = pr.id\s
            left join per_staff_role psr on pr.id = psr.role_id where psr.staff_id = #{id} and pm.status = 1 and pm.level > 0 and pm.is_deleted = 0""")
    List<Menu> queryPermission(@Param("id") Integer id);

}
