package com.example.ticketredis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@SpringBootTest
public class TicketTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Test
    void getTicket() {
        String ticket = stringRedisTemplate.opsForValue().get("ticket");
        log.info("ticket = {}", ticket);
    }

    @Test
    void sessionCallback() {
        ExecutorService executorService = Executors.newFixedThreadPool(15);
        for (int i = 0; i < 15; i++) {
            executorService.execute(() -> stringRedisTemplate.execute(new SessionCallback<Void>() {
                @Override
                public <K, V> Void execute(RedisOperations<K, V> operations) throws DataAccessException {
                    while (true) {
                        try {
                            operations.watch((K) "ticket");
                            V ticket = operations.opsForValue().get("ticket");
                            long ticketLong = 0;
                            if (ticket != null) {
                                ticketLong = Long.parseLong(ticket.toString());
                            }
                            if (ticketLong <= 0) {
                                log.info("no ticket!");
                                return null;
                            }
                            operations.multi();
                            operations.opsForValue().decrement((K) "ticket");
                            List<Object> exec = operations.exec();
                            if (exec.isEmpty()) {
                                continue;
                            }
                            log.info("success! remaining = {}", exec);
                        } catch (Exception e) {
                            log.error("error", e);
                        } finally {
                            operations.unwatch();
                        }
                        return null;
                    }
                }
            }));
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        executorService.shutdown();
    }

}