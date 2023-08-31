package com.example.stock.facade;

import static org.junit.jupiter.api.Assertions.*;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import com.example.stock.service.PessimisticLockStockService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@SpringBootTest
class OptimisticLockStockFacadeTest {

    @Autowired
    private OptimisticLockStockFacade optimisticLockStockFacade;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before () {
        stockRepository.saveAndFlush(new Stock(1L, 1000L));
    }

    @AfterEach
    public void after () {
        stockRepository.deleteAll();
    }
    @Test
    public void 동시에_100개의_요청 () throws InterruptedException {
        int threadCount = 10000;
        // 비동기로 실행하는 작업을 단순화하여 사용할 수 있게 도와주는 자바 API (ExecutorService)
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        // 다른 쓰레드에서 수행하는 작업이 끝날 때까지 대기하게 도와주는 클래스
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    optimisticLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        System.out.println(stock.getQuantity());

        assertEquals(0, stock.getQuantity());
    }
}