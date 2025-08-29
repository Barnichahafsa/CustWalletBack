package org.bits.diamabankwalletf.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "CUSTOMER_DWS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @Column(name = "CUSTOMER_ID")
    private String customerId;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "FP_STRING")
    private String fpString;

    @Lob
    @Column(name = "FP_IMAGE")
    private byte[] fpImage;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PHONE_NUMBER")
    private String phoneNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "BIRTHDATE")
    private Date birthdate;

    @Column(name = "NATIONALITY")
    private String nationality;

    @Column(name = "GENDER")
    private String gender;

    @Column(name = "DOCUMENT_CODE")
    private String documentCode;

    @Column(name = "DOCUMENT_TYPE")
    private String documentType;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "LAST_ACTION_USER")
    private String lastActionUser;

    @Lob
    @Column(name = "DOCUMENT_IMG")
    private byte[] documentImg;

    @Lob
    @Column(name = "FACE_ID")
    private byte[] faceId;

    @Temporal(TemporalType.DATE)
    @Column(name = "SIGNUP_DATE")
    private Date signupDate;

    @Lob
    @Column(name = "DOCUMENT_IMG_BACK")
    private byte[] documentImgBack;

    @Column(name = "BANK_CODE")
    private String bankCode;

    @Column(name = "BRANCH_CODE")
    private String branchCode;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "BLOCK_ACCESS")
    private String blockAccess;

    @Column(name = "NUMBER_OF_TRIES")
    private Integer numberOfTries;

    @Column(name = "NUMBER_OF_TRIES_ALLOWED")
    private Integer numberOfTriesAllowed;

    @Column(name = "FIRST_CONNECTION")
    private String firstConnection;

    @Column(name = "ENROLLED_FP")
    private String enrolledFp;

    @Column(name = "KYC_LEVEL")
    private String kycLevel;

    @Column(name = "ESTIMATED_INCOME")
    private String estimatedIncome;

}
