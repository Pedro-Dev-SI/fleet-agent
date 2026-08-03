package com.br.langchain4j.notification.application;

import com.br.langchain4j.rental.api.event.ReservationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(ReservationNotificationListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ReservationCreatedEvent event) {
        logger.info("Reserva feita com o id: {}", event.reservationId());
    }
}
