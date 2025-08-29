package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bits.diamabankwalletf.utils.CustomerActivityId;

import java.sql.Timestamp;

@Entity
@Table(name = "CUSTOMER_ACTIVITY_TRACKING")
@Getter
@Setter
@IdClass(CustomerActivityId.class)
public class CustomerActivity {

    @Id
    @Column(name = "OP_TIMESTAMP", nullable = false)
    private Timestamp opTimestamp;

    @Id
    @Column(name = "USER_CODE", length = 15, nullable = false)
    private String userCode;

    @Column(name = "USER_IP", length = 32)
    private String userIp;

    @Column(name = "OP_LINK", length = 1024)
    private String opLink;

    @Column(name = "OP_DESC", length = 256)
    private String opDesc;

    @Column(name = "DEVICE_ID", length = 256)
    private String deviceId;

    @Column(name = "USER_AGENT", length = 512)
    private String userAgent;

}
