package vn.giadung.catalog;

import java.math.BigDecimal;

public record Product(int productId, String name, BigDecimal price, long availableQuantity) {}
