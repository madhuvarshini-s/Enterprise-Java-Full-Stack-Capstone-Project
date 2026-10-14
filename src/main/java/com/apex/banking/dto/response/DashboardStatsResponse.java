package com.apex.banking.dto.response;

import java.math.BigDecimal;

public class DashboardStatsResponse {

    private BigDecimal totalLiquidity;
    private long totalAccounts;
    private long activeAccounts;
    private long frozenAccounts;
    private long totalUsers;
    private BigDecimal totalVolume;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(BigDecimal totalLiquidity, long totalAccounts, long activeAccounts,
                                  long frozenAccounts, long totalUsers, BigDecimal totalVolume) {
        this.totalLiquidity = totalLiquidity;
        this.totalAccounts = totalAccounts;
        this.activeAccounts = activeAccounts;
        this.frozenAccounts = frozenAccounts;
        this.totalUsers = totalUsers;
        this.totalVolume = totalVolume;
    }

    public BigDecimal getTotalLiquidity() {
        return totalLiquidity;
    }

    public void setTotalLiquidity(BigDecimal totalLiquidity) {
        this.totalLiquidity = totalLiquidity;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public long getActiveAccounts() {
        return activeAccounts;
    }

    public void setActiveAccounts(long activeAccounts) {
        this.activeAccounts = activeAccounts;
    }

    public long getFrozenAccounts() {
        return frozenAccounts;
    }

    public void setFrozenAccounts(long frozenAccounts) {
        this.frozenAccounts = frozenAccounts;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }
}
