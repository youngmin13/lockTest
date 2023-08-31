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
class RedissonLockStockFacadeTest {
    @Autowired
    private RedissonLockStockFacade redissonLockStockFacade;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void before () {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void after () {
        stockRepository.deleteAll();
    }

    @Test
    public void 동시에_100개의_요청 () throws InterruptedException {
        // 단순히 몇번 실행할지 나타내는 쓰레드 -> 쓰레드 갯수 지정한 줄 알았는데... 아니였음
        int threadCount = 100;
        // 비동기로 실행하는 작업을 단순화하여 사용할 수 있게 도와주는 자바 API (ExecutorService)
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        //newFixedThreadPool(int nThreads)
        //초기 스레드 개수는 0개 ,코어 스레드 수와 최대 스레드 수는 매개변수 nThreads 값으로 지정,
        //이 스레드 풀은 스레드 개수보다 작업 개수가 많으면 마찬가지로 스레드를 새로 생성하여 작업을 처리한다.
        //만약 일 없이 놀고 있어도 스레드를 제거하지 않고 내비둔다.

        // 다른 쓰레드에서 수행하는 작업이 끝날 때까지 대기하게 도와주는 클래스
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockStockFacade.decrease(1L, 1L);
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