package com.sromantics.sromantics_api.makeup;

import com.sromantics.sromantics_api.entity.MakeUpCredit;
import lombok.Data;

@Data
public class MakeUpCreditRequest {
    private String targetClassId;
    private String targetDate;
    private MakeUpCredit.Status status;
    private String note;
}
