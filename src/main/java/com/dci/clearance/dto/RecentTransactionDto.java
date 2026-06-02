// dto/RecentTransactionDto.java
package com.dci.clearance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentTransactionDto {
    private Long id;
    private String agent;
    private String company;
    private String dciAuthCode;
    private String plateNo;
    private String mvFile;
    private String chassisNo;
    private String engineNo;
    private String dateCreated;
}
