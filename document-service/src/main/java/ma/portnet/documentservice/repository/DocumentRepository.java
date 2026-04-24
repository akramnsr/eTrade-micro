package ma.portnet.documentservice.repository;

import ma.portnet.documentservice.entity.Document;
import ma.portnet.documentservice.entity.enums.TypeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByDemandIdOrderByUploadedDateDesc(String demandId);

    Optional<Document> findByDemandIdAndDocumentType(String demandId, TypeDocument type);

    List<Document> findByDemandIdAndMandatoryFlagTrue(String demandId);

    @Query("""
        SELECT COUNT(d) FROM Document d
        WHERE d.demandId = :demandId
        AND d.mandatoryFlag = true
        AND d.status <> 'REJECTED'
    """)
    long countValidMandatoryDocuments(@Param("demandId") String demandId);

    void deleteByDemandIdAndDocumentId(String demandId, String documentId);

    boolean existsByDemandIdAndDocumentType(String demandId, TypeDocument type);
}