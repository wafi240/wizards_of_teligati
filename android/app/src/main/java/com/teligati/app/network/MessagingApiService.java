package com.teligati.app.network;

import com.teligati.app.network.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface MessagingApiService {

    @GET("api/v1/conversations")
    Call<ApiResponse<PagedResponse<ConversationNetworkDto>>> getConversations(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @POST("api/v1/conversations")
    Call<ApiResponse<ConversationNetworkDto>> getOrCreateConversation(@Body ConversationCreateRequest request);

    @GET("api/v1/conversations/{id}")
    Call<ApiResponse<ConversationNetworkDto>> getConversationById(@Path("id") long id);

    @GET("api/v1/conversations/{id}/messages")
    Call<ApiResponse<PagedResponse<MessageNetworkDto>>> getConversationMessages(
            @Path("id") long conversationId,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @POST("api/v1/conversations/{id}/messages")
    Call<ApiResponse<MessageNetworkDto>> sendMessage(
            @Path("id") long conversationId,
            @Body MessageSendNetworkRequest request
    );

    @PATCH("api/v1/messages/{id}/read")
    Call<ApiResponse<MessageNetworkDto>> markMessageAsRead(@Path("id") long messageId);

    @PATCH("api/v1/conversations/{id}/read")
    Call<ApiResponse<Void>> markConversationAsRead(@Path("id") long conversationId);
}
