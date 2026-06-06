package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qiujie.entity.Insurance;
import com.qiujie.vo.StaffInsuranceVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author qiujie
 * @since 2022-03-23
 */
public interface InsuranceMapper extends BaseMapper<Insurance> {


    @Select("select ss.id staff_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name," +
            "si.id insurance_id,si.city_id,si.social_base,si.per_social_pay,si.com_injury_rate,si.com_social_pay,si.social_remark,si.house_base,si.per_house_rate," +
            "si.per_house_pay,si.com_house_rate,si.com_house_pay,si.house_remark from " +
            "(sys_staff ss left join sys_dept sd on ss.dept_id = sd.id) left join soc_insurance si on si.staff_id = ss.id " +
            "where ss.is_deleted = 0 and ss.name like concat('%',#{name},'%')")
    IPage<StaffInsuranceVO> listStaffInsuranceVO(IPage<StaffInsuranceVO> config, @Param("name") String name);

    @Select("select ss.id staff_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name," +
            "si.id insurance_id,si.city_id,si.social_base,si.per_social_pay,si.com_injury_rate,si.com_social_pay,si.social_remark,si.house_base,si.per_house_rate," +
            "si.per_house_pay,si.com_house_rate,si.com_house_pay,si.house_remark from " +
            "(sys_staff ss left join sys_dept sd on ss.dept_id = sd.id) left join soc_insurance si on si.staff_id = ss.id " +
            "where ss.is_deleted = 0 and ss.dept_id = #{deptId} and ss.name like concat('%',#{name},'%')")
    IPage<StaffInsuranceVO> listStaffDeptInsuranceVO(IPage<StaffInsuranceVO> config, @Param("name") String name,@Param("deptId") Integer deptId);

    @Select("select ss.id staff_id,ss.code,ss.name,ss.phone,ss.address,sd.name dept_name," +
            "si.id insurance_id,si.city_id,si.social_base,si.per_social_pay,si.com_injury_rate,si.com_social_pay,si.social_remark,si.house_base,si.per_house_rate," +
            "si.per_house_pay,si.com_house_rate,si.com_house_pay,si.house_remark from " +
            "(sys_staff ss left join sys_dept sd on ss.dept_id = sd.id) left join soc_insurance si on si.staff_id = ss.id " +
            "where ss.is_deleted = 0")
    List<StaffInsuranceVO> queryStaffInsuranceVO();

}
