package com.example.stock.facade;

import static org.junit.jupiter.api.Assertions.*;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LettuceLockStockFacadeTest {

    @Autowired
    private LettuceLockStockFacade lettuceLockStockFacade;

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
                    lettuceLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 100 - (1 * 100) = 0
        assertEquals(0, stock.getQuantity());
        // race condition 발생 -> 하나의 쓰레드가 실행되기 전에 다른 쓰레드도 실행
        // synchronized -> 해당 메서드는 한 개의 쓰레드만 접근 할 수 있음 (문제 : 서버가 하나일 때만 되고 여러개면 여러개가 접근 가능함... 한 프로세스안에서만 되는 기능)
        // -> 하지만 @Transactional을 사용하게 되기 때문에
        // 종료 시점에 데이터베이스 업데이트를 하기 때문에
        // decrease 메서드가 완료되고 데이터베이스에 적용되기 전에 다른 쓰레드가 메서드에 접근 가능
    }
}