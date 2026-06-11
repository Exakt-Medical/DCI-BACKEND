package com.dci.clearance.service;

import com.dci.clearance.entity.User;
import com.dci.clearance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled retry job for citizens whose Billeroo company sync is not yet complete.
 *
 * <ul>
 *   <li>Runs every 5 minutes</li>
 *   <li>Processes up to 10 unsynced users per run (batched to avoid thundering herd)</li>
 *   <li>Increments {@code billerooRetryCount} on each failed attempt</li>
 *   <li>Flips status to {@code DEAD} once retry count reaches 10</li>
 *   <li>Adds a 200ms delay between each sync call within a batch</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillerooSyncRetryJob {

    private static final int MAX_RETRY_COUNT = 10;
    private static final long INTER_CALL_DELAY_MS = 200;

    private final UserRepository userRepository;
    private final BillerooCompanySyncService billerooCompanySyncService;

    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void retryFailedBillerooSync() {
        List<User> unsynced = userRepository
                .findTop10ByBillerooSyncStatusInAndBillerooRetryCountLessThan(
                        List.of("PENDING", "FAILED"), MAX_RETRY_COUNT);

        if (unsynced.isEmpty()) return;

        log.info("Billeroo retry job: processing {} unsynced user(s)", unsynced.size());

        for (User user : unsynced) {
            try {
                String companyName = user.getFirstName() + " "
                        + user.getLastName() + " "
                        + user.getCompanyCode();
                billerooCompanySyncService.sync(
                        user.getCompanyCode(), user.getEmail(), companyName
                );
                user.setBillerooSyncStatus("SYNCED");
                log.info("Billeroo sync recovered for user id={}, code={}",
                        user.getId(), user.getCompanyCode());
            } catch (Exception e) {
                int newCount = user.getBillerooRetryCount() + 1;
                user.setBillerooRetryCount(newCount);
                if (newCount >= MAX_RETRY_COUNT) {
                    user.setBillerooSyncStatus("DEAD");
                    log.error("Billeroo sync permanently failed for user id={} after {} attempts. "
                                    + "Manual intervention required.",
                            user.getId(), newCount);
                } else {
                    log.warn("Billeroo retry {}/{} failed for user id={}: {}",
                            newCount, MAX_RETRY_COUNT, user.getId(), e.getMessage());
                }
            }
            userRepository.save(user);

            // Throttle between calls to avoid hammering Billeroo after a long outage
            try {
                Thread.sleep(INTER_CALL_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("Billeroo retry job interrupted");
                return;
            }
        }
    }
}
