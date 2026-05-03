package com.lyes0000.stock_market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lyes0000.stock_market.model.LogEntry;
import com.lyes0000.stock_market.model.StockEntry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class RedisService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    private static final String BANK_KEY = "bank:stocks";
    private static final String AUDIT_KEY = "audit:log";

    public RedisService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    // -- Bank

    public void setBankStocks(List<StockEntry> stocks) {
        redis.delete(BANK_KEY);
        for (StockEntry s : stocks) {
            redis.opsForHash().put(BANK_KEY, s.getName(), String.valueOf(s.getQuantity()));
        }
    }

    public List<StockEntry> getBankStocks() {
        Map<Object, Object> entries = redis.opsForHash().entries(BANK_KEY);
        List<StockEntry> result = new ArrayList<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            result.add(new StockEntry(e.getKey().toString(), Integer.parseInt(e.getValue().toString())));
        }
        return result;
    }

    public Integer getBankStockQuantity(String stockName) {
        Object val = redis.opsForHash().get(BANK_KEY, stockName);
        if (val == null) return null;
        return Integer.parseInt(val.toString());
    }

    public void setBankStockQuantity(String stockName, int quantity) {
        redis.opsForHash().put(BANK_KEY, stockName, String.valueOf(quantity));
    }

    // -- Wallet

    private String walletKey(String walletId) {
        return "wallet:" + walletId + ":stocks";
    }

    public List<StockEntry> getWalletStocks(String walletId) {
        Map<Object, Object> entries = redis.opsForHash().entries(walletKey(walletId));
        List<StockEntry> result = new ArrayList<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            result.add(new StockEntry(e.getKey().toString(), Integer.parseInt(e.getValue().toString())));
        }
        return result;
    }

    public Integer getWalletStockQuantity(String walletId, String stockName) {
        Object val = redis.opsForHash().get(walletKey(walletId), stockName);
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    public void setWalletStockQuantity(String walletId, String stockName, int quantity) {
        redis.opsForHash().put(walletKey(walletId), stockName, String.valueOf(quantity));
    }

    // -- Audit Log

    public void appendLog(LogEntry entry) {
        try {
            String json = objectMapper.writeValueAsString(entry);
            redis.opsForList().rightPush(AUDIT_KEY, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize log entry", e);
        }
    }

    public List<LogEntry> getLog() {
        List<String> raw = redis.opsForList().range(AUDIT_KEY, 0, -1);
        List<LogEntry> result = new ArrayList<>();
        if (raw == null) return result;
        for (String json : raw) {
            try {
                result.add(objectMapper.readValue(json, LogEntry.class));
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize log entry", e);
            }
        }
        return result;
    }
}