package com.sky.interceptor;

import com.alibaba.fastjson.JSON;
import com.sky.constant.JwtClaimsConstant;
import com.sky.context.BaseContext;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     *
     * @param request 请求
     * @param response 响应
     * @param handler 处理器，就是被拦截的方法
     * @return 拦截结果，true表示放行，false表示拦截
     * @throws Exception 抛出的异常
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("当前线程的id:" + Thread.currentThread().getId());

        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            BaseContext.setCurrentId(userId);
            log.info("当前用户id：{}", userId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            // 修复点1：打印具体异常，便于排查问题
            log.error("JWT校验失败", ex);

            // 修复点2：返回JSON格式的错误响应（不再是空响应体）
            response.setStatus(401); // 保留401状态码
            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 设置响应格式为JSON
            response.setCharacterEncoding("UTF-8"); // 防止中文乱码

            // 构建和后端统一的错误返回格式
            Result<String> errorResult = Result.error("登录状态失效，请重新登录");

            // 将错误信息写入响应体
            try (PrintWriter writer = response.getWriter()) {
                writer.write(JSON.toJSONString(errorResult));
                writer.flush();
            }

            return false;
        }

    }

    // 核心修改：在postHandle清理ThreadLocal（响应体输出前）
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            log.info("清理ThreadLocal，当前线程id：{}", Thread.currentThread().getId());
            BaseContext.removeCurrentId(); // 移除而不是set为null
        }
    }

}
