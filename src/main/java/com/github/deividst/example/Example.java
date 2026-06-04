package com.github.deividst.example;

import com.github.deividst.annotations.CnabField;
import com.github.deividst.enums.FieldType;

public class Example {

    @CnabField(start = 1, end = 3, type = FieldType.ALPHANUMERIC, defaultValue = " ")
    private String bank;

    @CnabField(start = 4, end = 7, type = FieldType.ALPHANUMERIC, defaultValue = " ")
    private String agency;

    @CnabField(start = 8, end = 19, type = FieldType.NUMERIC, defaultValue = "0")
    private String account;

    public String getBank() {
        return bank;
    }

    public String getAgency() {
        return agency;
    }

    public String getAccount() {
        return account;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
