package org.bits.diamabankwalletf.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintRequest {
    private String userPhone;
    private String userName;
    private String complaintType;
    private String priority;
    private String title;
    private String description;
    private String incidentDate;
    private String transactionRef;
    private String contactMethod;
    private String alternateContact;
}
