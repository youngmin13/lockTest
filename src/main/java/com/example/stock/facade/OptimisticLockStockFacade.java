package com.example.stock.facade;

import com.example.stock.repository.StockRepository;
import com.example.stock.service.OptimisticLockStockService;
import org.springframework.stereotype.Component;

@Component
public class OptimisticLockStockFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    private final StockRepository stockRepository;

    public OptimisticLockStockFacade (OptimisticLockStockService optimisticLockStockService, StockRepository stockRepository) {
        this.optimisticLockStockService = optimisticLockStockService;
        this.stockRepository = stockRepository;
    }

    public void decrease (Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (Exception e) {
//                Thread.interrupted();
                Thread.sleep(100);
            }
        }
    }
}
