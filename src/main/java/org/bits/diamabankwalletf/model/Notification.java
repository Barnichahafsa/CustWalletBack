package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "NOTIFICATION")
@Data
public class Notification {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "title")
    private String title;

    @Column(name = "body")
    private String body;
}
