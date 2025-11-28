package org.bits.diamabankwalletf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "SECRET_QUESTION")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretQuestion {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "BANK_CODE")
    private String bankCode;

    @Column(name = "QUESTION")
    private String question;

    @Column(name = "STATUS")
    private Character status;
}
