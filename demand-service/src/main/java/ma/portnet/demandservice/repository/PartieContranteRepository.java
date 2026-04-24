package ma.portnet.demandservice.repository;

import ma.portnet.demandservice.entity.PartieContrante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartieContranteRepository extends JpaRepository<PartieContrante, String> {

    List<PartieContrante> findByIsBankTrue();

    Optional<PartieContrante> findBySwiftCode(String swiftCode);

    Optional<PartieContrante> findByBankCode(String bankCode);
}