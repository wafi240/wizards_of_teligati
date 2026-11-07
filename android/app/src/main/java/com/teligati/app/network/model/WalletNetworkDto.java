package com.teligati.app.network.model;

public class WalletNetworkDto {
    private Long id;
    private Long userId;
    private double availableBalance;
    private double escrowBalance;
    private String currency;
    private String updatedAt;
    private String disclaimer;
    private String demoNotice;

    public WalletNetworkDto() {}

    public WalletNetworkDto(Long id, Long userId, double availableBalance, double escrowBalance, String currency, String updatedAt, String disclaimer, String demoNotice) {
        this.id = id;
        this.userId = userId;
        this.availableBalance = availableBalance;
        this.escrowBalance = escrowBalance;
        this.currency = currency;
        this.updatedAt = updatedAt;
        this.disclaimer = disclaimer;
        this.demoNotice = demoNotice;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public double getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }
    public double getEscrowBalance() { return escrowBalance; }
    public void setEscrowBalance(double escrowBalance) { this.escrowBalance = escrowBalance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
    public String getDemoNotice() { return demoNotice; }
    public void setDemoNotice(String demoNotice) { this.demoNotice = demoNotice; }
}
