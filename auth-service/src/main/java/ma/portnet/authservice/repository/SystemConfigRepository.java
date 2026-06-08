package ma.portnet.authservice.repository;

import ma.portnet.authservice.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {}