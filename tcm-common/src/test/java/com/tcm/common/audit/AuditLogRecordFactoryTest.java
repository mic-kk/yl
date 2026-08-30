package com.tcm.common.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogRecordFactoryTest {

    @Test
    void build_shouldMaskPasswordAndFillFields() {
        Object[] args = new Object[]{"{\"username\":\"admin\",\"password\":\"secret123\"}"};
        AuditLogRecord record = AuditLogRecordFactory.build(
                new TestAuditLog(), args, 1001L, "admin", "127.0.0.1",
                "{\"code\":200}", 15L, true);

        assertEquals("user", record.getModule());
        assertEquals("login", record.getAction());
        assertEquals(1001L, record.getOperatorId());
        assertEquals("admin", record.getOperatorName());
        assertEquals("127.0.0.1", record.getRequestIp());
        assertFalse(record.getParamsSnapshot().contains("secret123"), "参数必须脱敏");
        assertEquals(15L, record.getCostTime());
        assertTrue(record.isSuccess());
    }

    @Test
    void build_shouldCaptureSnapshotWhenFirstArgImplementsProvider() {
        SnapshotDto dto = new SnapshotDto();
        dto.setOldStatus("PENDING");
        dto.setNewStatus("APPROVED");
        AuditLogRecord record = AuditLogRecordFactory.build(
                new TestAuditLog(), new Object[]{dto}, 1L, "u", null, null, 1L, true);

        assertEquals("PENDING", record.getBeforeSnapshot());
        assertEquals("APPROVED", record.getAfterSnapshot());
    }

    static class TestAuditLog implements AuditLog {
        @Override
        public Class<? extends java.lang.annotation.Annotation> annotationType() {
            return AuditLog.class;
        }

        @Override
        public String module() {
            return "user";
        }

        @Override
        public String action() {
            return "login";
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public boolean logResponse() {
            return true;
        }
    }

    static class SnapshotDto implements AuditLogRecord.SnapshotProvider {
        private String oldStatus;
        private String newStatus;

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        @Override
        public String provideBeforeSnapshot() {
            return oldStatus;
        }

        @Override
        public String provideAfterSnapshot() {
            return newStatus;
        }
    }
}
