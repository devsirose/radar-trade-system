package com.radartrade.platform.service.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
public class RTSPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RTSPaymentServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Đặt timezone mặc định cho toàn JVM
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.out.println("JVM default timezone set to: " + TimeZone.getDefault().getID());
    }
}
