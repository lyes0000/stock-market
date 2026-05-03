package com.lyes0000.stock_market.model;

import lombok.Data;

@Data
public class TradeRequest {
    private String type; // 'buy' or 'sell'
}
