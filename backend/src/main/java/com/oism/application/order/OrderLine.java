package com.oism.application.order;

import java.util.UUID;

/** Dong hang khi tao don: don gia lay tu gia ban le hien tai cua SKU va duoc chup vao don. */
public record OrderLine(UUID skuId, int quantity) {
}
