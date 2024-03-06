package com.parsa.middleware.config;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Aspect class for logging method calls and returns.
 * Methods annotated with appropriate pointcuts will have their entry and exit points logged.
 */
@Aspect
@Component
public class LoggingAspect {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Pointcut definition for all methods in the com.parsa.middleware package.
     */
    @Pointcut("execution(* com.parsa.middleware..*.*(..))")
    private void allMethods() {}

    /**
     * Advice method for logging method entry.
     *
     * @param joinPoint The JoinPoint representing the intercepted method.
     * @throws JsonProcessingException If JSON serialization fails.
     */
    @Before("allMethods()")
    public void logMethodCall(JoinPoint joinPoint) throws JsonProcessingException {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        logger.info("Executing method: " + methodName );

        for (Object arg : args) {
            logger.info("Argument: " + arg);
        }


    }
    /**
     * Advice method for logging method return.
     *
     * @param joinPoint    The JoinPoint representing the intercepted method.
     * @param returnValue The return value of the intercepted method.
     * @throws JsonProcessingException If JSON serialization fails.
     */
    @AfterReturning(pointcut = "allMethods()", returning = "returnValue")
    public void logMethodReturn(JoinPoint joinPoint, Object returnValue) throws JsonProcessingException {
        String methodName = joinPoint.getSignature().toShortString();
        logger.info("Returning from method: " + methodName + ", Return Value: " + returnValue);
    }


}