package io.github.pedrodevsi.fleetagent.rental.application;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationExpirationSchedulerTest {

    @Test
    void shouldDelegateExpirationWithoutDependingOnAiFlow() {
        ReservationExpirationService expirationService = mock(ReservationExpirationService.class);
        ReservationExpirationScheduler scheduler = new ReservationExpirationScheduler(expirationService);
        when(expirationService.completeExpiredReservations()).thenReturn(2);

        scheduler.completeExpiredReservations();

        verify(expirationService).completeExpiredReservations();
    }
}
