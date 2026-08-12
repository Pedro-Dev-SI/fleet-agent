package io.github.pedrodevsi.fleetagent.rental.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class ReservationExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpirationScheduler.class);

    private final ReservationExpirationService expirationService;

    ReservationExpirationScheduler(ReservationExpirationService expirationService) {
        this.expirationService = expirationService;
    }

    @Scheduled(fixedDelayString = "${app.rental.reservation-completion-interval:PT1M}")
    void completeExpiredReservations() {
        int completed = expirationService.completeExpiredReservations();
        if (completed > 0) {
            logger.info("Completed {} expired reservations", completed);
        }
    }
}
