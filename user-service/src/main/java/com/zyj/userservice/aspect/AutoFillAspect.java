package com.zyj.userservice.aspect;


import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import com.zyj.userservice.annotation.AutoFill;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.aspectj.lang.reflect.MethodSignature;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @Author：zyj
 * @Package：com.zyj.userservice.aspect
 * @Project：yun-shan
 * @name：AutoFillAspect
 * @Date：06 12月 2025  14:06
 * @Filename：AutoFillAspect
 */
@Aspect // 标记为切面类
@Component // 组件。交给spring管理
@Slf4j
public class AutoFillAspect {
    // 切入点 它定义了在哪里应用通知(Advice)，即程序执行过程中的特定连接点
    /*
      匹配 com.sky.mapper 包下所有类的所有方法
      * 表示任意返回类型
      com.sky.mapper.* 表示包下的所有类
      .*(..) 表示类中的所有方法，参数任意
     */
    @Pointcut("execution(* com.zyj.userservice.mapper.*.*(..)) && @annotation(com.zyj.userservice.annotation.AutoFill)")
    public void autoFillPointCut(){
    }


    @Around("autoFillPointCut()")
    public Object doAutoFillAround(ProceedingJoinPoint joinPoint) throws Throwable {
        //程序运行前
        // 1. 拿到被拦截方法的“签名”（包含方法的所有信息：名称、参数、注解等）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 2. 从方法签名中，获取贴在方法上的 @AutoFill 注解对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        // 3. 从注解对象中，取出你指定的操作类型（INSERT 或 UPDATE）
        OperationType operationType = autoFill.value();

        // 2. 获取mapper方法的实体参数（比如User对象）
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            log.info("当前方法没有参数，无法进行自动填充");
            return null;
        }
        Object entity = args[0];//对应的实体对象要放在第一个参数位置

        //准备要赋值的参数
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        if(operationType == OperationType.INSERT){
            //为4个属性赋值(INSERT)
            try {
                /*
                为什么用 getDeclaredMethod 而不是 getMethod？
                getDeclaredMethod：能找到类的 所有方法（包括 private、protected、public），兼容性更强（比如如果你的 set 方法不是 public，也能找到）；
                getMethod：只能找到类的 public 方法，如果 set 方法权限不是 public，就会报错。所以用 getDeclaredMethod 更稳妥。
                 */
                //getDeclaredMethod(方法名, 参数类型)：根据 “方法名” 和 “参数类型”，从 “说明书” 里找到对应的方法（这是反射的核心 API）
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);

                //通过反射 invoke(目标对象, 方法参数)
                setCreateTime.invoke(entity,now);
                setUpdateTime.invoke(entity,now);
                setCreateUser.invoke(entity,currentId);
                setUpdateUser.invoke(entity,currentId);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }else{
            //为2个属性赋值(UPDATE)
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                //通过反射
                setUpdateTime.invoke(entity,now);
                setUpdateUser.invoke(entity,currentId);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

        }

        //执行要运行的程序
        Object result = joinPoint.proceed();

        return result;
    }
}
