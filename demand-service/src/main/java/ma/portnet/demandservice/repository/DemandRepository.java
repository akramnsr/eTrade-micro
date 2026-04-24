package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.DemandAchatTraite;
import ma.portnet.demandservice.entity.enums.StatusDemande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandRepository extends JpaRepository<DemandAchatTraite, String> {

    // Toutes les demandes d'un exportateur, du plus récent au plus ancien
    List<DemandAchatTraite> findByExporterIdOrderByCreatedDateDesc(String exporterId);

    // Toutes les demandes par statut
    List<DemandAchatTraite> findByStatusOrderByCreatedDateDesc(StatusDemande status);

    // Demandes d'un exportateur par statut
    List<DemandAchatTraite> findByExporterIdAndStatusOrderByCreatedDateDesc(
            String exporterId, StatusDemande status
    );

    // Vérifier unicité du numéro de demande
    boolean existsByRequestNumber(String requestNumber);

    // Trouver par numéro de demande
    Optional<DemandAchatTraite> findByRequestNumber(String requestNumber);

    // Compter par statut pour les stats du dashboard
    @Query("SELECT d.status, COUNT(d) FROM DemandAchatTraite d GROUP BY d.status")
    List<Object[]> countByStatus();

    // Toutes les demandes visibles par la banque (soumises et plus)
    @Query("""
        SELECT d FROM DemandAchatTraite d
        WHERE d.status NOT IN ('DRAFT')
        ORDER BY d.submittedDate DESC
    """)
    List<DemandAchatTraite> findAllVisibleToBank();

    // Demandes par banque exportateur assignée
    List<DemandAchatTraite> findByBankExporterIdOrderBySubmittedDateDesc(String bankExporterId);

    // Somme totale financée pour stats
    @Query("""
        SELECT COALESCE(SUM(df.montantNet), 0)
        FROM DetailsFinanciers df
        WHERE df.demande.status = 'FINANCED'
    """)
    java.math.BigDecimal sumMontantNetFinanced();
}