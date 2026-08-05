package com.br.langchain4j.ai.tools;

import com.br.langchain4j.rental.api.CancelReservationRequest;
import com.br.langchain4j.rental.api.ReservationUseCase;
import com.br.langchain4j.rental.api.CreateReservationRequest;
import com.br.langchain4j.rental.api.ReservationCancelledResponse;
import com.br.langchain4j.rental.api.ReservationCreatedResponse;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ReservationTools {

    private final ReservationUseCase reservationUseCase;

    public ReservationTools(ReservationUseCase reservationUseCase) {
        this.reservationUseCase = reservationUseCase;
    }

    @Tool("Realiza uma nova reserva de um carro escolhido pelo cliente")
    public ReservationCreatedResponse createNewReservation(
            @ToolMemoryId UUID sessionId,
            @P("CPF do cliente") String document,
            @P("Data de retirada do veículo, início da locação") LocalDateTime startDate,
            @P("Data para a entrega do veículo, fim da locação") LocalDateTime endDate,
            @P("Modelo do carro escolhido") String carModel
    ){
        CreateReservationRequest reservationRequest = new CreateReservationRequest(
                sessionId,
                document,
                startDate,
                endDate,
                carModel
        );
        return reservationUseCase.createReservation(reservationRequest);
    }

    @Tool("Busca reserva no banco para retornar para o cliente quando ele precisar")
    public ReservationCreatedResponse reviewReservation(@P("CPF do cliente") String document){
        return reservationUseCase.findByCustomerDocument(document);
    }

    @Tool("Cancela uma reserva específica que pertence ao cliente informado")
    public ReservationCancelledResponse cancelReservation(
            @P("Identificador UUID da reserva") UUID reservationId,
            @P("CPF do cliente proprietário da reserva") String document
    ) {
        return reservationUseCase.cancelReservation(new CancelReservationRequest(reservationId, document));
    }
}
