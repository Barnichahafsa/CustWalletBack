package org.bits.diamabankwalletf.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "NATIONALITY")
public class Nationality {
    @Id
    private String code;
    private String wording;
}
