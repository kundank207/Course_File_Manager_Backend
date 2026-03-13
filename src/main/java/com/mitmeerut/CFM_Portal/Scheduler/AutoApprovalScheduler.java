package com.mitmeerut.CFM_Portal.Scheduler;

import com.mitmeerut.CFM_Portal.Service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AutoApprovalScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AutoApprovalScheduler.class);
    private final DocumentService documentService;

    @Autowired
    public AutoApprovalScheduler(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Professional Auto-Approval Task.
     * Runs every hour to check for documents that have exceeded the 48-hour review
     * deadline.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at the top of the hour
    public void runAutoApproval() {
        logger.info("Starting professional auto-approval process...");
        try {
            documentService.processAutoApprovals();
            logger.info("Auto-approval process completed successfully.");
        } catch (Exception e) {
            logger.error("Error during auto-approval process: {}", e.getMessage());
        }
    }
}
