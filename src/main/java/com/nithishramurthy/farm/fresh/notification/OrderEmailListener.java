package com.nithishramurthy.farm.fresh.notification;

import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.repository.OrderRepository;
import com.nithishramurthy.farm.fresh.service.MailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends order emails, but only after the change they describe has been saved.
 * If the transaction rolls back (say, the stock ran out), no "confirmed" email
 * goes out. The actual sending happens on a background thread with retries
 * (MailDispatcher), so a slow mail server never holds up an order.
 */
@Component
public class OrderEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEmailListener.class);

    private final OrderRepository orderRepository;
    private final OrderEmailTemplates templates;
    private final MailService mailService;

    public OrderEmailListener(OrderRepository orderRepository, OrderEmailTemplates templates, MailService mailService) {
        this.orderRepository = orderRepository;
        this.templates = templates;
        this.mailService = mailService;
    }

    // fallbackExecution: cash-on-delivery checkout publishes after its transaction
    // has already committed, so there's no transaction to wait for.
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onOrderEmail(OrderEmailEvent event) {
        try {
            Order order = orderRepository.findById(event.orderId()).orElse(null);
            if (order == null) {
                return;
            }
            mailService.sendOrderEmail(templates.build(order, event.type()));
        } catch (RuntimeException e) {
            // An email problem must never undo or fail the order change itself.
            log.error("Couldn't send {} email for order {}", event.type(), event.orderId(), e);
        }
    }
}
