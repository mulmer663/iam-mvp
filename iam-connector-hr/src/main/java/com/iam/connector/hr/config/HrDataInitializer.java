package com.iam.connector.hr.config;

import com.iam.connector.hr.infrastructure.persistence.entity.HrRawData;
import com.iam.connector.hr.infrastructure.persistence.repository.HrRawDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes mock HR source data in the database.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HrDataInitializer implements CommandLineRunner {

    private final HrRawDataRepository hrRawDataRepository;

    @Override
    public void run(String... args) {
        if (hrRawDataRepository.count() > 0) {
            log.info("Mock HR data already exists. Skipping initialization.");
            return;
        }

        log.info("Initializing mock HR data...");

        List<HrRawData> mockData = List.of(
                HrRawData.builder()
                        .empNo("2023001")
                        .firstName("Gildong")
                        .lastName("Hong")
                        .position("Principal Engineer")
                        .deptCode("DEPT01")
                        .email("gildong.hong@example.com")
                        .status("ACTIVE")
                        .build(),
                HrRawData.builder()
                        .empNo("2023002")
                        .firstName("Chulsoo")
                        .lastName("Kim")
                        .position("Senior Engineer")
                        .deptCode("DEPT01")
                        .email("chulsoo.kim@example.com")
                        .status("ACTIVE")
                        .build(),
                HrRawData.builder()
                        .empNo("2023003")
                        .firstName("Younghee")
                        .lastName("Lee")
                        .position("Junior Planner")
                        .deptCode("DEPT02")
                        .email("younghee.lee@example.com")
                        .status("ACTIVE")
                        .build());

        hrRawDataRepository.saveAll(mockData);
        log.info("Mock HR data initialized with {} records.", mockData.size());
    }
}
