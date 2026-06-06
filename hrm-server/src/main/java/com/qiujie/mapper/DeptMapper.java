package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.entity.Dept;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-03-07
 */
public interface DeptMapper extends BaseMapper<Dept> {

    @Select("select * from sys_dept where is_deleted = 0 and parent_id = 0 and name like concat('%',#{name},'%')")
    IPage<Dept> listParentDept(IPage<Dept> config, @Param("name") String name);

    /**
     * 查找所有子部门
     *
     */
    @Select("select * from sys_dept where is_deleted = 0 and parent_id != 0")
    List<Dept> queryAllSub();

    /**
     * 查找员工所在的部门
     */
    @Select("select sd.* from sys_dept sd right join sys_staff ss on sd.id = ss.dept_id where ss.is_deleted = 0 and ss.id = #{id} ")
    Dept queryByStaffId(@Param("id") Integer id);

}
