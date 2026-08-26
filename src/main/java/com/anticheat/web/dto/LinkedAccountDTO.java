package com.anticheat.web.dto;

/**
 * 关联账号。与前端 LinkedAccount 对齐。
 */
public class LinkedAccountDTO {

    public String uuid;
    public String name;
    public String relation;
    public boolean ipMatch;
    public boolean hardwareMatch;

    public LinkedAccountDTO() {
    }

    public LinkedAccountDTO(String uuid, String name, String relation, boolean ipMatch, boolean hardwareMatch) {
        this.uuid = uuid;
        this.name = name;
        this.relation = relation;
        this.ipMatch = ipMatch;
        this.hardwareMatch = hardwareMatch;
    }
}
