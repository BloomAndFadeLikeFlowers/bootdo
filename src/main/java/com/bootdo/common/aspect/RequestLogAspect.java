package com.bootdo.common.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class RequestLogAspect {

    private Logger logger = LoggerFactory.getLogger(RequestLogAspect.class);
    private static String requestBeginFlag = "RequestBegin";
    private static String requestMessageFlag = "RequestMessage";
    private static String requestEndFlag = "RequestEnd";
    private static String execControllerServiceBeginFlag = "execControllerServiceBegin";
    private static String execControllerServiceEndFlag = "execControllerServiceEnd";

    private static final String CONTROLLER = "CONTROLLER";
    private static final String SERVICE = "SERVICE";
    private static final String DAO = "DAO";
    private static final String IMPL = "IMPL";

    @Pointcut("execution( * com.bootdo..controller.*.*(..))")//两个..代表所有子目录，最后括号里的两个..代表所有参数
    public void requestPointCut() {

    }

    @Before("requestPointCut()")
    public void requestBefore(JoinPoint joinPoint) throws Throwable {
//        logger.debug(requestBeginFlag);
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();
        long startTime = System.currentTimeMillis();
        request.setAttribute("startTime", startTime);
    }

    @Around("requestPointCut()")
    public Object requestAround(ProceedingJoinPoint pjp) throws Throwable {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();

//        String url = request.getRequestURL().toString();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String ip = getIpAddr(request);
        Object[] args = pjp.getArgs();
        String params = "";
        Object result = pjp.proceed();
        //getMethodMessage(args);
        long endTime = System.currentTimeMillis();
        try {
            long startTime = (long) request.getAttribute("startTime");
            //获取请求参数集合并进行遍历拼接
            if (args.length > 0) {
                if ("POST".equals(method)) {
                    params = JSONArray.toJSONString(args);
                } else if ("GET".equals(method)) {
                    params = StringUtils.isEmpty(queryString) ? "" : queryString;
                }
                // params = URLDecoder.decode(params, "UTF-8");
            }
            //"requestMethod:{},url:{},params:{},elapsed:{}ms,responseBody:{}."
            logger.debug(requestMessageFlag + ":{}\t{}\t{}\t{}",  method, uri, params, (endTime - startTime));
//            JSON.toJSONString(result, SerializerFeature.WriteMapNullValue)
            //logger.debug(requestEndFlag);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("log error !!", e);
        }

        return result;
    }

    @After("requestPointCut()")
    public void requestAfter() throws Throwable {

    }

    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr()的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值
     *
     * @return ip
     */
    private String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        //System.out.println("x-forwarded-for ip: " + ip);
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
            //   System.out.println("Proxy-Client-IP ip: " + ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
            //   System.out.println("WL-Proxy-Client-IP ip: " + ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
            // System.out.println("HTTP_CLIENT_IP ip: " + ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            // System.out.println("HTTP_X_FORWARDED_FOR ip: " + ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
            //   System.out.println("X-Real-IP ip: " + ip);
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            //  System.out.println("getRemoteAddr ip: " + ip);
        }
        //System.out.println("获取客户端ip: " + ip);
        return ip;
    }

    private void getMethodMessage(Object[] ars) {

        Thread current = Thread.currentThread();
        StackTraceElement[] elements = current.getStackTrace();

        //倒序输出 栈帧 信息 ，过滤出 项目的代码 这里只过滤出(Controller / service impl / dao impl)层的代码，如需要其他的可自行遍历
        if (elements != null && elements.length > 0) {
            //获得项目名
            String packageName = DisplayExecuteSqlInterceptor.class.getPackage().getName();
            packageName = org.apache.commons.lang.StringUtils.substringBefore(packageName, ".");
            StringBuilder sb = new StringBuilder();
            sb.append(execControllerServiceBeginFlag);
            sb.append('\n');
            for (int i = elements.length; i > 0; i--) {
                StackTraceElement e = elements[i - 1];
                if (org.apache.commons.lang.StringUtils.contains(e.getClassName(), packageName)) {
                    String cn = org.apache.commons.lang.StringUtils.upperCase(e.getClassName());
                    if (org.apache.commons.lang.StringUtils.contains(cn, CONTROLLER)) {
                        sb.append(CONTROLLER + " 层 ->类名：" + e.getClassName() + ",方法名：" + e.getMethodName() + ",代码行数：" + e.getLineNumber() + "");
                        sb.append('\n');
                    } else if (org.apache.commons.lang.StringUtils.contains(cn, SERVICE) && org.apache.commons.lang.StringUtils.contains(cn, IMPL) && e.getLineNumber() > 0) {
                        sb.append(SERVICE + " 层 ->类名：" + e.getClassName() + ",方法名：" + e.getMethodName() + ",代码行数：" + e.getLineNumber() + "");
                        sb.append('\n');
                    } else if (org.apache.commons.lang.StringUtils.contains(cn, DAO)) {
                        sb.append(DAO + " 层->类名：" + e.getClassName() + ",方法名：" + e.getMethodName() + ",代码行数：" + e.getLineNumber() + "");
                        sb.append('\n');
                    }
                }
            }
            sb.append(execControllerServiceEndFlag);
            logger.debug(sb.toString());
        }
    }
}
