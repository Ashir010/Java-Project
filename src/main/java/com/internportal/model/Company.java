package com.internportal.model;

public class Company {

    private int companyId;
    private String companyName;
    private String industry;
    private String website;
    private String contactEmail;

    public int getCompanyId() { return companyId; }
    public void setCompanyId(int companyId) { this.companyId = companyId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    /** Used by JComboBox to display the company. */
    @Override
    public String toString() {
        return companyName;
    }
}
