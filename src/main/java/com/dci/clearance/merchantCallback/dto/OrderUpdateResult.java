package com.dci.clearance.merchantCallback.dto;

import com.dci.clearance.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateResult {
    private Order order;
    private BilleroConfirmResult confirmResult;
}
