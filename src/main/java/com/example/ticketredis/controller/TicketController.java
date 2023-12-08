package com.example.ticketredis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ticket")
public class TicketController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RequestMapping("/getTicket")
    public String getTicket() {
        return stringRedisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> String execute(RedisOperations<K, V> operations) throws DataAccessException {
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
                            return "fail";
                        }
                        operations.multi();
                        operations.opsForValue().decrement((K) "ticket");
                        List<Object> exec = operations.exec();
                        if (exec.isEmpty()) {
                            log.info("retry");
                            continue;
                        }
                        log.info("success! remaining = {}", exec);
                        return "success";
                    } catch (Exception e) {
                        log.error("error", e);
                        return "fail";
                    } finally {
                        operations.unwatch();
                    }
                }
            }
        });
    }
}
