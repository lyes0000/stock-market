package com.lyes0000.stock_market.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockEntry {
    private String name;
    private int quantity;
}
