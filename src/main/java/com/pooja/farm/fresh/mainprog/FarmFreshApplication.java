package com.pooja.farm.fresh.mainprog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(basePackages = "com.pooja.farm.fresh")
@EntityScan("com.pooja.farm.fresh.entity")
@EnableJpaRepositories(basePackages = {
    "com.pooja.farm.fresh.repository"
})
public class FarmFreshApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmFreshApplication.class, args);
    }
}
