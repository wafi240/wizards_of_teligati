package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.ConversationEntity;
import com.teligati.app.data.entity.MessageEntity;
import com.teligati.app.data.entity.NotificationEntity;
import com.teligati.app.data.entity.TransactionEntity;
import java.util.List;

@Dao
public interface CommunicationDao {
    // Conversations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertConversation(ConversationEntity conv);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversations(List<ConversationEntity> convs);

    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    List<ConversationEntity> getAllConversations();

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    ConversationEntity getConversationById(long id);

    // Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMessage(MessageEntity msg);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> msgs);

    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForConversation(long convId);

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :convId AND receiverId = :userId")
    void markConversationMessagesRead(long convId, String userId);

    @Query("UPDATE messages SET isRead = 1 WHERE id = :msgId")
    void markMessageRead(long msgId);

    @Query("SELECT * FROM messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp ASC")
    List<MessageEntity> getMessagesForUser(String userId);

    @Query("SELECT * FROM messages WHERE (senderId = :u1 AND receiverId = :u2) OR (senderId = :u2 AND receiverId = :u1) ORDER BY timestamp ASC")
    List<MessageEntity> getConversationBetween(String u1, String u2);

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    List<MessageEntity> getAllMessages();

    // Notifications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNotification(NotificationEntity notif);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotifications(List<NotificationEntity> notifs);

    @Query("SELECT * FROM notifications WHERE userId = :userId OR userId = 'ALL' ORDER BY timestamp DESC")
    List<NotificationEntity> getNotificationsForUser(String userId);

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    List<NotificationEntity> getAllNotifications();

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    void markNotificationAsRead(long id);

    // Transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransaction(TransactionEntity tx);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransactions(List<TransactionEntity> txs);

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY id DESC")
    List<TransactionEntity> getTransactionsForUser(String userId);

    @Query("SELECT * FROM transactions ORDER BY id DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    void updateTransactionStatus(String id, String status);
}
