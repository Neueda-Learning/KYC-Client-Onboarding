package service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the expiring-documents check once a day at 07:00 local time.
 */
public class DocumentExpiryScheduledJob {
    private static final Logger logger = LoggerFactory.getLogger(DocumentExpiryScheduledJob.class);
    private static final LocalTime RUN_TIME = LocalTime.of(7, 0);
    private static final int LOOKAHEAD_DAYS = 30;

    private final ClientService clientService;
    private final ScheduledExecutorService executor;

    public DocumentExpiryScheduledJob() {
        this(new ClientService(), Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "document-expiry-scheduler");
            thread.setDaemon(true);
            return thread;
        }));
    }

    public DocumentExpiryScheduledJob(ClientService clientService, ScheduledExecutorService executor) {
        this.clientService = clientService;
        this.executor = executor;
    }

    /**
     * Schedules the recurring daily run, starting at the next occurrence of 07:00.
     */
    public void start() {
        LocalDateTime nextRun = nextRunTime();
        long initialDelaySeconds = Duration.between(LocalDateTime.now(), nextRun).getSeconds();
        executor.scheduleAtFixedRate(this::runCheck, initialDelaySeconds, Duration.ofDays(1).getSeconds(),
                TimeUnit.SECONDS);
        logger.info("Document expiry check scheduled: runTime={} nextRun={} lookaheadDays={}",
                RUN_TIME, nextRun, LOOKAHEAD_DAYS);
    }

    private LocalDateTime nextRunTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayRun = LocalDateTime.of(now.toLocalDate(), RUN_TIME);
        return now.isBefore(todayRun) ? todayRun : todayRun.plusDays(1);
    }

    private void runCheck() {
        try {
            String json = clientService.listExpiringDocuments(LOOKAHEAD_DAYS);
            logger.info("Scheduled document expiry check completed: lookaheadDays={} result={}",
                    LOOKAHEAD_DAYS, json);
        } catch (Exception e) {
            logger.error("Scheduled document expiry check failed", e);
        }
    }
}
