package com.teligati.backend.service;

import com.teligati.backend.dto.*;
import com.teligati.backend.entity.Conversation;
import com.teligati.backend.entity.Message;
import com.teligati.backend.entity.User;
import com.teligati.backend.exception.BadRequestException;
import com.teligati.backend.exception.ResourceNotFoundException;
import com.teligati.backend.repository.ConversationRepository;
import com.teligati.backend.repository.MessageRepository;
import com.teligati.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PlatformTransactionManager transactionManager;

    public ConversationResponse getOrCreateConversation(String principalName, ConversationCreateRequest request) {
        User sender = getUserByPrincipal(principalName);
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found with id: " + request.getRecipientId()));

        if (sender.getId().equals(recipient.getId())) {
            throw new BadRequestException("You cannot start a conversation with yourself");
        }

        // Canonical ordering to enforce single conversation per pair
        User p1 = sender.getId() < recipient.getId() ? sender : recipient;
        User p2 = sender.getId() < recipient.getId() ? recipient : sender;

        String lockKey = ("conv_pair_" + p1.getId() + "_" + p2.getId()).intern();
        synchronized (lockKey) {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            return transactionTemplate.execute(status -> doCreateOrGetConversation(sender, recipient, p1, p2, request));
        }
    }

    private ConversationResponse doCreateOrGetConversation(User sender, User recipient, User p1, User p2, ConversationCreateRequest request) {
        Conversation conversation = conversationRepository.findByParticipant1IdAndParticipant2Id(p1.getId(), p2.getId())
                .orElse(null);

        boolean isNew = false;
        if (conversation == null) {
            conversation = Conversation.builder()
                    .participant1(p1)
                    .participant2(p2)
                    .lastMessageAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            isNew = true;
        }

        if (request.getInitialMessage() != null && !request.getInitialMessage().trim().isEmpty()) {
            String initialContent = request.getInitialMessage().trim();
            conversation.setLastMessageText(initialContent);
            conversation.setLastMessageAt(LocalDateTime.now());
            conversation.setUpdatedAt(LocalDateTime.now());
            Conversation savedConv = conversationRepository.save(conversation);

            Message message = Message.builder()
                    .conversation(savedConv)
                    .sender(sender)
                    .recipient(recipient)
                    .content(initialContent)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            messageRepository.save(message);

            // Notify recipient
            String preview = initialContent.length() > 60 ? initialContent.substring(0, 57) + "..." : initialContent;
            notificationService.createNotification(
                    recipient,
                    "New Message from " + sender.getName(),
                    preview,
                    "NEW_MESSAGE",
                    savedConv.getId().toString()
            );

            return mapToConversationResponse(savedConv, sender.getId());
        }

        Conversation savedConv = isNew ? conversationRepository.save(conversation) : conversation;
        return mapToConversationResponse(savedConv, sender.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConversationResponse> getUserConversations(String principalName, int page, int size) {
        User user = getUserByPrincipal(principalName);
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> convPage = conversationRepository.findByUserIdOrderByLastMessageAtDesc(user.getId(), pageable);

        return PagedResponse.<ConversationResponse>builder()
                .content(convPage.getContent().stream().map(c -> mapToConversationResponse(c, user.getId())).toList())
                .page(convPage.getNumber())
                .size(convPage.getSize())
                .totalElements(convPage.getTotalElements())
                .totalPages(convPage.getTotalPages())
                .last(convPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversationById(Long id, String principalName) {
        User user = getUserByPrincipal(principalName);
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + id));

        if (!conversation.hasParticipant(user.getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Unauthorized: You are not a participant in this conversation");
        }

        return mapToConversationResponse(conversation, user.getId());
    }

    @Transactional(readOnly = true)
    public PagedResponse<MessageResponse> getConversationMessages(String principalName, Long conversationId, int page, int size) {
        User user = getUserByPrincipal(principalName);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!conversation.hasParticipant(user.getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Unauthorized: You are not a participant in this conversation");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);

        return PagedResponse.<MessageResponse>builder()
                .content(messagePage.getContent().stream().map(this::mapToMessageResponse).toList())
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .last(messagePage.isLast())
                .build();
    }

    @Transactional
    public MessageResponse sendMessage(String principalName, Long conversationId, MessageSendRequest request) {
        User sender = getUserByPrincipal(principalName);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!conversation.hasParticipant(sender.getId())) {
            throw new AccessDeniedException("Unauthorized: You cannot post messages to a conversation you are not participating in");
        }

        User recipient = conversation.getOtherParticipant(sender.getId());
        String content = request.getContent().trim();
        if (content.isEmpty()) {
            throw new BadRequestException("Message content cannot be blank");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .recipient(recipient)
                .content(content)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);

        // Update conversation summary
        conversation.setLastMessageText(content);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Notify recipient
        String preview = content.length() > 60 ? content.substring(0, 57) + "..." : content;
        notificationService.createNotification(
                recipient,
                "New Message from " + sender.getName(),
                preview,
                "NEW_MESSAGE",
                conversation.getId().toString()
        );

        return mapToMessageResponse(saved);
    }

    @Transactional
    public MessageResponse markMessageAsRead(String principalName, Long messageId) {
        User user = getUserByPrincipal(principalName);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));

        if (!message.getRecipient().getId().equals(user.getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Unauthorized: Only the message recipient can mark it as read");
        }

        if (!message.isRead()) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
            message = messageRepository.save(message);
        }

        return mapToMessageResponse(message);
    }

    @Transactional
    public void markConversationAsRead(String principalName, Long conversationId) {
        User user = getUserByPrincipal(principalName);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!conversation.hasParticipant(user.getId()) && !isAdmin(user)) {
            throw new AccessDeniedException("Unauthorized: You are not a participant in this conversation");
        }

        List<Message> unread = messageRepository.findUnreadInConversation(conversationId, user.getId());
        for (Message m : unread) {
            m.setRead(true);
            m.setReadAt(LocalDateTime.now());
        }
        if (!unread.isEmpty()) {
            messageRepository.saveAll(unread);
        }
    }

    private ConversationResponse mapToConversationResponse(Conversation c, Long currentUserId) {
        User other = c.getOtherParticipant(currentUserId);
        long unread = messageRepository.countUnreadMessages(c.getId(), currentUserId);

        return ConversationResponse.builder()
                .id(c.getId())
                .otherParticipantId(other != null ? other.getId() : null)
                .otherParticipantName(other != null ? other.getName() : "Unknown")
                .otherParticipantEmail(other != null ? other.getEmail() : null)
                .otherParticipantRole(other != null ? other.getRole() : null)
                .otherParticipantDept(other != null ? other.getDepartment() : null)
                .lastMessageText(c.getLastMessageText())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unread)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private MessageResponse mapToMessageResponse(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getName())
                .senderRole(m.getSender().getRole())
                .recipientId(m.getRecipient().getId())
                .recipientName(m.getRecipient().getName())
                .content(m.getContent())
                .isRead(m.isRead())
                .readAt(m.getReadAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private User getUserByPrincipal(String principalIdentifier) {
        return userRepository.findByIdentifier(principalIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for identifier: " + principalIdentifier));
    }

    private boolean isAdmin(User user) {
        String role = user.getRole();
        return "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }
}
