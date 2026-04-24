package ma.portnet.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.portnet.authservice.entity.enums.RoleUtilisateur;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    private String email;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleUtilisateur role;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "company_number", length = 50)
    private String companyNumber;

    @Column(name = "keycloak_id", length = 36, unique = true)
    private String keycloakId;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (isActive    == null) isActive    = true;
    }
}