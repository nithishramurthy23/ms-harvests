package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.admin.AdminProduct;
import com.nithishramurthy.farm.fresh.dto.admin.SalesSummary;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Owner-only: how the shop is doing. */
@Service
public class OwnerService {

    static final int LOW_STOCK_THRESHOLD = 5;

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OwnerService(UserRepository userRepository, OrderRepository orderRepository,
                        ProductRepository productRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Builds the owner's overview. Revenue only counts DELIVERED orders, because that money
     * is certain. Orders still on their way (pending, processing, shipped) are added up
     * separately as "open". Products with 5 or fewer left are listed so they can be restocked.
     */
    @Transactional(readOnly = true)
    public SalesSummary summary() {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, orderRepository.countByStatus(status));
        }
        BigDecimal open = orderRepository.sumTotalByStatus(OrderStatus.PENDING)
                .add(orderRepository.sumTotalByStatus(OrderStatus.PROCESSING))
                .add(orderRepository.sumTotalByStatus(OrderStatus.SHIPPED));
        List<AdminProduct> lowStock = productRepository
                .findByActiveTrueAndStockQuantityLessThanEqualOrderByStockQuantityAsc(LOW_STOCK_THRESHOLD)
                .stream().map(AdminProduct::from).toList();

        return new SalesSummary(
                byStatus,
                orderRepository.sumTotalByStatus(OrderStatus.DELIVERED),
                open,
                userRepository.countByRole(Role.USER.authority()) + userRepository.countByRole(Role.MEMBER.authority()),
                LOW_STOCK_THRESHOLD,
                lowStock);
    }
}
