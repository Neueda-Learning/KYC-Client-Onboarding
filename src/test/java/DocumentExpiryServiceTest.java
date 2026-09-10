package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DocumentExpiryServiceTest {

    private final DocumentExpiryService documentExpiryService = new DocumentExpiryService();
    private final LocalDate today = LocalDate.of(2026, 1, 1);

    @ParameterizedTest
    @CsvSource({"30", "60", "90"})
    void documentExpiringExactlyAtThresholdIsFlagged(int thresholdDays) {
        LocalDate expiryDate = today.plusDays(thresholdDays);
        assertTrue(documentExpiryService.isExpiringWithin(expiryDate, today, thresholdDays));
    }

    @ParameterizedTest
    @CsvSource({"30", "60", "90"})
    void documentExpiringOneDayBeforeThresholdIsFlagged(int thresholdDays) {
        LocalDate expiryDate = today.plusDays(thresholdDays - 1);
        assertTrue(documentExpiryService.isExpiringWithin(expiryDate, today, thresholdDays));
    }

    @ParameterizedTest
    @CsvSource({"30", "60", "90"})
    void documentExpiringOneDayAfterThresholdIsNotFlagged(int thresholdDays) {
        LocalDate expiryDate = today.plusDays(thresholdDays + 1);
        assertFalse(documentExpiryService.isExpiringWithin(expiryDate, today, thresholdDays));
    }

    @Test
    void documentExpiringTodayIsFlagged() {
        assertTrue(documentExpiryService.isExpiringWithin(today, today, 30));
    }

    @Test
    void documentAlreadyExpiredIsNotFlaggedAsExpiring() {
        assertFalse(documentExpiryService.isExpiringWithin(today.minusDays(1), today, 30));
    }

    @Test
    void nullDatesThrow() {
        assertThrows(IllegalArgumentException.class, () -> documentExpiryService.isExpiringWithin(null, today, 30));
        assertThrows(IllegalArgumentException.class,
                () -> documentExpiryService.isExpiringWithin(today, null, 30));
    }
}
