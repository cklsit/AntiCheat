package com.anticheat.web.dto;

import java.util.List;

/**
 * 案件实体。与前端 CaseEntity 对齐。
 */
public class CaseDTO {

    public String id;
    public String playerName;
    public String playerUuid;
    public String topModule;
    public int topScore;
    /** 0=LOW 1=MEDIUM 2=HIGH 3=EXTREME */
    public int riskLevel;
    public int evidenceCount;
    /** 自创建以来的小时数 */
    public long age;
    /** pending / reviewing / completed */
    public String status;
    public String createdAt;
    public String assignedTo;
    /** guilty / innocent / watched */
    public String verdict;
    public List<ModuleStatDTO> modules;
    public List<EvidenceSummaryDTO> evidenceSummary;

    public CaseDTO() {
    }
}
