package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService (StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    // synchronized -> 해당 메서드는 한 개의 쓰레드만 접근 할 수 있음
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decrease (Long id, Long quantity) {
        // Stock 조회
        // 재고 감소
        // 갱신된 값을 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
