//package org.ccs.m3u8sync.config;
//
//import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
//import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.AsyncConfigurer;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.concurrent.Executor;
//
//@Configuration
//@EnableAsync(proxyTargetClass = true)//利用@EnableAsync注解开启异步任务支持
//@ComponentScan("org.ccs.m3u8sync") //必须加此注解扫描包
//public class CustomMultiThreadingConfig implements AsyncConfigurer {
//    @Override
//    public Executor getAsyncExecutor() {
//        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
//        taskExecutor.setCorePoolSize(10);
//        taskExecutor.setMaxPoolSize(20);
//        taskExecutor.setQueueCapacity(500);
//        //当提交的任务个数大于QueueCapacity，就需要设置该参数，但spring提供的都不太满足业务场景，可以自定义一个，也可以注意不要超过QueueCapacity即可
//        //taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//        taskExecutor.setAwaitTerminationSeconds(10);
//        taskExecutor.setThreadNamePrefix("thread-");
//        taskExecutor.initialize();
//        return taskExecutor;
//    }
//
//    @Override
//    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
//        return new SimpleAsyncUncaughtExceptionHandler();
//    }
//}