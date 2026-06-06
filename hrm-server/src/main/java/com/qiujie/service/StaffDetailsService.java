package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.Menu;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffDetails;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.MenuMapper;
import com.qiujie.mapper.StaffMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class StaffDetailsService implements UserDetailsService {

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private MenuMapper menuMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Staff staff = this.staffMapper.selectOne(new QueryWrapper<Staff>().eq("code", username));
        if (staff == null) {
            throw new ServiceException(BusinessStatusEnum.STAFF_NOT_EXIST);
        }
        if (staff.getStatus() == 0){
            throw new ServiceException(BusinessStatusEnum.STAFF_STATUS_ERROR);
        }
        // 查询员工的权限信息
        List<Menu> menuList = this.menuMapper.queryPermission(staff.getId());
        List<GrantedAuthority> list = new ArrayList<>();
        for (Menu menu : menuList) {
            SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(menu.getPermission());
            list.add(simpleGrantedAuthority);
        }
        return new StaffDetails(username, staff.getPassword(), list,
                true, true, true, true);
    }
}
