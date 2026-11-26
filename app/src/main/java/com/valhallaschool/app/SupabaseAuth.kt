package com.valhallaschool.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Autenticación contra Supabase Auth (GoTrue) con solo HttpURLConnection
 * + org.json. Sin SDK de Supabase ni dependencias nuevas.
 *
 * Proyecto: Valhalla School — ver [SupabaseConfig].
 * La confirmación de correo está APAGADA en este proyecto, así que el
 * registro ([signUp]) devuelve sesión directa, igual que el login.
 *
 * La sesión (access_token + user id) se guarda en SharedPreferences
 * privadas de la app. El token es lo que [GameProgressSync] manda como
 * `Authorization: Bearer <token>` en cada llamada a PostgREST.
 *
 * Todas las llamadas son asíncronas (hilo propio) y devuelven el
 * resultado por callback ([Callback]): (sesión, error). Los callbacks
 * corren en un hilo de fondo: quien los reciba en la UI debe volver
 * al hilo principal (p. ej. `runOnUiThread { ... }`).
 *
 * Uso:
 * ```
 * SupabaseAuth.init(context)          // una vez, al arrancar la app
 * SupabaseAuth.signUp(email, pass) { session, error ->
 *     if (error == null) { /* ya hay sesión */ }
 * }
 * val session = SupabaseAuth.currentSession()  // null si no hay login
 * ```
 */
object SupabaseAuth {

    /** Sesión mínima que guarda la app. */
    data class Session(
        val accessToken: String,
        val userId: String,
        val email: String?
    )

    private const val PREFS_NAME = "valhalla_school_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"

    private lateinit var prefs: SharedPreferences

    /** Llamar una vez al arrancar la app, antes que cualquier otro método. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Sesión guardada, o null si no hay login (o se cerró sesión). */
    fun currentSession(): Session? {
        if (!::prefs.isInitialized) return null
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        return Session(token, userId, prefs.getString(KEY_EMAIL, null))
    }

    /** true si hay sesión guardada. No verifica que el token siga vigente. */
    fun isSignedIn(): Boolean = currentSession() != null

    /**
     * Registra un usuario nuevo. Como la confirmación de correo está OFF,
     * Supabase devuelve la sesión en la misma respuesta.
     * Si algún día se vuelve a encender la confirmación, la respuesta
     * vendrá sin `access_token` y esto devolverá un fallo explicándolo.
     */
    fun signUp(email: String, password: String, callback: Callback<Session>) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
        runRequest("POST", "${SupabaseConfig.SUPABASE_URL}/auth/v1/signup", body) { result ->
            val mapped = result.fold(
                onSuccess = { json -> parseAndStoreSession(json, email) },
                onFailure = { Result.failure(it) }
            )
            callback.onResult(mapped.getOrNull(), mapped.exceptionOrNull())
        }
    }

    /**
     * Inicia sesión con correo y contraseña.
     * Endpoint: POST /auth/v1/token?grant_type=password
     */
    fun signIn(email: String, password: String, callback: Callback<Session>) {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
        val url = "${SupabaseConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"
        runRequest("POST", url, body) { result ->
            val mapped = result.fold(
                onSuccess = { json -> parseAndStoreSession(json, email) },
                onFailure = { Result.failure(it) }
            )
            callback.onResult(mapped.getOrNull(), mapped.exceptionOrNull())
        }
    }

    /**
     * Cierra sesión en local: borra el token guardado. No llama al
     * servidor: el token expira solo y sin él la app ya no puede leer
     * nada (el RLS lo bloquea). Si en Fase 2 se quiere invalidar el
     * token en el servidor, añadir POST /auth/v1/logout con el token.
     */
    fun signOut() {
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    /** Extrae access_token + user.id de la respuesta JSON y los guarda. */
    private fun parseAndStoreSession(json: String, email: String): Result<Session> {
        return try {
            val obj = JSONObject(json)
            val token = obj.optString("access_token", "")
            if (token.isEmpty()) {
                // Registro sin sesión: pasa si la confirmación de correo
                // está encendida en Supabase. Hoy está OFF, así que esto
                // no debería ocurrir; si ocurre, el mensaje lo explica.
                return Result.failure(
                    IllegalStateException(
                        "Cuenta creada pero sin sesión: revisa la confirmación " +
                            "de correo en Supabase (o tu bandeja de entrada)."
                    )
                )
            }
            val userId = obj.getJSONObject("user").getString("id")
            val session = Session(token, userId, email)
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .apply()
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Respuesta de Auth no válida: ${e.message}", e))
        }
    }

    /**
     * POST genérico a Auth con las cabeceras que exige Supabase:
     * apikey + Authorization Bearer con la ANON KEY (clave pública).
     * Devuelve el cuerpo como String o un fallo con el mensaje del
     * servidor cuando el HTTP no es 2xx.
     */
    private fun runRequest(
        method: String,
        urlString: String,
        body: String?,
        callback: (Result<String>) -> Unit
    ) {
        Thread {
            try {
                val url = URL(urlString)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    setRequestProperty(
                        "Authorization",
                        "Bearer ${SupabaseConfig.SUPABASE_ANON_KEY}"
                    )
                    setRequestProperty("Content-Type", "application/json")
                    if (body != null) {
                        doOutput = true
                        outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                conn.disconnect()
                if (code in 200..299) {
                    callback(Result.success(response))
                } else {
                    callback(Result.failure(httpError(code, response)))
                }
            } catch (e: IOException) {
                // Sin internet, DNS caído, timeout, etc.
                callback(Result.failure(IOException("Sin conexión al servidor: ${e.message}", e)))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }.start()
    }

    /** Convierte un HTTP no-2xx en una excepción con mensaje legible. */
    private fun httpError(code: Int, body: String): Exception {
        val serverMsg = try {
            JSONObject(body).optString("msg",
                JSONObject(body).optString("message", ""))
        } catch (_: Exception) {
            ""
        }
        val detail = if (serverMsg.isNotEmpty()) " — $serverMsg" else ""
        return when (code) {
            400 -> IllegalArgumentException("Datos no válidos (HTTP 400)$detail")
            401 -> IllegalStateException("No autorizado (HTTP 401)$detail")
            403 -> IllegalStateException("Acceso denegado (HTTP 403)$detail")
            422 -> IllegalArgumentException("Correo o contraseña no válidos (HTTP 422)$detail")
            429 -> IllegalStateException("Demasiados intentos, espera un poco (HTTP 429)$detail")
            else -> IllegalStateException("Error del servidor (HTTP $code)$detail")
        }
    }
}
