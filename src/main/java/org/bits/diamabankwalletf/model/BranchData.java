package org.bits.diamabankwalletf.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BRANCH_DATA")
@Getter
@Setter
public class BranchData {
    @Id
    @Column(name = "ATM_ID")
    private String atmId;

    @Column(name = "ATM_NAME")
    private String atmName;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "ZONE")
    private String zone;

    @Column(name = "BRANCH")
    private String branch;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "GEO_LOCATION")
    private String geoLocation;

    @Column(name = "LATITUDE")
    private String latitude;

    @Column(name = "LONGITUDE")
    private String longitude;

    @Column(name = "WEB_ADDRESS")
    private String webAddress;
}
