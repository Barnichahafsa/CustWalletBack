package org.bits.diamabankwalletf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "EPS_PROFILE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EpsProfile {
    @Id
    @Column(name = "CODE")
    private String code;
    @Column(name = "VALUE")
    private String value;
    @Column(name = "DESCRIPTION")
    private String description;
}
