package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class LimitDetail {
    private String processingCode;
    private String operationName;
    private String currencyCode;
    private Double montantMinimum;
    private Double montantMaximum;
    private Double limiteJournaliere;
    private Double consommeAujourdhui;
    private Double restantAujourdhui;
    private Integer nombreTransactionsJour;
    private Double pourcentageUtilisationJour;
    private Double limitePeriode;
    private Double consommePeriode;
    private Double restantPeriode;
    private String typePeriode;
    private String debutPeriode;
    private String finPeriode;
    private String niveauAlerte;
    private String peutTransacter;
}
