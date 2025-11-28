package org.bits.diamabankwalletf.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestInitiateDSD {

    private String numeroTelephone;
    private String categVehicule;
    private String numeroChassis;
    private String amount;
}

