package com.foresee.watchdog.applicationmonitor.story;

import com.foresee.api_automation.tests.microservices.hierarchy_service.HierarchyLeavesTest;
import com.foresee.api_automation.tests.microservices.leaderboard_service.LeaderboardTest;
import com.foresee.api_automation.tests.microservices.platform_scim_service.SCIMGetControllerTest;
import com.foresee.api_automation.tests.microservices.ppt_export_service.PDFExportControllerTest;
import com.foresee.api_automation.tests.microservices.settings_service.test_cases.SettingsServiceTests;
import com.foresee.watchdog.applicationmonitor.service.QAMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
public class Mixed {
    private final QAMonitor QAMonitor;

    @PostConstruct
    public void postConstruct() {
        HierarchyLeavesTest leavesTest = new HierarchyLeavesTest();
        QAMonitor.register("Hierarchy_Leaves", leavesTest::testLeavesConditionGroup_8207581);
        LeaderboardTest leaderboardTest = new LeaderboardTest();
        QAMonitor.register("Leaderboard", () -> {
            try {
                leaderboardTest.testLeaderboardValidLevel_8643260();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
//        MasterDataControllerTest masterDataControllerTest = new MasterDataControllerTest();
//        monitor.register("Master_data", () -> {
//            try {
//                masterDataControllerTest.doBeforeTest();
//                masterDataControllerTest.testGetMasterDataRecords_12571595();
//                masterDataControllerTest.doAfterTest();
//            } catch (Exception e) {
//                throw new AssertionError(e);
//            }
//        });
        SCIMGetControllerTest scimGetControllerTest = new SCIMGetControllerTest();
        QAMonitor.register("Auth_scim", scimGetControllerTest::testSCIMGetUserByInternalId_8667538);
        PDFExportControllerTest pdfExportControllerTest = new PDFExportControllerTest();
        QAMonitor.register("Pdf_export", () -> {
            try {
                pdfExportControllerTest.testPOSTGeneratePDFRequest200_16122759();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        SettingsServiceTests settingsServiceTests = new SettingsServiceTests();
        QAMonitor.register("settings", () -> {
            try {
                settingsServiceTests.testGETWhiteListUrl200_21989149();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });

    }
}
