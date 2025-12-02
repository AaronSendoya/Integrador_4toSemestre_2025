package com.example.sistema_riesgos.data;

import com.example.sistema_riesgos.models.ReportModel;
import com.example.sistema_riesgos.models.RiskAnalysisResponse;
import com.example.sistema_riesgos.models.UserModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface RiskGuardApi {

    // --------- IA ---------
    @Multipart
    @POST("analyze-risk")
    Call<RiskAnalysisResponse> analyzeRiskImage(@Part MultipartBody.Part image);

    // --------- REGISTROS (REPORTES) ---------

    // CREATE
    @POST("registros")
    Call<ReportModel> createReport(@Body ReportModel report);

    // READ - todos
    @GET("registros")
    Call<List<ReportModel>> getAllReports();

    // READ - por usuario
    @GET("registros/usuario/{userId}")
    Call<List<ReportModel>> getReportsByUser(@Path("userId") String userId);

    // UPDATE
    @PUT("registros/{id}")
    Call<ReportModel> updateReport(
            @Path("id") String id,
            @Body ReportModel report
    );

    // DELETE
    @DELETE("registros/{id}")
    Call<Void> deleteReport(@Path("id") String id);

    // --------- USUARIOS ---------

    @POST("usuarios")
    Call<UserModel> createUser(@Body UserModel user);

    @GET("usuarios")
    Call<List<UserModel>> getAllUsers();

    // LOGIN: POST /api/usuarios/login
    @POST("usuarios/login")
    Call<UserModel> login(@Body UserModel credenciales);
}
