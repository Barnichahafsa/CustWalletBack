package org.bits.diamabankwalletf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@Entity
@Table(name="EPS_SMS_LOG")
@Data
@AllArgsConstructor
@NoArgsConstructor


public class SmsLog {
    @Id
    @Column(name="SMS_TS")
    private Timestamp timestamp;
    @Column(name="MSISDN" , length=64)	//22
    private String msisdn;
    @Column(name="SENDER_MODULE" , length=64)
    private String senderModule;
    @Column(name="SMS_TEXT" , length=2000)
    private String smsText;
    @Column(name="SMS_STATUS" , length=1 )
    private Character smsStatus;
    @Column(name="SEND_TS")
    private Timestamp sendTimestamp;
    @Column(name="API_RESP_CODE" , length=64 )
    private String apiRespCode;
    @Column(name="API_RESP_MSG" , length=256 )
    private Date apiRespMsg;

}
