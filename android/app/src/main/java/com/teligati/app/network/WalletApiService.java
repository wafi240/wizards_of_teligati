package com.teligati.app.network;

import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.EscrowCreateRequest;
import com.teligati.app.network.model.EscrowNetworkDto;
import com.teligati.app.network.model.TopUpRequest;
import com.teligati.app.network.model.TransactionNetworkDto;
import com.teligati.app.network.model.WalletNetworkDto;
import com.teligati.app.network.model.WithdrawRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface WalletApiService {

    @GET("api/v1/wallet")
    Call<ApiResponse<WalletNetworkDto>> getWallet();

    @POST("api/v1/wallet/topup")
    Call<ApiResponse<WalletNetworkDto>> topUp(@Body TopUpRequest request);

    @POST("api/v1/wallet/withdraw")
    Call<ApiResponse<WalletNetworkDto>> withdraw(@Body WithdrawRequest request);

    @GET("api/v1/wallet/transactions")
    Call<ApiResponse<List<TransactionNetworkDto>>> getTransactions();

    @POST("api/v1/escrow/create")
    Call<ApiResponse<EscrowNetworkDto>> createEscrow(@Body EscrowCreateRequest request);

    @GET("api/v1/escrow")
    Call<ApiResponse<List<EscrowNetworkDto>>> getEscrows();

    @GET("api/v1/escrow/{id}")
    Call<ApiResponse<EscrowNetworkDto>> getEscrowById(@Path("id") long id);

    @POST("api/v1/escrow/{id}/progress")
    Call<ApiResponse<EscrowNetworkDto>> markInProgress(@Path("id") long id);

    @POST("api/v1/escrow/{id}/release")
    Call<ApiResponse<EscrowNetworkDto>> releaseEscrow(@Path("id") long id);

    @POST("api/v1/escrow/{id}/refund")
    Call<ApiResponse<EscrowNetworkDto>> refundEscrow(@Path("id") long id);

    @POST("api/v1/escrow/{id}/dispute")
    Call<ApiResponse<EscrowNetworkDto>> disputeEscrow(@Path("id") long id, @Body Map<String, String> body);
}
