// demand-service/.../repository/DocumentReferenceRepository.java
package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.DocumentReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentReferenceRepository extends JpaRepository<DocumentReference, String> {

    List<DocumentReference> findByDemande_DemandId(String demandId);

    Optional<DocumentReference> findByDemande_DemandIdAndDocumentType(String demandId, String documentType);

    void deleteByDemande_DemandIdAndDocumentId(String demandId, String documentId);

    long countByDemande_DemandIdAndMandatoryTrue(String demandId);
}