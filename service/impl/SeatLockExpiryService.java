package com.shivam.bookMyShow.service.impl;

import com.shivam.bookMyShow.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockExpiryService {

    private final ShowSeatRepository showSeatRepository;

    // Runs every minute — releases locked seats older than 10 minutes
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime expiry = LocalDateTime.now().minusMinutes(10);
        showSeatRepository.releaseExpiredLocks(expiry);
        log.info("Released expired seat locks older than 10 minutes");
    }
}
