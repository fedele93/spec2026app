package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Errore leggibile da mostrare all'utente (es. "Posti non sufficienti", "Server non raggiungibile"). */
class RemoteException(message: String, val statusCode: Int = 0) : Exception(message)

object RemoteClient {
    val moshi: Moshi = Moshi.Builder().build()

    /**
     * Crea il client verso il backend.
     * @param baseUrl es. https://festa.tuodominio.it
     * @param clientId identificativo del dispositivo (X-Client-Id)
     * @param adminToken token organizzatore, opzionale (X-Admin-Token)
     */
    fun create(baseUrl: String, clientId: String, adminToken: String, debugLogging: Boolean = false): NeuroPartyApi {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Client-Id", clientId)
                    .apply { if (adminToken.isNotBlank()) header("X-Admin-Token", adminToken) }
                    .build()
                chain.proceed(request)
            }
        if (debugLogging) {
            builder.addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        return Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(builder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NeuroPartyApi::class.java)
    }

    /** Converte le eccezioni di rete/HTTP in un messaggio comprensibile. */
    fun toRemoteException(e: Throwable): RemoteException = when (e) {
        is RemoteException -> e
        is HttpException -> RemoteException(messageFromErrorBody(e.response()?.errorBody()?.string(), e.code()), e.code())
        is IOException -> RemoteException("Server non raggiungibile: controlla la connessione")
        else -> RemoteException(e.message ?: "Errore imprevisto")
    }

    private val mapAdapter by lazy {
        moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
    }

    /** Estrae il campo "detail" delle risposte di errore di FastAPI (stringa o lista di errori di validazione). */
    fun messageFromErrorBody(body: String?, code: Int): String {
        if (body.isNullOrBlank()) return "Errore del server ($code)"
        return try {
            when (val detail = mapAdapter.fromJson(body)?.get("detail")) {
                is String -> detail
                is List<*> -> (detail.firstOrNull() as? Map<*, *>)?.get("msg")?.toString()
                    ?.removePrefix("Value error, ")
                    ?: "Dati non validi"
                else -> "Errore del server ($code)"
            }
        } catch (_: Exception) {
            "Errore del server ($code)"
        }
    }
}
