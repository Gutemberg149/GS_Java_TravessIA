package br.com.travessia.trafego;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"br.com.travessia.trafego", "br.com.travessia.comum.exception"})
@EnableFeignClients(basePackages = "br.com.travessia.comum.feign")
public class TrafficApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrafficApplication.class, args);
    }
}
