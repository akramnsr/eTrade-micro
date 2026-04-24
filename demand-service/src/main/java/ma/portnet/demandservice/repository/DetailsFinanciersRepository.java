package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.DetailsFinanciers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DetailsFinanciersRepository extends JpaRepository<DetailsFinanciers, String> {

    Optional<DetailsFinanciers> findByDemande_DemandId(String demandId);
}