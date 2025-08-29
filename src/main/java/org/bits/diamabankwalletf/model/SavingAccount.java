package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bits.diamabankwalletf.utils.SavingAccountId;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@IdClass(SavingAccountId.class)
@Table(name = "SAVING_ACCOUNT")
public class SavingAccount {

    @Id
    @Column(name = "WALLET_NUMBER", length = 100)
    private String walletNumber;

    @Id
    @Column(name = "BANK_CODE", length = 5)
    private String bankCode;

    @Id
    @Column(name = "BRANCH_CODE", length = 5)
    private String branchCode;

    @Column(name = "STATUS_ACCOUNT")
    private Character statusAccount;

    @Temporal(TemporalType.DATE)
    @Column(name = "STATUS_DATE")
    private Date statusDate;

    @Column(name = "STATUS_REASON", length = 2)
    private String statusReason;

    @Column(name = "ACTIVATION_ACTION")
    private Character activationAction;

    @Temporal(TemporalType.DATE)
    @Column(name = "ACTIVATION_DATE")
    private Date activationDate;

    @Column(name = "LAST_ACTION_CODE")
    private Character lastActionCode;

    @Temporal(TemporalType.DATE)
    @Column(name = "LAST_ACTION_DATE")
    private Date lastActionDate;

    @Column(name = "LAST_ACTION_USER", length = 64)
    private String lastActionUser;

    @Column(name = "ACCOUNT_NUMBER", length = 1080)
    private String accountNumber;

    @Column(name = "MEMO", length = 400)
    private String memo;

    @Column(name = "ENTITY_ID", length = 50)
    private String entityId;

}
