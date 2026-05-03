package com.lyes0000.stock_market.controller;

import com.lyes0000.stock_market.model.LogEntry;
import com.lyes0000.stock_market.model.StockEntry;
import com.lyes0000.stock_market.model.TradeRequest;
import com.lyes0000.stock_market.service.RedisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing all stock market endpoints.
 * This layer will handle only http (parse request and return reposnse)
 */
@RestController
public class StockMarketController {

    private final RedisService redisService;

    public StockMarketController(RedisService redisService) {
        this.redisService = redisService;
    }

    // --Bank

    /**
     * GET /stocks
     * Return the current state of the bank
     * 
     */
    @GetMapping("/stocks")
    public ResponseEntity<Map<String, List<StockEntry>>> getStocks() {
        List<StockEntry> stocks = redisService.getBankStocks();
        return ResponseEntity.ok(Map.of("stocks", stocks));
    }

    /**
     * POST /stocks
     * Repalces the bank state with provided stocks
     */
    @PostMapping("/stocks")
    public ResponseEntity<Void> setStocks(@RequestBody Map<String, List<StockEntry>> body) {
        List<StockEntry> stocks = body.get("stocks");
        redisService.setBankStocks(stocks);
        return ResponseEntity.ok().build();
    }

    // -- Wallet

    /**
     * GET /wallets/{wallet_id}
     * Returns all stocks in one wallet
     * We use LinkedHashMap to preserve the insertion order
     */
    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<Map<String, Object>> getWallet(@PathVariable String walletId) {
        List<StockEntry> stocks = redisService.getWalletStocks(walletId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", walletId);
        response.put("stocks", stocks);

        return ResponseEntity.ok(response);
    }

    /**
 * GET /wallets/{wallet_id}/stocks/{stock_name}
 * Returns the quantity of a specific stock in a wallet
 */
    @GetMapping("/wallets/{walletId}/stocks/{stockName}")
    public ResponseEntity<Integer> getWalletStock(
            @PathVariable String walletId,
            @PathVariable String stockName) {
        int quantity = redisService.getWalletStockQuantity(walletId, stockName);
        return ResponseEntity.ok(quantity);
    }

    /**
     * POST /wallets/{wallet_id}/stocks/{stock_name}
     * Buy or Sell of a single stock
     */
    @PostMapping("/wallets/{walletId}/stocks/{stockName}")
    public ResponseEntity<Void> trade(
            @PathVariable String walletId, 
            @PathVariable String stockName,
            @RequestBody TradeRequest request) {
        
        Integer bankQuantity = redisService.getBankStockQuantity(stockName);

        // Stock is not in the bank at all (stock doesn't exist)
        if (bankQuantity == null) {
            return ResponseEntity.notFound().build();
        }

        if ("buy".equals(request.getType())) {
            // Bank has none left for sale
            if (bankQuantity == 0) {
                return ResponseEntity.badRequest().build();
            }
            redisService.setBankStockQuantity(stockName, bankQuantity - 1 );
            int walletQuantity = redisService.getWalletStockQuantity(walletId, stockName);
            redisService.setWalletStockQuantity(walletId, stockName, walletQuantity + 1);
        }
        else if ("sell".equals(request.getType())) {
            int walletQuantity = redisService.getWalletStockQuantity(walletId, stockName);
            // if the stock is not in the wallet, can not sell
            if (walletQuantity == 0) {
                return ResponseEntity.badRequest().build();
            }
            redisService.setBankStockQuantity(stockName, bankQuantity + 1);
            redisService.setWalletStockQuantity(walletId, stockName, walletQuantity - 1);

        } else {
            // Invalid trade type
            return ResponseEntity.badRequest().build();
        }

        redisService.appendLog(new LogEntry(request.getType(), walletId, stockName));
        return ResponseEntity.ok().build();
    }
        // -- Audit Log

        /**
         * GET /log
         * Returns all audit log in order of occurence
         */
    @GetMapping("/log")
    public ResponseEntity<Map<String, List<LogEntry>>> getLog() {
        List<LogEntry> log = redisService.getLog();
        return ResponseEntity.ok(Map.of("log", log));
    }

    // --Chaos

    /**
     * POST /chaos
     * Kills this instance and leaves the others
     */
    @PostMapping("/chaos")
    public void chaos(){
        System.exit(0);
    }
}
