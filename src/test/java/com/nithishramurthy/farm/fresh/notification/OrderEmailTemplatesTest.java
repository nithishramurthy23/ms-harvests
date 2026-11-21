package com.nithishramurthy.farm.fresh.notification;

import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;
import com.nithishramurthy.farm.fresh.entity.User;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEmailTemplatesTest {

    private final OrderEmailTemplates templates = new OrderEmailTemplates("https://shop.example/");

    @Test
    void confirmationForCashOnDeliveryListsTheItemsAndTheCashToKeepReady() {
        OrderEmail email = templates.build(order(PaymentStatus.PAY_ON_DELIVERY), OrderEmailType.CONFIRMED);

        assertThat(email.to()).isEqualTo("asha@example.com");
        assertThat(email.subject()).isEqualTo("Farm Fresh: Order #41 confirmed");
        assertThat(email.text())
                .startsWith("Thanks, Asha! Your order is confirmed.")
                .contains("keep ₹1,398 ready to pay in cash")
                .contains("2 x Kiwi (500 g)  ₹298")
                .contains("1 x Raw Honey  ₹1,100")
                .contains("Your orders: https://shop.example/orders");
        assertThat(email.html()).contains("<h1").contains("2 &times; Kiwi (500 g)").contains("href=\"https://shop.example/orders\"");
    }

    @Test
    void confirmationForAnOnlineOrderSaysItsPaid() {
        OrderEmail email = templates.build(order(PaymentStatus.PAID), OrderEmailType.CONFIRMED);
        assertThat(email.text()).contains("We've received your payment of ₹1,398.").doesNotContain("cash");
    }

    @Test
    void cancellationExplainsWhatHappensToTheMoney() {
        assertThat(templates.build(order(PaymentStatus.REFUND_PENDING), OrderEmailType.CANCELLED).text())
                .contains("We're refunding ₹1,398").contains("5 to 7 working days");
        assertThat(templates.build(order(PaymentStatus.UNPAID), OrderEmailType.CANCELLED).text())
                .contains("didn't receive the payment in time").contains("Nothing was charged");
        assertThat(templates.build(order(PaymentStatus.PAY_ON_DELIVERY), OrderEmailType.CANCELLED).text())
                .contains("Nothing was charged");
    }

    @Test
    void shippedDeliveredAndRefundedHaveTheirOwnSubjects() {
        assertThat(templates.build(order(PaymentStatus.PAY_ON_DELIVERY), OrderEmailType.SHIPPED).subject())
                .endsWith("Order #41 is on its way");
        assertThat(templates.build(order(PaymentStatus.PAID), OrderEmailType.DELIVERED).subject())
                .endsWith("Order #41 delivered");
        assertThat(templates.build(order(PaymentStatus.REFUNDED), OrderEmailType.REFUNDED).subject())
                .endsWith("Refund for order #41 sent");
    }

    @Test
    void customerTextIsEscapedInTheHtml() {
        Order order = order(PaymentStatus.PAID);
        order.getUser().setName("<script>alert(1)</script>");
        order.setShippingAddress("12 MG Road <b>");

        String html = templates.build(order, OrderEmailType.CONFIRMED).html();
        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;").contains("12 MG Road &lt;b&gt;");
    }

    @Test
    void formatsRupeesTheIndianWay() {
        assertThat(OrderEmailTemplates.rupees(new BigDecimal("149.00"))).isEqualTo("₹149");
        assertThat(OrderEmailTemplates.rupees(new BigDecimal("149.5"))).isEqualTo("₹149.50");
        assertThat(OrderEmailTemplates.rupees(new BigDecimal("125000"))).isEqualTo("₹1,25,000");
        assertThat(OrderEmailTemplates.rupees(new BigDecimal("1234567.5"))).isEqualTo("₹12,34,567.50");
        assertThat(OrderEmailTemplates.rupees(new BigDecimal("999"))).isEqualTo("₹999");
    }

    private static Order order(PaymentStatus payment) {
        User user = new User();
        user.setName("Asha Rao");
        user.setEmail("asha@example.com");

        Order order = new Order();
        order.setId(41L);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("1398.00"));
        order.setShippingAddress("12 MG Road, Bengaluru");
        order.setPaymentStatus(payment);
        order.getOrderItems().add(item("Kiwi", "500 g", "149.00", 2));
        order.getOrderItems().add(item("Raw Honey", null, "1100.00", 1));
        return order;
    }

    private static OrderItem item(String name, String weight, String price, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductName(name);
        item.setProductWeight(weight);
        item.setProductPrice(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }
}
