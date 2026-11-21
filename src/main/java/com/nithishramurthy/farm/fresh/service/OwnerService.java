package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.admin.AdminProduct;
import com.nithishramurthy.farm.fresh.dto.admin.SalesSummary;
import com.nithishramurthy.farm.fresh.dto.admin.UserView;
import com.nithishramurthy.farm.fresh.entity.OrderStatus;
import com.nithishramurthy.farm.fresh.entity.Role;
import com.nithishramurthy.farm.fresh.entity.User;
import com.nithishramurthy.farm.fresh.exception.ResourceNotFoundException;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.repository.ProductRepository;
import com.nithishramurthy.farm.fresh.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Owner-only actions: who has which role, and how the shop is doing. */
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

    @Transactional(readOnly = true)
    public List<UserView> listUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(UserView::from).toList();
    }

    /**
     * Gives a user a new role. An owner can't change their own role, and the
     * last owner can't be demoted, so the shop is never left without one.
     */
    @Transactional
    public UserView changeRole(Long userId, Role role, Long actingUserId) {
        if (userId.equals(actingUserId)) {
            throw new IllegalStateException("You can't change your own role.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.roleEnum() == Role.OWNER && role != Role.OWNER
                && userRepository.countByRole(Role.OWNER.authority()) <= 1) {
            throw new IllegalStateException("The shop needs at least one owner.");
        }
        user.setRole(role.authority());
        return UserView.from(userRepository.save(user));
    }

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
