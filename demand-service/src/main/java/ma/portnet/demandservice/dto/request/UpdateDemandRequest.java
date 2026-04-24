package ma.portnet.demandservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDemandRequest(

        @Size(min = 3, max = 3, message = "La devise doit être un code ISO 3 lettres")
        String devise,

        @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
        @Digits(integer = 16, fraction = 2)
        BigDecimal montantNominal,

        @Future(message = "La date d'échéance doit être dans le futur")
        LocalDate dateEcheance,

        @Size(max = 1000)
        String description

) {}