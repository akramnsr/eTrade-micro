package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.HistoriqueStatuts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriqueStatutsRepository extends JpaRepository<HistoriqueStatuts, String> {

    List<HistoriqueStatuts> findByDemande_DemandIdOrderByChangeDateAsc(String demandId);
}