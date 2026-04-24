package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.Decision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DecisionRepository extends JpaRepository<Decision, String> {

    Optional<Decision> findByDemande_DemandId(String demandId);
}