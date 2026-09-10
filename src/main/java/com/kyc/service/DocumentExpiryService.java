package service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines whether a document's expiry date falls within a lookahead threshold.
 */
public class DocumentExpiryService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentExpiryService.class);

    /**
     * Checks whether a document is expiring within the given threshold window.
     * A document expiring in the past is not considered "expiring" (already expired).
     *
     * @param expiryDate document's expiry date
     * @param today reference date to compare against
     * @param thresholdDays lookahead window, in days, inclusive
     * @return true when 0 &lt;= (expiryDate - today) &lt;= thresholdDays
     */
    public boolean isExpiringWithin(LocalDate expiryDate, LocalDate today, int thresholdDays) {
        if (expiryDate == null || today == null) {
            throw new IllegalArgumentException("expiryDate and today must not be null");
        }
        long daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate);
        boolean expiring = daysUntilExpiry >= 0 && daysUntilExpiry <= thresholdDays;
        if (expiring) {
            logger.debug("Document expiring within threshold: expiryDate={} today={} thresholdDays={} daysUntilExpiry={}",
                    expiryDate, today, thresholdDays, daysUntilExpiry);
        }
        return expiring;
    }
}
