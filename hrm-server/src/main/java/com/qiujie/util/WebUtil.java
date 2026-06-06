package com.qiujie.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WebUtil {

    /**
     * 渲染字符串到前端
     *
     * @param response
     * @param str
     */
    public static void renderString(HttpServletResponse response, String str){
        try{
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
