package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "ADVERTISEMENT")
@Data
public class Advertisement {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "BANK_CODE")
    private String bankCode;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IMAGE")
    @Lob
    private byte[] image;

    @Column(name = "LAST_UPDATE")
    private Date lastUpdate;
}
