package com.k1den.synccart_v20.network;

import com.k1den.synccart_v20.models.User;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @FormUrlEncoded
    @POST("api/auth/request-code")
    Call<Void> requestVerificationCode(
            @Field("email") String email,
            @Field("username") String username
    );

    @FormUrlEncoded
    @POST("api/auth/verify")
    Call<User> verifyCode(
            @Field("email") String email,
            @Field("code") String code
    );

    @FormUrlEncoded
    @POST("api/auth/verify")
    Call<User> verifyCode(
            @Field("email") String email,
            @Field("username") String username,
            @Field("code") String code,
            @Field("avatar_color") String userColor
    );

    @FormUrlEncoded
    @POST("api/chats/create")
    Call<com.k1den.synccart_v20.models.Chat> createChat(
            @Field("title") String title,
            @Field("userId") int userId
    );

    @GET("api/chats/my")
    Call<java.util.List<com.k1den.synccart_v20.models.Chat>> getMyChats(
            @Query("userId") int userId
    );

    @FormUrlEncoded
    @POST("api/messages/send")
    Call<com.k1den.synccart_v20.models.Message> sendMessage(
            @Field("chatId") int chatId,
            @Field("userId") int userId,
            @Field("content") String content
    );

    @GET("api/messages/{chatId}")
    Call<java.util.List<com.k1den.synccart_v20.models.Message>> getMessages(
            @Path("chatId") int chatId
    );


    @POST("api/lists/create")
    Call<com.k1den.synccart_v20.models.ShoppingList> createList(@Query("chatId") int chatId);

    @GET("api/lists/chat/{chatId}")
    Call<java.util.List<com.k1den.synccart_v20.models.ShoppingList>> getListsByChat(@Path("chatId") int chatId);

    @GET("api/lists/{listId}/items")
    Call<java.util.List<com.k1den.synccart_v20.models.ListItem>> getItems(@Path("listId") int listId);

    @PUT("api/lists/items/{itemId}/toggle")
    Call<com.k1den.synccart_v20.models.ListItem> toggleItemStatus(@Path("itemId") int itemId);

    @FormUrlEncoded
    @POST("api/lists/items/add")
    Call<com.k1den.synccart_v20.models.ListItem> addItem(
            @Field("listId") int listId,
            @Field("name") String name,
            @Field("category") String category
    );

    @POST("api/lists/create/standalone")
    Call<com.k1den.synccart_v20.models.ShoppingList> createStandaloneList(@Query("userId") int userId);

    @GET("api/lists/standalone/{userId}")
    Call<java.util.List<com.k1den.synccart_v20.models.ShoppingList>> getStandaloneLists(@Path("userId") int userId);

    @POST("api/chats/{chatId}/invite")
    Call<okhttp3.ResponseBody> inviteUser(
            @Path("chatId") int chatId,
            @Query("username") String username,
            @Query("chatTitle") String chatTitle
    );

    @DELETE("api/lists/items/{itemId}/delete")
    Call<Void> deleteItem(@Path("itemId") int itemId);

    @GET("api/chats/invitations/{userId}")
    Call<java.util.List<com.k1den.synccart_v20.models.ChatInvitation>> getInvites(@Path("userId") int userId);

    @POST("api/chats/invitations/{inviteId}/accept")
    Call<okhttp3.ResponseBody> acceptInvite(@Path("inviteId") int inviteId);

    @DELETE("api/chats/invitations/{inviteId}/decline")
    Call<Void> declineInvite(@Path("inviteId") int inviteId);

    @PUT("api/lists/items/{itemId}/assign")
    Call<com.k1den.synccart_v20.models.ListItem> assignItem(
            @Path("itemId") int itemId,
            @Query("assigneeName") String assigneeName
    );

    @DELETE("api/chats/{chatId}")
    Call<Void> deleteChat(@Path("chatId") int chatId);

    @DELETE("api/lists/{listId}")
    Call<Void> deleteList(@Path("listId") int listId);

    @POST("api/chat-ai/process-message")
    Call<okhttp3.ResponseBody> processAiMessage(
            @Query("chatId") int chatId,
            @Query("messageText") String messageText
    );
}
