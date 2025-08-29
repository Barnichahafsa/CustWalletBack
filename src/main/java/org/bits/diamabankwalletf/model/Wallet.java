package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.*;
import org.bits.diamabankwalletf.utils.WalletDataPK;

import java.util.Date;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "WALLET_DATA", uniqueConstraints = {
        @UniqueConstraint(name = "WALLET_DATA_PK", columnNames = {"WALLET_NUMBER", "SUPPLEMENTARY_DATA_1", "BANK_CODE"}),
        @UniqueConstraint(name = "WALLET_DATA_PK2", columnNames = {"WALLET_NUMBER", "SUPPLEMENTARY_DATA_1", "BANK_CODE"})
})
@IdClass(WalletDataPK.class)
public class Wallet {

    @Id
    @Column(name = "WALLET_NUMBER", nullable = false, length = 100)
    private String walletNumber;

    @Id
    @Column(name = "SUPPLEMENTARY_DATA_1", nullable = false, length = 200)
    private String supplementaryData1;

    @Id
    @Column(name = "BANK_CODE", nullable = false, length = 5)
    private String bankCode;

    @Column(name = "CARD_SEQUENCE", length = 4)
    private String cardSequence;

    @Column(name = "CLIENT_CODE", nullable = false, length = 30)
    private String clientCode;

    @Column(name = "BRANCH_CODE", length = 5)
    private String branchCode;

    @Column(name = "CLIENT_TYPE", length = 1)
    private Character clientType;

    @Column(name = "STATUS_WALLET", length = 1)
    private String statusWallet;

    @Column(name = "STATUS_DATE")
    @Temporal(TemporalType.DATE)
    private Date statusDate;

    @Column(name = "STATUS_REASON", length = 2)
    private String statusReason;

    @Column(name = "PRODUCT_CODE", length = 3)
    private String productCode;

    @Column(name = "WALLET_LEVEL", length = 1)
    private Character walletLevel;

    @Column(name = "PRIMARY_WALLET_NUMBER", length = 24)
    private String primaryWalletNumber;

    @Column(name = "REPLACEMENT_WALLET", length = 24)
    private String replacementWallet;

    @Column(name = "DATE_REPLACEMENT")
    @Temporal(TemporalType.DATE)
    private Date dateReplacement;

    @Column(name = "TITLE_CARDHOLDER", length = 12)
    private String titleCardholder;

    @Column(name = "GENDER", length = 1)
    private Character gender;

    @Column(name = "FAMILY_NAME", length = 26)
    private String familyName;

    @Column(name = "FIRST_NAME", length = 26)
    private String firstName;

    @Column(name = "MAIDEN_NAME", length = 26)
    private String maidenName;

    @Column(name = "EMBOSSED_NAME", length = 26)
    private String embossedName;

    @Column(name = "ENCODED_NAME", length = 26)
    private String encodedName;

    @Column(name = "BIRTH_DATE")
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Column(name = "BIRTY_CITY", length = 5)
    private String birthyCity;

    @Column(name = "BIRTH_COUNTRY", length = 3)
    private String birthCountry;

    @Column(name = "DOCUMENT_CODE", length = 2)
    private String documentCode;

    @Column(name = "DOCUMENT_ID", length = 30)
    private String documentId;

    @Column(name = "MOTHER_TONGUE", length = 3)
    private String motherTongue;

    @Column(name = "ADDRESS_1", length = 120)
    private String address1;

    @Column(name = "TYPE_ADDRESS_1", length = 1)
    private Character typeAddress1;

    @Column(name = "CITY_CODE_1", length = 5)
    private String cityCode1;

    @Column(name = "COUNTRY_CODE_1", length = 3)
    private String countryCode1;

    @Column(name = "ADRESS_2", length = 120)
    private String adress2;

    @Column(name = "TYPE_ADDRESS_2", length = 1)
    private Character typeAddress2;

    @Column(name = "CITY_CODE_2", length = 5)
    private String cityCode2;

    @Column(name = "COUNTRY_CODE_2", length = 3)
    private String countryCode2;

    @Column(name = "ADDRESS_3", length = 120)
    private String address3;

    @Column(name = "TYPE_ADDRESS_3", length = 1)
    private Character typeAddress3;

    @Column(name = "CITY_CODE_3", length = 5)
    private String cityCode3;

    @Column(name = "COUNTRY_CODE_3", length = 3)
    private String countryCode3;

    @Column(name = "MOBILE_NUMBER", length = 30)
    private String mobileNumber;

    @Column(name = "WORK_PHONE_NUMBER", length = 30)
    private String workPhoneNumber;

    @Column(name = "HOME_PHONE_NUMBER", length = 30)
    private String homePhoneNumber;

    @Column(name = "CODE_STAFF", length = 30)
    private String codeStaff;

    @Column(name = "NAME_COMPANY_STAFF", length = 60)
    private String nameCompanyStaff;

    @Column(name = "POSITION_STAFF", length = 30)
    private String positionStaff;

    @Column(name = "WALLET_AGREEMENT_DATE")
    @Temporal(TemporalType.DATE)
    private Date walletAgreementDate;

    @Column(name = "WALLET_CAPTURING_DATE")
    @Temporal(TemporalType.DATE)
    private Date walletCapturingDate;

    @Column(name = "EXPIRY_DATE")
    @Temporal(TemporalType.DATE)
    private Date expiryDate;

    @Column(name = "RENEWED_EXPIRY_DATE")
    @Temporal(TemporalType.DATE)
    private Date renewedExpiryDate;

    @Column(name = "LAST_BILLING_DATE")
    @Temporal(TemporalType.DATE)
    private Date lastBillingDate;

    @Column(name = "DELIVERY_RENEWED", length = 1)
    private Character deliveryRenewed;

    @Column(name = "DELIVERY_RENEWED_DATE")
    @Temporal(TemporalType.DATE)
    private Date deliveryRenewedDate;

    @Column(name = "DELIVERY_ACTION", length = 1)
    private Character deliveryAction;

    @Column(name = "DELIVERY_DATE")
    @Temporal(TemporalType.DATE)
    private Date deliveryDate;

    @Column(name = "ACTIVATION_ACTION", length = 1)
    private Character activationAction;

    @Column(name = "ACTIVATION_DATE")
    @Temporal(TemporalType.DATE)
    private Date activationDate;

    @Column(name = "RECORD_ORIGINE", length = 1)
    private String recordOrigine;

    @Column(name = "LAST_ACTION_CODE", length = 1)
    private Character lastActionCode;

    @Column(name = "LAST_ACTION_DATE")
    @Temporal(TemporalType.DATE)
    private Date lastActionDate;

    @Column(name = "LAST_ACTION_USER", length = 64)
    private String lastActionUser;

    @Column(name = "WALLET_ACCOUNTS", length = 1080)
    private String walletAccounts;

    @Column(name = "WALLET_SERVICES", length = 40)
    private String walletServices;

    @Column(name = "INDEX_LIMIT", length = 3)
    private String indexLimit;

    @Column(name = "PERIOD", length = 1)
    private Character period;

    @Column(name = "PERIOD_VALUE", length = 1)
    private Character periodValue;

    @Column(name = "SUPPLEMENTARY_DATA_2", length = 200)
    private String supplementaryData2;

    @Column(name = "DELIVERY_BRANCH", length = 5)
    private String deliveryBranch;

    @Column(name = "PIN_DELIVERY_FLAG", length = 1)
    private Character pinDeliveryFlag;

    @Column(name = "PIN_DELIVERY_DATE")
    @Temporal(TemporalType.DATE)
    private Date pinDeliveryDate;

    @Column(name = "WALLET_NBR_TRUNK", length = 22)
    private String walletNbrTrunk;

    @Column(name = "WALLET_PIN", length = 200)
    private String walletPin;

    @Column(name = "TITLE_WALLETHOLDER", length = 12)
    private String titleWalletholder;

    @Column(name = "WALLET_SEQUENCE", length = 4)
    private String walletSequence;

    @Column(name = "NATIONALITY", length = 20)
    private String nationality;

    @Column(name = "PHONE_NUMBER", length = 30)
    private String phoneNumber;

    @Column(name = "EMAIL_ADDRESS", length = 50)
    private String emailAddress;

    @Column(name = "BLOCK_ACTION", length = 1)
    private Character blockAction;

    @Column(name = "BLOCK_DATE")
    @Temporal(TemporalType.DATE)
    private Date blockDate;

    @Column(name = "ENTITY_ID", length = 50)
    private String entityId;

    @Column(name = "NUMBER_TRIED")
    private Integer numberTried;

    @Column(name = "SECRET_QUESTION", length = 2)
    private String secretQuestion;

    @Column(name = "ANSWER", length = 35)
    private String answer;

    @Column(name = "KYC_LEVEL", length = 1)
    private Character kycLevel;

    @Column(name = "NKIN_PHONE", length = 30)
    private String nkinPhone;

    @Column(name = "NKIN_NAME", length = 50)
    private String nkinName;

    @Column(name = "OCCUPATION", length = 100)
    private String occupation;

    @Column(name = "ESTIMATED_INCOME", length = 100)
    private String estimatedIncome;

    @Column(name = "SOURCE_OF_FUNDS", length = 100)
    private String sourceOfFunds;

    @Column(name = "POLITICAL_EXPOSED", length = 100)
    private String politicalExposed;

    @Column(name = "RISK_CATEGORY", length = 100)
    private String riskCategory;

    @Column(name = "EMPLOYMENT_TYPE", length = 20)
    private String employmentType;

    @Column(name = "PROFESSION", length = 100)
    private String profession;

    @Column(name = "PIN_EXPIRY_DATE")
    @Temporal(TemporalType.DATE)
    private Date pinExpiryDate;

    @Column(name = "PIN_LAST_CHANGED_DATE")
    @Temporal(TemporalType.DATE)
    private Date pinLastChangedDate;

    @Column(name = "PIN_CHANGE_REQUIRED", length = 1, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private Character pinChangeRequired = 'N';

    @Column(name = "PIN_EXPIRY_NOTIFICATION_SENT", length = 1, columnDefinition = "CHAR(1) DEFAULT 'N'")
    private Character pinExpiryNotificationSent = 'N';

}
