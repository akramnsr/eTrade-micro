package ma.portnet.demandservice.dto.request;

import jakarta.validation.constraints.*;
import ma.portnet.demandservice.entity.enums.CategorieRisquePays;
import ma.portnet.demandservice.entity.enums.TypeDecision;

public record DecisionRequest(

        @NotNull(message = "Le type de décision est obligatoire")
        TypeDecision decisionType,

        @Min(value = 0, message = "Le score banque doit être entre 0 et 100")
        @Max(value = 100, message = "Le score banque doit être entre 0 et 100")
        Integer bankRatingScore,

        @Min(value = 0)
        @Max(value = 100)
        Integer countryRiskScore,

        @Min(value = 0)
        @Max(value = 100)
        Integer documentaryRiskScore,

        CategorieRisquePays countryRiskCategory,

        @Size(max = 2000, message = "Les conditions ne doivent pas dépasser 2000 caractères")
        String conditions,

        @Size(max = 2000)
        String recommendation,

        // Obligatoire uniquement pour REJECT
        String reason

) {}