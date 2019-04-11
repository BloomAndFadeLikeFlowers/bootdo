package com.bootdo.common.aspect;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 *
 * 方法拦截  粒度在方法上
 *
 * @desc 调试管理 利用 AOP 原理, 在开发模式下于控制台展示 dao层 的实际执行的SQL
 * 粘出来即可 在pl/sql下执行，已经替换掉 ？ 了
 *
 * @author luotianyi
 * @create 2017-11-30 14:03
 **/
public class DisplayExecuteSqlInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DisplayExecuteSqlInterceptor.class);

    private static final String CONTROLLER ="CONTROLLER";
    private static final String SERVICE ="SERVICE";
    private static final String DAO ="DAO";
    private static final String IMPL ="IMPL";

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {

        //系统的开发模式
        String codeModel =System.getProperty("codeModel");
        String flag="true";
        if(StringUtils.equals(flag,codeModel)){
            //获取该方法的传参
            Object[] ars = mi.getArguments();

            //通过反射机制 获取到该方法 (Method 包含 作用域 返回类型  方法名  参数类型)
            Method method= mi.getMethod();

            //获取代理的对象 （也就是这个方法所在内存中的对象）
            Object obj = mi.getThis();

            Object [] params =new Object[]{} ;
            String sql ="";
            for(Object o :ars){
                if(o instanceof Object[]){
                    params= (Object[]) o;
                }else if(o instanceof String){
                    sql=(String) o;
                }
            }

            Thread current = Thread.currentThread();
            StackTraceElement[] elements =current.getStackTrace();

            //倒序输出 栈帧 信息 ，过滤出 项目的代码 这里只过滤出(Controller / service impl / dao impl)层的代码，如需要其他的可自行遍历
            if(elements !=null && elements.length>0){
                //获得项目名
                String packageName =DisplayExecuteSqlInterceptor.class.getPackage().getName();
                packageName=StringUtils.substringBefore(packageName,".");
                StringBuilder sb =new StringBuilder();
                sb.append(" -------->本次执行SQL的代码在<--------- ");
                sb.append('\n');
                for(int i=elements.length ;i>0 ;i--){
                    StackTraceElement e =elements[i-1];
                    if(StringUtils.contains(e.getClassName(),packageName)){
                        String cn=StringUtils.upperCase(e.getClassName());
                        if(StringUtils.contains(cn,CONTROLLER)){
                            sb.append( CONTROLLER+" 层 ->类名："+e.getClassName()+",方法名："+e.getMethodName()+",代码行数："+e.getLineNumber()+"");
                            sb.append('\n');
                        }else if(StringUtils.contains(cn,SERVICE) &&StringUtils.contains(cn,IMPL) && e.getLineNumber()>0) {
                            sb.append( SERVICE+" 层 ->类名："+e.getClassName()+",方法名："+e.getMethodName()+",代码行数："+e.getLineNumber()+"");
                            sb.append('\n');
                        }else if(StringUtils.contains(cn,DAO) &&StringUtils.contains(cn,IMPL) && e.getLineNumber()>0 ){
                            sb.append(DAO +" 层->类名："+e.getClassName()+",方法名："+e.getMethodName()+",代码行数："+e.getLineNumber()+"");
                            sb.append('\n');
                        }
                    }
                }
                log.info(sb.toString());
            }
            getExecuteSql(sql,params);
        }
        //执行被拦截的方法，切记，如果此方法不调用，则被拦截的方法不会被执行。
        return mi.proceed();
    }

    private String getExecuteSql(String sql, Object[] params) {
        if (StringUtils.isNotBlank(sql)) {
            if (params != null && params.length > 0) {
                int a = getCount(sql, '?');
                int b = params.length;
                if (a == b) {
                    sql = StringUtils.replace(sql, "?", "XXXX");
                    for (int i = 0; i < params.length; i++) {
                        Object obj = params[i];
                        if (StringUtils.isNotBlank(String.valueOf(obj)) && StringUtils.isNumeric(String.valueOf(obj))) {
                            obj = Integer.valueOf(String.valueOf(obj));
                        } else {
                            obj = "'" + obj + "'";
                        }
                        sql = sql.replaceFirst("XXXX", String.valueOf(obj));
                    }
                } else {
                    log.info("参数个数传的不正确, sql中 需要 ：{} 个参数,实际传入参数为 ：{} 个。", a, b);
                    return null;
                }
            }
        }

        StringBuilder sb =new StringBuilder();
        sb.append(" ----------->本次执行sql为：<----------- ");
        sb.append('\n');
        sb.append(sql+'\n');

        log.info(sb.toString());
        return sql;

    }

    private  int getCount(String sql ,char a ){
        int count=0;
        if(StringUtils.isNotBlank(sql)){
            for (int i = 0; i < sql.length(); i++) {
                if(sql.charAt(i)==a){
                    count++;
                }
            }
        }
        return count;
    }

}
