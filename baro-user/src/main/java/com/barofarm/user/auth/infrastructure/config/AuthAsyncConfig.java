package com.barofarm.user.auth.infrastructure.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AuthAsyncConfig {

    @Bean(name = "opaEventExecutor")
    public Executor opaEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        /*
         * 판매자 승인/정지 같은 관리자 작업은 사용자 응답을 오래 붙잡지 않아야 한다.
         * OPA hotlist 전파는 커밋 이후 비동기로 넘겨 요청 스레드와 분리한다.
         */
        executor.setThreadNamePrefix("opa-event-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
