package com.qiujie.util;

import cn.hutool.core.util.StrUtil;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.vo.StaffDeptVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 安全上下文工具类，提供当前登录用户信息的便捷访问
 *
 * @author qiujie
 */
@Component
public class SecurityUtil {

    @Autowired
    private StaffMapper staffMapper;

    /**
     * 获取当前登录操作人的员工ID
     *
     * @return 员工ID，未登录时返回 null
     */
    public Integer getCurrentOperatorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StrUtil.isNotBlank(authentication.getName())) {
            return null;
        }
        StaffDeptVO staffDeptVO = this.staffMapper.queryByCode(authentication.getName());
        return staffDeptVO == null ? null : staffDeptVO.getId();
    }
}
