package com.example.sistema_riesgos.data;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    @GET("forecast.json")
    Call<JsonObject> getForecastByCoords(
            @Query("key") String apiKey,
            @Query("q") String query,
            @Query("days") int days
    );
}
