package com.nithishramurthy.farm.fresh.mainprog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/** Starts the Spring Boot backend. @EnableAsync lets emails be sent on background threads. */
@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.nithishramurthy.farm.fresh")
@EntityScan("com.nithishramurthy.farm.fresh.entity")
@EnableJpaRepositories(basePackages = {
    "com.nithishramurthy.farm.fresh.repository"
})
public class FarmFreshApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmFreshApplication.class, args);
    }
}
