package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.entity.StaffLeave;
import com.qiujie.vo.StaffLeaveVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-04-05
 */
public interface StaffLeaveMapper extends BaseMapper<StaffLeave> {


    @Select("select * from att_staff_leave where is_deleted = 0 and staff_id = #{id}")
    IPage<StaffLeave> listStaffLeaveByStaffId(IPage<StaffLeave> config, @Param("id") Integer id);

    /**
     * 查询组任务以及个人任务
     *
     * @param config
     * @param name
     * @param deptId
     * @param ids
     * @return
     */
    @Select("<script> " +
                "select asl.*,ss.code,ss.name ,ss.phone,sd.name dept_name from att_staff_leave asl inner join sys_staff ss on asl.staff_id = ss.id " +
                "inner join sys_dept sd on ss.dept_id = sd.id " +
                "where asl.is_deleted = 0 and asl.status in (0,4) " +
                "<if test= 'name != null'>" +
                    "and ss.name like concat('%',#{name},'%')" +
                "</if>" +
                "<if test= 'deptId != null'>" +
                    "and ss.dept_id = #{deptId}" +
                "</if>" +
                "<choose>" +
                    "<when test='ids != null and ids.size > 0' >" +
                        "and asl.id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
                    "</when> " +
                    "<otherwise>" +
                        "and 1 != 1" +
                    "</otherwise>" +
                "</choose>" +
            "</script>")
    IPage<StaffLeaveVO> listStaffLeaveVO(IPage<StaffLeaveVO> config, @Param("name") String name, @Param("deptId") Integer deptId, @Param("ids") List<Integer> ids);

}
