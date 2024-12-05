package com.picktartup.contractservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class ContractserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContractserviceApplication.class, args);
    }

}
