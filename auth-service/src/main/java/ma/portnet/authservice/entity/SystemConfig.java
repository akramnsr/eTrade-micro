package ma.portnet.authservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    private String id = "default";

    @Column(name = "default_rate_mad", precision = 5, scale = 2)
    private BigDecimal defaultRateMAD = BigDecimal.ZERO;

    @Column(name = "default_rate_eur", precision = 5, scale = 2)
    private BigDecimal defaultRateEUR = BigDecimal.ZERO;

    @Column(name = "default_rate_usd", precision = 5, scale = 2)
    private BigDecimal defaultRateUSD = BigDecimal.ZERO;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate = BigDecimal.ZERO;

    @Column(name = "fixed_fees", precision = 10, scale = 2)
    private BigDecimal fixedFees = BigDecimal.ZERO;

    @Column(name = "min_operation_amount", precision = 15, scale = 2)
    private BigDecimal minOperationAmount = BigDecimal.ZERO;

    @Column(name = "max_operation_amount", precision = 15, scale = 2)
    private BigDecimal maxOperationAmount = BigDecimal.ZERO;

    @Column(name = "analysis_deadline_days")
    private Integer analysisDeadlineDays = 5;

    @Column(name = "currencies_enabled", length = 100)
    private String currenciesEnabled = "MAD,EUR,USD";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // ── Getters / Setters ───────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getDefaultRateMAD() { return defaultRateMAD; }
    public void setDefaultRateMAD(BigDecimal v) { this.defaultRateMAD = v; }
    public BigDecimal getDefaultRateEUR() { return defaultRateEUR; }
    public void setDefaultRateEUR(BigDecimal v) { this.defaultRateEUR = v; }
    public BigDecimal getDefaultRateUSD() { return defaultRateUSD; }
    public void setDefaultRateUSD(BigDecimal v) { this.defaultRateUSD = v; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal v) { this.commissionRate = v; }
    public BigDecimal getFixedFees() { return fixedFees; }
    public void setFixedFees(BigDecimal v) { this.fixedFees = v; }
    public BigDecimal getMinOperationAmount() { return minOperationAmount; }
    public void setMinOperationAmount(BigDecimal v) { this.minOperationAmount = v; }
    public BigDecimal getMaxOperationAmount() { return maxOperationAmount; }
    public void setMaxOperationAmount(BigDecimal v) { this.maxOperationAmount = v; }
    public Integer getAnalysisDeadlineDays() { return analysisDeadlineDays; }
    public void setAnalysisDeadlineDays(Integer v) { this.analysisDeadlineDays = v; }
    public String getCurrenciesEnabled() { return currenciesEnabled; }
    public void setCurrenciesEnabled(String v) { this.currenciesEnabled = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
}