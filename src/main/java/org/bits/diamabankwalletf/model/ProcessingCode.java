package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "PROCESSING") // Exactly as in Queries
@Data
public class ProcessingCode {

    @Id
    @Column(name = "PROCESSING_CODE")
    private String code;

    @Column(name = "WORDING")
    private String wording;
}
