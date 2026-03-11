package com.example;

import java.math.BigDecimal;

public class Order {
    private final String item;
    private final int quantity;
    private final BigDecimal price;

    public Order(String item, int quantity, BigDecimal price) {
        this.item = item;
        this.quantity = quantity;
        this.price = price;
    }

    public String getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
