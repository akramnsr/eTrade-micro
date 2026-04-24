package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partie_contrante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartieContrante {

    @Id
    @Column(name = "actor_id", length = 36)
    private String actorId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "country", length = 3)
    private String country;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "swift_code", length = 11)
    private String swiftCode;

    @Column(name = "is_bank")
    private Boolean isBank;

    public boolean isBank()     { return Boolean.TRUE.equals(isBank); }
}