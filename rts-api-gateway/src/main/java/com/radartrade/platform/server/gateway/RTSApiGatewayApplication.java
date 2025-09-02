package com.radartrade.platform.server.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = "com.radartrade.platform.server.gateway")
public class RTSApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RTSApiGatewayApplication.class, args);
    }
}