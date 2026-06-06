package com.qiujie.controller;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.service.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author qiujie
 * @Date 2022/2/27
 * @Version 1.0
 */

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    @GetMapping("/staff")
    public ResponseDTO queryStaff() {
        return this.homeService.queryStaff();
    }

    @GetMapping("/count")
    public ResponseDTO queryCount() {
        return this.homeService.queryCount();
    }

    @GetMapping("/city")
    public ResponseDTO queryCity() {
        return this.homeService.queryCity();
    }

    @GetMapping("/attendance")
    public ResponseDTO queryAttendance(@RequestParam Integer id, String month) {
        return this.homeService.queryAttendance(id, month);
    }
    @GetMapping("/department")
    public ResponseDTO queryDepartment() {
        return this.homeService.queryDepartment();
    }

}
