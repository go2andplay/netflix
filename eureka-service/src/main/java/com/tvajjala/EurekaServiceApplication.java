package com.tvajjala;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableDiscoveryClient
@SpringBootApplication
public class EurekaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServiceApplication.class, args);
    }
}

@RestController
class GreetingController {

    @RequestMapping(path = "/greetings/{name}")
    Map<String, String> greeting(@PathVariable String name, @RequestHeader("x-forwarded-host") Optional<String> host,
            @RequestHeader("x-forwarded-host") Optional<Integer> port) {

        host.ifPresent(h -> System.out.println("Host " + h));
        port.ifPresent(p -> System.out.println("Port " + p));
        System.out.println("Returning from the 8082 port greeting-service");
        return Collections.singletonMap("greeting", String.format("Hello %s !", name));
    }
}
