package ma.portnet.demandservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ma.portnet.demandservice.entity.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateDemandRequest(

        @NotBlank(message = "La devise est obligatoire")
        @Size(min = 3, max = 3, message = "La devise doit être un code ISO 3 lettres (ex: USD)")
        String devise,

        @NotNull(message = "Le montant nominal est obligatoire")
        @DecimalMin(value = "1.00", message = "Le montant doit être supérieur à 0")
        @Digits(integer = 16, fraction = 2, message = "Format montant invalide")
        BigDecimal montantNominal,

        @NotNull(message = "La date d'échéance est obligatoire")
        @Future(message = "La date d'échéance doit être dans le futur")
        LocalDate dateEcheance,

        @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
        String description,

        // ─── Type de produit (EBP ou EBD) ─────────────────────────────
        ProductType productType,

        // ─── Contexte commercial : LC_ACCEPTANCE, LC_DEFERRED_PAYMENT,
        //     DC_ACCEPTANCE_DA, OPEN_ACCOUNT ────────────────────────────
        @Size(max = 50)
        String billContext,

        // ─── Référence LC ou DC associée (optionnelle en Open Account) ─
        @Size(max = 50)
        String lcDcReference,

        // ─── Type d'achat / escompte : WITH_RECOURSE, WITHOUT_RECOURSE ─
        @Size(max = 30)
        String purchaseType,

        @NotEmpty(message = "Au moins un détail de traite est requis")
        @Valid
        List<DetailTraiteRequest> details

) {
    public record DetailTraiteRequest(
            @NotBlank(message = "Le numéro de traite est obligatoire")
            String draftNumber,

            @NotBlank(message = "Le pays de l'accepteur est obligatoire")
            @Size(min = 2, max = 3)
            String acceptorCountry,

            @NotBlank(message = "Le nom de l'accepteur est obligatoire")
            String acceptorName,

            String acceptorBank,

            @NotNull(message = "Le montant nominal de la traite est obligatoire")
            @DecimalMin(value = "1.00")
            BigDecimal nominalAmount,

            @NotBlank(message = "La devise de la traite est obligatoire")
            @Size(min = 3, max = 3)
            String currencyCode,

            @NotNull(message = "La date d'acceptation est obligatoire")
            LocalDate acceptanceDate,

            @NotNull(message = "La date d'échéance de la traite est obligatoire")
            @Future
            LocalDate maturityDate
    ) {}
}