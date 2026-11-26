package com.valhallaschool.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Sincronización del progreso del juego con Supabase (PostgREST).
 *
 * Solo HttpURLConnection + org.json. Sin SDK de Supabase ni
 * dependencias nuevas. Las tablas están en `sql/01_tables.sql` y
 * protegidas por RLS: cada usuario solo toca sus filas, y el ranking
 * (`game_scores`) es legible por cualquier usuario autenticado.
 *
 * Todas las llamadas necesitan sesión: el token del usuario se toma de
 * [SupabaseAuth.currentSession()]. Sin sesión, devuelven un fallo
 * ("Sin sesión") sin tocar la red.
 *
 * Todas las llamadas son asíncronas (hilo propio) y devuelven el
 * resultado por callback ([Callback]): (valor, error). Los callbacks
 * corren en un hilo de fondo: quien los reciba en la UI debe volver
 * al hilo principal (p. ej. `runOnUiThread { ... }`).
 *
 * Uso:
 * ```
 * GameProgressSync.upsertProgress(module = 1, lesson = 3, xp = 50,
 *     streak = 2, hearts = 3, gems = 10, completed = true) { _, error ->
 *     if (error != null) { /* reintentar luego; el progreso local manda */ }
 * }
 * GameProgressSync.fetchLeaderboard(20) { ranking, _ ->
 *     /* mostrar top 20 */
 * }
 * ```
 */
object GameProgressSync {

    /** Una fila de `game_progress`. */
    data class Progress(
        val module: Int,
        val lesson: Int,
        val xp: Int,
        val streak: Int,
        val hearts: Int,
        val gems: Int,
        val completed: Boolean
    )

    /** Una fila de `game_scores` (ranking). */
    data class ScoreEntry(
        val userId: String,
        val username: String?,
        val totalXp: Int,
        val level: Int
    )

    private const val TABLE_PROGRESS = "game_progress"
    private const val TABLE_SCORES = "game_scores"

    // ------------------------------------------------------------------
    // Progreso por lección
    // ------------------------------------------------------------------

    /**
     * Guarda (o actualiza) el avance de una lección.
     * Upsert por la clave única (user_id, module, lesson): si ya existe
     * la fila, se mezcla con los valores nuevos; si no, se crea.
     */
    fun upsertProgress(
        module: Int,
        lesson: Int,
        xp: Int,
        streak: Int,
        hearts: Int,
        gems: Int,
        completed: Boolean,
        callback: Callback<Progress>
    ) {
        val session = SupabaseAuth.currentSession()
        if (session == null) {
            callback.onResult(null, IllegalStateException("Sin sesión: inicia sesión primero."))
            return
        }
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("module", module)
            .put("lesson", lesson)
            .put("xp", xp)
            .put("streak", streak)
            .put("hearts", hearts)
            .put("gems", gems)
            .put("completed", completed)
            .put("updated_at", nowIso())
            .toString()
        // on_conflict dice qué clave única resuelve el duplicado;
        // Prefer resolution=merge-duplicates hace el upsert y
        // return=representation devuelve la fila guardada.
        val path = "$TABLE_PROGRESS?on_conflict=user_id,module,lesson"
        val prefer = "resolution=merge-duplicates,return=representation"
        runRequest("POST", path, body, session.accessToken, prefer) { result ->
            val mapped = result.fold(
                onSuccess = { json ->
                    val row = JSONArray(json).optJSONObject(0)
                    if (row != null) Result.success(parseProgress(row))
                    else Result.failure(IllegalStateException("El servidor no devolvió la fila guardada."))
                },
                onFailure = { Result.failure(it) }
            )
            callback.onResult(mapped.getOrNull(), mapped.exceptionOrNull())
        }
    }

    /** Lee todo el progreso del usuario, ordenado por módulo y lección. */
    fun fetchProgress(callback: Callback<List<Progress>>) {
        val session = SupabaseAuth.currentSession()
        if (session == null) {
            callback.onResult(null, IllegalStateException("Sin sesión: inicia sesión primero."))
            return
        }
        val path = "$TABLE_PROGRESS?select=*&order=module.asc,lesson.asc"
        runRequest("GET", path, null, session.accessToken, null) { result ->
            val mapped = result.fold(
                onSuccess = { json ->
                    val arr = JSONArray(json)
                    val list = ArrayList<Progress>(arr.length())
                    for (i in 0 until arr.length()) list.add(parseProgress(arr.getJSONObject(i)))
                    Result.success(list)
                },
                onFailure = { Result.failure(it) }
            )
            callback.onResult(mapped.getOrNull(), mapped.exceptionOrNull())
        }
    }

    // ------------------------------------------------------------------
    // Puntuación total y ranking
    // ------------------------------------------------------------------

    /**
     * Guarda (o actualiza) la puntuación total del usuario.
     * Upsert por user_id (único en `game_scores`).
     */
    fun submitScore(
        totalXp: Int,
        level: Int,
        username: String?,
        callback: Callback<ScoreEntry>
    ) {
        val session = SupabaseAuth.currentSession()
        if (session == null) {
            callback.onResult(null, IllegalStateException("Sin sesión: inicia sesión primero."))
            return
        }
        val body = JSONObject()
            .put("user_id", session.userId)
            .put("username", username)
            .put("total_xp", totalXp)
            .put("level", level)
            .put("updated_at", nowIso())
            .toString()
        val path = "$TABLE_SCORES?on_conflict=user_id"
        val prefer = "resolution=merge-duplicates,return=representation"
        runRequest("POST", path, body, session.accessToken, prefer) { result ->
            val mapped = result.fold(
                onSuccess = { json ->
                    val row = JSONArray(json).optJSONObject(0)
                    if (row != null) Result.success(parseScore(row))
                    else Result.failure(IllegalStateException("El servidor no devolvió la fila guardada."))
                },
                onFailure = { Result.failure(it) }
            )
            callback.onResult(mapped.getOrNull(), mapped.exceptionOrNull())
        }
    }

    /**
     * Lee el ranking: los [limit] usuarios con más XP total.
     * La política RLS de `game_scores` permite leer todas las filas
     * a cualquier usuario autenticado.
     */
    fun fetchLeaderboard(limit: Int = 20, callback: Callback<List<ScoreEntry>>) {
        val session = SupabaseAuth.currentSession()
        if (session == null) {
            callback.onResult(null, IllegalStateException("Sin sesión: inicia sesión primero."))
            return
        }
        val safeLimit = limit.coerceIn(1, 100)
        val path = "$TABLE_SCORES?select=user_id,username,total_xp,level" +
            "&order=total_xp.desc&limit=$safeLimit"
        runRequest("GET", path, null, session.accessToken, null) { result ->
            val mapped = result.fold(
                onSuccess = { json ->
                    val arr = JSONArray(json)
                    val list = ArrayList<ScoreEntry>(arr.length())
                    for (i in 0 until arr.length()) list.add(parseScore(arr.getJSONObject(i)))
                    Result.success(list)
                },
                onFailure = { Result.failure(it) }
            )
            callback.onResult(mapped.getOrNull(), mapped.exceptionOrNull())
        }
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private fun parseProgress(row: JSONObject) = Progress(
        module = row.getInt("module"),
        lesson = row.getInt("lesson"),
        xp = row.optInt("xp", 0),
        streak = row.optInt("streak", 0),
        hearts = row.optInt("hearts", 3),
        gems = row.optInt("gems", 0),
        completed = row.optBoolean("completed", false)
    )

    private fun parseScore(row: JSONObject) = ScoreEntry(
        userId = row.getString("user_id"),
        username = if (row.isNull("username")) null else row.optString("username"),
        totalXp = row.optInt("total_xp", 0),
        level = row.optInt("level", 1)
    )

    /**
     * Llamada a PostgREST. Cabeceras: apikey (ANON KEY pública) +
     * Authorization Bearer con el token DEL USUARIO (no la anon key):
     * es ese token lo que el RLS usa para saber quién eres
     * (`auth.uid()`). `prefer` solo se manda en upserts.
     */
    private fun runRequest(
        method: String,
        path: String,
        body: String?,
        userToken: String,
        prefer: String?,
        callback: (Result<String>) -> Unit
    ) {
        Thread {
            try {
                val url = URL("${SupabaseConfig.SUPABASE_URL}/rest/v1/$path")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer $userToken")
                    setRequestProperty("Content-Type", "application/json")
                    if (prefer != null) setRequestProperty("Prefer", prefer)
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
                callback(Result.failure(IOException("Sin conexión al servidor: ${e.message}", e)))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }.start()
    }

    /** Fecha/hora actual en ISO-8601 UTC, para `updated_at`. */
    private fun nowIso(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return fmt.format(java.util.Date())
    }

    /** Convierte un HTTP no-2xx en una excepción con mensaje legible. */
    private fun httpError(code: Int, body: String): Exception {
        val serverMsg = try {
            JSONObject(body).optString("message", "")
        } catch (_: Exception) {
            ""
        }
        val detail = if (serverMsg.isNotEmpty()) " — $serverMsg" else ""
        return when (code) {
            401 -> IllegalStateException(
                "Sesión no válida o expirada (HTTP 401)$detail. " +
                    "Vuelve a iniciar sesión."
            )
            403 -> IllegalStateException(
                "El servidor rechazó la operación (HTTP 403)$detail. " +
                    "Revisa las políticas RLS de la tabla."
            )
            400 -> IllegalArgumentException("Petición no válida (HTTP 400)$detail")
            404 -> IllegalStateException("Tabla o ruta no encontrada (HTTP 404)$detail")
            409 -> IllegalStateException("Conflicto de datos (HTTP 409)$detail")
            else -> IllegalStateException("Error del servidor (HTTP $code)$detail")
        }
    }
}
