package com.example.cowin_booker;

public class VaccinationCenter {
    public String centerId;
    public String centerName;
    public int availableCap;
    public int availableCapD1;
    public int availableCapD2;
    public int minAgeLimit;
    public String vaccinationType;

    public VaccinationCenter() {

    }

    public VaccinationCenter(String centerId, String centerName, int availableCap, int availableCapD1, int availableCapD2, int minAgeLimit, String vaccinationType) {
        this.centerId = centerId;
        this.centerName = centerName;
        this.availableCap = availableCap;
        this.availableCapD1 = availableCapD1;
        this.availableCapD2 = availableCapD2;
        this.minAgeLimit = minAgeLimit;
        this.vaccinationType = vaccinationType;
    }

    public String getCenterId() {
        return centerId;
    }

    public void setCenterId(String centerId) {
        this.centerId = centerId;
    }

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public int getAvailableCap() {
        return availableCap;
    }

    public void setAvailableCap(int availableCap) {
        this.availableCap = availableCap;
    }

    public int getAvailableCapD1() {
        return availableCapD1;
    }

    public void setAvailableCapD1(int availableCapD1) {
        this.availableCapD1 = availableCapD1;
    }

    public int getAvailableCapD2() {
        return availableCapD2;
    }

    public void setAvailableCapD2(int availableCapD2) {
        this.availableCapD2 = availableCapD2;
    }

    public int getMinAgeLimit() {
        return minAgeLimit;
    }

    public void setMinAgeLimit(int minAgeLimit) {
        this.minAgeLimit = minAgeLimit;
    }

    public String getVaccinationType() {
        return vaccinationType;
    }

    public void setVaccinationType(String vaccinationType) {
        this.vaccinationType = vaccinationType;
    }
}
