package com.pooja.farm.fresh.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pooja.farm.fresh.dto.OrderItemDTO;
import com.pooja.farm.fresh.entity.OrderItem;
import com.pooja.farm.fresh.repository.OrderItemRepository;
import com.pooja.farm.fresh.exception.ResourceNotFoundException; // Correct custom exception

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderItemDTO> getItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(OrderItemDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderItemDTO getById(Long id) {
        OrderItem item = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with ID: " + id));
        return new OrderItemDTO(item);
    }

    public void delete(Long id) {
        if (!orderItemRepository.existsById(id)) {
            throw new ResourceNotFoundException("OrderItem not found with ID: " + id + " for deletion.");
        }
        orderItemRepository.deleteById(id);
    }

    public OrderItem save(OrderItem item) {
        return orderItemRepository.save(item);
    }

    // Optional: Update existing OrderItem
    public OrderItem update(Long id, OrderItem updatedItem) {
        OrderItem existingItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with ID: " + id));

        existingItem.setQuantity(updatedItem.getQuantity());
        existingItem.setProductPrice(updatedItem.getProductPrice());
        existingItem.setDiscountApplied(updatedItem.isDiscountApplied());
        existingItem.setProductName(updatedItem.getProductName());
        existingItem.setProductWeight(updatedItem.getProductWeight());
        existingItem.setProductImageUrl(updatedItem.getProductImageUrl());
        // Add any other fields you wish to update

        return orderItemRepository.save(existingItem);
    }
}
