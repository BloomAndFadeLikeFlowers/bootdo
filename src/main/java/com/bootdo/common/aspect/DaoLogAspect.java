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

//@Aspect
//@Component
public class DaoLogAspect {

    private static Logger logger = LoggerFactory.getLogger(DaoLogAspect.class);
    private static String requestBeginFlag = "RequestBegin";
    private static String requestMessageFlag = "RequestMessage";
    private static String requestEndFlag = "RequestEnd";
    private static String execControllerServiceBeginFlag = "execControllerServiceBegin";
    private static String execControllerServiceEndFlag = "execControllerServiceEnd";

    private static final String CONTROLLER = "CONTROLLER";
    private static final String SERVICE = "SERVICE";
    private static final String DAO = "DAO";
    private static final String IMPL = "IMPL";

    @Pointcut("execution( * com.bootdo..dao.*.*(..))")//两个..代表所有子目录，最后括号里的两个..代表所有参数
    public void controllerServicePointCut() {

    }


    @Before("controllerServicePointCut()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
    }

    @Around("controllerServicePointCut()")
    public static Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();
        String uri = request.getRequestURI();

        Object[] args = pjp.getArgs();
        getMethodMessage(args);

        return pjp.proceed();
    }

    @After("controllerServicePointCut()")
    public void doAfter() throws Throwable {
    }

    private static void getMethodMessage(Object[] ars) {

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
