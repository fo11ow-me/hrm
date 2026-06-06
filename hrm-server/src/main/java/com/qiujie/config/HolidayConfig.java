package com.qiujie.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties // 指明前缀，因为没有前缀，所以不用指明，默认为”“即可
public class HolidayConfig {
    private List<String> holidays;
}
