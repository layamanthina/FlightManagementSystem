package com.flightmanagement.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    private HandlerFilterFunction<ServerResponse, ServerResponse> injectUserHeaders() {
        return (request, next) -> {
            String email = (String) request.servletRequest().getAttribute("X-User-Email");
            String role  = (String) request.servletRequest().getAttribute("X-User-Role");
            if (email != null) {
                ServerRequest mutated = ServerRequest.from(request)
                        .header("X-User-Email", email)
                        .header("X-User-Role", role != null ? role : "")
                        .build();
                return next.handle(mutated);
            }
            return next.handle(request);
        };
    }

    @Bean
    public RouterFunction<ServerResponse> userServiceRoute() {
        return route("user-service")
                .route(path("/api/auth/**").or(path("/api/users/**")), http("http://localhost:8084"))
                .filter(injectUserHeaders())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> flightServiceRoute() {
        return route("flight-service")
                .route(path("/api/flights/**"), http("http://localhost:8082"))
                .filter(injectUserHeaders())
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> bookingServiceRoute() {
        return route("booking-service")
                .route(path("/api/bookings/**"), http("http://localhost:8083"))
                .filter(injectUserHeaders())
                .build();
    }
}
