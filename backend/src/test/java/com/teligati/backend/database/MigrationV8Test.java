package com.teligati.backend.database;

import com.teligati.backend.dto.ConversationCreateRequest;
import com.teligati.backend.dto.ConversationResponse;
import com.teligati.backend.service.MessagingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class MigrationV8Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MessagingService messagingService;

    @Test
    @DisplayName("Migration V8: Verify conversations table schema integrity")
    void verifyConversationsTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'conversations'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "conversations must contain id");
        assertTrue(columnNames.contains("participant1_id"), "conversations must contain participant1_id");
        assertTrue(columnNames.contains("participant2_id"), "conversations must contain participant2_id");
        assertTrue(columnNames.contains("last_message_text"), "conversations must contain last_message_text");
        assertTrue(columnNames.contains("last_message_at"), "conversations must contain last_message_at");
        assertTrue(columnNames.contains("created_at"), "conversations must contain created_at");
        assertTrue(columnNames.contains("updated_at"), "conversations must contain updated_at");
    }

    @Test
    @DisplayName("Migration V8: Verify messages table schema integrity")
    void verifyMessagesTableSchemaIntegrity() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'messages'"
        );
        List<String> columnNames = columns.stream()
                .map(row -> row.get("COLUMN_NAME").toString().toLowerCase())
                .toList();

        assertTrue(columnNames.contains("id"), "messages must contain id");
        assertTrue(columnNames.contains("conversation_id"), "messages must contain conversation_id");
        assertTrue(columnNames.contains("sender_id"), "messages must contain sender_id");
        assertTrue(columnNames.contains("recipient_id"), "messages must contain recipient_id");
        assertTrue(columnNames.contains("content"), "messages must contain content");
        assertTrue(columnNames.contains("is_read"), "messages must contain is_read");
        assertTrue(columnNames.contains("read_at"), "messages must contain read_at");
        assertTrue(columnNames.contains("created_at"), "messages must contain created_at");
    }

    @Test
    @DisplayName("Migration V8: Verify participant ordering CHECK constraint (participant1_id < participant2_id)")
    void verifyParticipantOrderingConstraint() {
        List<Long> userIds = jdbcTemplate.queryForList("SELECT id FROM users ORDER BY id ASC LIMIT 2", Long.class);
        assertTrue(userIds.size() >= 2, "Must have at least 2 users for test");
        long p1 = userIds.get(0);
        long p2 = userIds.get(1);

        // Attempting to insert in inverted order (p2 as participant1_id, p1 as participant2_id where p2 > p1)
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO conversations (participant1_id, participant2_id, last_message_at, created_at, updated_at) " +
                            "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    p2, p1
            );
        }, "Inserting participant1_id > participant2_id must violate chk_participant_order");
    }

    @Test
    @DisplayName("Migration V8: Verify unique participant-pair constraint (uk_conversations_participants)")
    void verifyUniqueParticipantPairConstraint() {
        List<Long> userIds = jdbcTemplate.queryForList("SELECT id FROM users ORDER BY id ASC LIMIT 2", Long.class);
        long p1 = userIds.get(0);
        long p2 = userIds.get(1);

        // Delete any existing conversation between p1 and p2 for a clean test
        jdbcTemplate.update("DELETE FROM conversations WHERE participant1_id = ? AND participant2_id = ?", p1, p2);

        // First insert succeeds
        jdbcTemplate.update(
                "INSERT INTO conversations (participant1_id, participant2_id, last_message_at, created_at, updated_at) " +
                        "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                p1, p2
        );

        // Duplicate insert must throw unique constraint violation
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO conversations (participant1_id, participant2_id, last_message_at, created_at, updated_at) " +
                            "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    p1, p2
            );
        }, "Inserting duplicate (p1, p2) must violate uk_conversations_participants");
    }

    @Test
    @DisplayName("Migration V8: Verify foreign key constraints on conversations and messages")
    void verifyForeignKeyConstraints() {
        long nonExistentUserId = 9999999L;

        // Conversation with invalid user ID fails
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO conversations (participant1_id, participant2_id, last_message_at, created_at, updated_at) " +
                            "VALUES (1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    nonExistentUserId
            );
        }, "Inserting conversation with non-existent participant2_id must violate foreign key");

        // Message with invalid conversation ID fails
        long nonExistentConvId = 8888888L;
        assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO messages (conversation_id, sender_id, recipient_id, content, created_at) " +
                            "VALUES (?, 1, 2, 'Invalid conv test', CURRENT_TIMESTAMP)",
                    nonExistentConvId
            );
        }, "Inserting message with non-existent conversation_id must violate foreign key");
    }

    @Test
    @DisplayName("Migration V8: Verify existing database records remain intact after V8 migration")
    void verifyExistingRecordsIntact() {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertNotNull(userCount);
        assertTrue(userCount > 0, "Users table must retain records after V8 migration");

        Integer profileCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM profiles", Integer.class);
        assertNotNull(profileCount);
        assertTrue(profileCount > 0, "Profiles table must retain records");
    }

    @Test
    @DisplayName("Migration V8: Verify concurrent conversation creation creates only one canonical row")
    void verifyConcurrentConversationCreation() throws Exception {
        List<Long> userIds = jdbcTemplate.queryForList("SELECT id FROM users ORDER BY id ASC LIMIT 2", Long.class);
        long p1 = userIds.get(0);
        long p2 = userIds.get(1);

        String user1Email = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, p1);
        String user2Email = jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, p2);

        // Clean out conversation between p1 and p2 before test
        jdbcTemplate.update("DELETE FROM conversations WHERE participant1_id = ? AND participant2_id = ?", p1, p2);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<ConversationResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final boolean fromUser1 = (i % 2 == 0);
            futures.add(executor.submit(() -> {
                startLatch.await(); // ensure all threads fire at once
                if (fromUser1) {
                    return messagingService.getOrCreateConversation(user1Email, ConversationCreateRequest.builder().recipientId(p2).build());
                } else {
                    return messagingService.getOrCreateConversation(user2Email, ConversationCreateRequest.builder().recipientId(p1).build());
                }
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();

        List<Long> resultConvIds = Collections.synchronizedList(new ArrayList<>());
        for (Future<ConversationResponse> future : futures) {
            ConversationResponse resp = future.get(10, TimeUnit.SECONDS);
            assertNotNull(resp);
            resultConvIds.add(resp.getId());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Verify that all 10 concurrent threads received the exact same conversation ID
        long firstId = resultConvIds.get(0);
        for (Long id : resultConvIds) {
            assertEquals(firstId, id, "All concurrent callers must receive identical conversation ID");
        }

        // Verify that exactly 1 row exists in the database
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM conversations WHERE participant1_id = ? AND participant2_id = ?",
                Integer.class, p1, p2
        );
        assertEquals(1, rowCount, "Exactly one conversation row must exist in the database");
    }
}
