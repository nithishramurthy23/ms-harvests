package com.nithishramurthy.farm.fresh.notification;

import com.nithishramurthy.farm.fresh.entity.Order;
import com.nithishramurthy.farm.fresh.entity.OrderItem;
import com.nithishramurthy.farm.fresh.entity.PaymentStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The words in each order email. Every email is built once as a list of
 * paragraphs and then written out twice, as plain text and as simple HTML, so
 * the two versions can't drift apart. Mail apps show the HTML one; the text one
 * is for those that can't, and for spam filters, which like having both.
 */
@Component
public class OrderEmailTemplates {

    private static final String GREEN = "#15803d";

    private final String shopUrl;

    public OrderEmailTemplates(@Value("${app.frontend-url:http://localhost:5173}") String shopUrl) {
        this.shopUrl = shopUrl.endsWith("/") ? shopUrl.substring(0, shopUrl.length() - 1) : shopUrl;
    }

    public OrderEmail build(Order order, OrderEmailType type) {
        String name = firstName(order.getUser().getName());
        String total = rupees(order.getTotalAmount());
        boolean cash = order.getPaymentStatus() == PaymentStatus.PAY_ON_DELIVERY;
        long id = order.getId();

        String subject;
        String heading;
        List<String> lines = new ArrayList<>();
        boolean showItems = true;

        switch (type) {
            case CONFIRMED -> {
                subject = "Order #" + id + " confirmed";
                heading = "Thanks, " + name + "! Your order is confirmed.";
                lines.add(cash
                        ? "Please keep " + total + " ready to pay in cash when it arrives."
                        : "We've received your payment of " + total + ".");
                lines.add("We'll email you again when it's on its way.");
            }
            case SHIPPED -> {
                subject = "Order #" + id + " is on its way";
                heading = "Your order is on its way, " + name + ".";
                lines.add("It's left the shop and should reach you soon.");
                if (cash) {
                    lines.add("Please keep " + total + " ready to pay in cash.");
                }
            }
            case DELIVERED -> {
                subject = "Order #" + id + " delivered";
                heading = "Delivered. Enjoy, " + name + "!";
                lines.add("If anything isn't right, just reply to this email and we'll sort it out.");
                showItems = false;
            }
            case CANCELLED -> {
                subject = "Order #" + id + " cancelled";
                heading = "Your order #" + id + " has been cancelled.";
                switch (order.getPaymentStatus()) {
                    case UNPAID -> lines.add("We didn't receive the payment in time, so we've released the items. "
                            + "Nothing was charged.");
                    case REFUND_PENDING, REFUNDED -> lines.add("We're refunding " + total
                            + ". It usually reaches your account in 5 to 7 working days. We'll email you when it's sent.");
                    default -> lines.add("Nothing was charged.");
                }
                lines.add("If you didn't expect this, reply to this email.");
                showItems = false;
            }
            case REFUNDED -> {
                subject = "Refund for order #" + id + " sent";
                heading = "We've sent your refund of " + total + ".";
                lines.add("Your bank usually shows it within 5 to 7 working days.");
                showItems = false;
            }
            default -> throw new IllegalArgumentException("Unknown email: " + type);
        }

        String ordersLink = shopUrl + "/orders";
        return new OrderEmail(order.getUser().getEmail(), "Farm Fresh: " + subject,
                text(heading, lines, showItems ? order : null, ordersLink),
                html(heading, lines, showItems ? order : null, ordersLink));
    }

    private static String text(String heading, List<String> lines, Order order, String link) {
        StringBuilder out = new StringBuilder(heading).append("\n\n");
        lines.forEach(line -> out.append(line).append("\n\n"));
        if (order != null) {
            out.append("Order #").append(order.getId()).append("\n");
            for (OrderItem item : order.getOrderItems()) {
                out.append("  ").append(item.getQuantity()).append(" x ").append(item.getProductName());
                if (item.getProductWeight() != null) out.append(" (").append(item.getProductWeight()).append(")");
                out.append("  ").append(rupees(lineTotal(item))).append("\n");
            }
            out.append("  Total  ").append(rupees(order.getTotalAmount())).append("\n\n");
            out.append("Delivering to: ").append(order.getShippingAddress()).append("\n\n");
        }
        out.append("Your orders: ").append(link).append("\n\n");
        out.append("Farm Fresh, from the farms around Ooty");
        return out.toString();
    }

    private static String html(String heading, List<String> lines, Order order, String link) {
        StringBuilder out = new StringBuilder()
                .append("<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;color:#3b2f2a\">")
                .append("<p style=\"color:").append(GREEN).append(";font-weight:bold;margin:0 0 16px\">Farm Fresh</p>")
                .append("<h1 style=\"font-size:22px;margin:0 0 16px\">").append(esc(heading)).append("</h1>");
        lines.forEach(line -> out.append("<p style=\"line-height:1.5\">").append(esc(line)).append("</p>"));
        if (order != null) {
            out.append("<table style=\"width:100%;border-collapse:collapse;margin:16px 0\">")
                    .append("<tr><th align=\"left\" colspan=\"2\" style=\"padding:8px 0;border-bottom:1px solid #ddd\">Order #")
                    .append(order.getId()).append("</th></tr>");
            for (OrderItem item : order.getOrderItems()) {
                String what = item.getQuantity() + " × " + item.getProductName()
                        + (item.getProductWeight() != null ? " (" + item.getProductWeight() + ")" : "");
                out.append("<tr><td style=\"padding:6px 0\">").append(esc(what)).append("</td>")
                        .append("<td align=\"right\">").append(esc(rupees(lineTotal(item)))).append("</td></tr>");
            }
            out.append("<tr><td style=\"padding:8px 0;border-top:1px solid #ddd\"><b>Total</b></td>")
                    .append("<td align=\"right\" style=\"border-top:1px solid #ddd\"><b>")
                    .append(esc(rupees(order.getTotalAmount()))).append("</b></td></tr></table>")
                    .append("<p style=\"color:#6b5f5a\">Delivering to: ").append(esc(order.getShippingAddress())).append("</p>");
        }
        out.append("<p style=\"margin:24px 0\"><a href=\"").append(esc(link))
                .append("\" style=\"background:").append(GREEN)
                .append(";color:#fff;padding:10px 18px;border-radius:999px;text-decoration:none\">See your orders</a></p>")
                .append("<p style=\"color:#8a7f7a;font-size:12px\">Farm Fresh, from the farms around Ooty</p></div>");
        return out.toString();
    }

    private static BigDecimal lineTotal(OrderItem item) {
        return item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    /** ₹1,249, ₹1,25,000 or ₹149.50: Indian digit grouping, and paise only when there are some. */
    static String rupees(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        String digits = rounded.toBigInteger().abs().toString();
        // last three digits, then groups of two: 12,34,567
        StringBuilder grouped = new StringBuilder();
        int firstGroupEnd = digits.length() - 3;
        if (firstGroupEnd <= 0) {
            grouped.append(digits);
        } else {
            String head = digits.substring(0, firstGroupEnd);
            for (int i = 0; i < head.length(); i++) {
                if (i > 0 && (head.length() - i) % 2 == 0) grouped.append(',');
                grouped.append(head.charAt(i));
            }
            grouped.append(',').append(digits.substring(firstGroupEnd));
        }
        int paise = rounded.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String sign = rounded.signum() < 0 ? "-" : "";
        return sign + "₹" + grouped + (paise == 0 ? "" : String.format(".%02d", paise));
    }

    private static String firstName(String name) {
        if (name == null || name.isBlank()) {
            return "there";
        }
        return name.trim().split("\\s+")[0];
    }

    /** Names and products come from users, so they're escaped before going into HTML. */
    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
