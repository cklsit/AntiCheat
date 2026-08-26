package com.anticheat.web.dto;

/**
 * 案件证据摘要。与前端 EvidenceSummary 对齐。
 */
public class EvidenceSummaryDTO {

    public String type;
    public int count;
    public int peakScore;
    public String lastTime;

    public EvidenceSummaryDTO() {
    }

    public EvidenceSummaryDTO(String type, int count, int peakScore, String lastTime) {
        this.type = type;
        this.count = count;
        this.peakScore = peakScore;
        this.lastTime = lastTime;
    }
}
