package com.valhallaschool.app

/**
 * Callback Java-amigable para las llamadas asíncronas a Supabase.
 *
 * `kotlin.Result` es una value class: sus métodos (`isSuccess`,
 * `getOrNull`, `exceptionOrNull`…) no son visibles desde Java, así
 * que la API pública no lo expone. El resultado llega como
 * (valor, error): en éxito `error` es null; en fallo `value` es
 * null y `error` trae el motivo.
 *
 * Uso desde Java:
 * ```
 * SupabaseAuth.INSTANCE.signUp(email, pass, (session, err) -> {
 *     if (err != null) { /* mostrar err.getMessage() */ return; }
 *     // session ya disponible
 * });
 * ```
 */
fun interface Callback<T> {
    fun onResult(value: T?, error: Throwable?)
}
