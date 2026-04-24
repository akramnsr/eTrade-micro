package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.DetailTraite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetailTraiteRepository extends JpaRepository<DetailTraite, String> {

    List<DetailTraite> findByDemande_DemandId(String demandId);

    void deleteByDemande_DemandId(String demandId);
}