package com.tvajjala;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.common.util.concurrent.RateLimiter;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

@EnableCircuitBreaker
@EnableFeignClients
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class EurekaClientApplication {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(EurekaClientApplication.class, args);
    }
}

@FeignClient("greeting-service")
interface GreetingClient {

    @RequestMapping(method = RequestMethod.GET, value = "/greetings/{name}")
    Greeting greet(@PathVariable("name") String name);

}

@RestController
class GreetingApiGatewayController {
    GreetingClient greetingClient;

    public GreetingApiGatewayController(GreetingClient greetingClient) {
        this.greetingClient = greetingClient;
    }

    public String fallback(String name) {
        return "core service down";
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @RequestMapping(method = RequestMethod.GET, value = "/greet/{name}")
    public String greet(@PathVariable String name) {

        return greetingClient.greet(name).getGreeting();
        // return restTemplate.exchange("http://greeting-service/greetings/{name}", HttpMethod.GET, null, Greeting.class, name).getBody().getGreeting();
    }
}

class Greeting {

    private String greeting;

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }
}

@Component
class RateLimitingZuulFilter extends ZuulFilter {

    private final RateLimiter rateLimiter = RateLimiter.create(1.0 / 30.0);

    @Override
    public Object run() {

        final RequestContext context = RequestContext.getCurrentContext();
        final HttpServletResponse response = context.getResponse();

        if (!this.rateLimiter.tryAcquire()) {

            try {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().append(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
                context.setSendZuulResponse(false);
            } catch (final IOException e) {
                ReflectionUtils.rethrowRuntimeException(e);
            }
        }

        return null;
    }

    @Override
    public boolean shouldFilter() {

        return true;
    }

    @Override
    public int filterOrder() {

        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public String filterType() {

        return "pre";
    }

}
