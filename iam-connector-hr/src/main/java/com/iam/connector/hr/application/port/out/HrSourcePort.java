package com.iam.connector.hr.application.port.out;

import com.iam.connector.hr.domain.model.HrRecord;
import java.util.List;

/**
 * Port to fetch raw records from the HR source system.
 */
public interface HrSourcePort {
    List<HrRecord> fetchAll();
}
