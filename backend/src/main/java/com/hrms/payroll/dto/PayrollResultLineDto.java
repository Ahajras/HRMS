package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PayrollResultLineDto {
    private UUID id;
    private String componentCode;
    private String componentName;
    private String componentType;
    private BigDecimal quantity;
    private BigDecimal rate;
    private BigDecimal amount;
    private String source;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
