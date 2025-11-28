package org.bits.diamabankwalletf.dto;

import lombok.Data;

@Data
public class LimitSummary {
    private String numeroPortefeuille;
    private String numeroMobile;
    private String nomComplet;
    private String produit;
    private String typeClient;
    private String statut;
    private String niveauKyc;
    private String profilLimite;
    private String nomProfil;
    private Double totalConsommeJour;
    private Integer totalTransactionsJour;
    private Double totalConsommePeriode;
    private String typePeriode;
    private String debutPeriode;
    private String finPeriode;
    private String derniereTransaction;
    private Double soldeDisponible;
    private String devise;
}
