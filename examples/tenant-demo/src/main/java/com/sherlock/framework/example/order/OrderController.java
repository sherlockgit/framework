package com.sherlock.framework.example.order;

import com.sherlock.framework.base.web.ApiResponse;
import com.sherlock.framework.cache.CachePort;
import com.sherlock.framework.security.PrincipalContext;
import com.sherlock.framework.web.trace.TraceIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final OrderMapper orderMapper;
    private final CachePort cache;
    private final TraceIdResolver traceIdResolver;

    public OrderController(OrderMapper orderMapper, CachePort cache, TraceIdResolver traceIdResolver) {
        this.orderMapper = orderMapper;
        this.cache = cache;
        this.traceIdResolver = traceIdResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderView>> create(@RequestBody CreateOrderRequest request,
                                                           HttpServletRequest servletRequest) {
        validate(request);
        long creatorId = PrincipalContext.require().userId();
        orderMapper.insertOrder(request.businessKey(), creatorId, request.departmentId(), request.name());
        OrderView order = requireOrder(request.businessKey());
        cache.put("order", request.businessKey(), order.name(), CACHE_TTL);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, traceIdResolver.resolve(servletRequest)));
    }

    @GetMapping("/{businessKey}")
    public ApiResponse<OrderView> findByBusinessKey(@PathVariable String businessKey, HttpServletRequest servletRequest) {
        OrderView order = requireOrder(businessKey);
        boolean cacheMatchesAuthorizedOrder = cache.get("order", businessKey)
                .filter(order.name()::equals)
                .isPresent();
        if (!cacheMatchesAuthorizedOrder) {
            cache.put("order", businessKey, order.name(), CACHE_TTL);
        }
        return ApiResponse.success(order, traceIdResolver.resolve(servletRequest));
    }

    private OrderView requireOrder(String businessKey) {
        if (businessKey == null || businessKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessKey is required");
        }
        OrderRow row = orderMapper.selectByBusinessKey(businessKey);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found");
        }
        return new OrderView(row.getBusinessKey(), row.getName());
    }

    private static void validate(CreateOrderRequest request) {
        if (request == null || request.businessKey() == null || request.businessKey().isBlank()
                || request.name() == null || request.name().isBlank() || request.departmentId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid order request");
        }
    }

    public record CreateOrderRequest(String businessKey, long departmentId, String name) {
    }

    public record OrderView(String businessKey, String name) {
    }
}
