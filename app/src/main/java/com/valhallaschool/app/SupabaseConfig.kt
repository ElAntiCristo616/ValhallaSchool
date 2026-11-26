package com.valhallaschool.app

/**
 * Configuración de Supabase para Valhalla School.
 *
 * La ANON KEY es la clave PÚBLICA del proyecto (publishable): va en la
 * app sin problema, la protege el RLS de las tablas. NUNCA poner aquí
 * la service_role key: esa solo vive en el servidor.
 *
 * Proyecto: Valhalla School — https://aombvvcfxwlnhujjfqwt.supabase.co
 * Región us-east-2, plan Free. Confirmación de correo APAGADA:
 * el registro entra directo, sin correo.
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://aombvvcfxwlnhujjfqwt.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFvbWJ2dmNmeHdsbmh1ampmcXd0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTU2NDczNjEsImV4cCI6MjExMTIyMzM2MX0.OXNQ9VC01cyyAwCU3up-l-jXJTyCoCmZvkU40Iu4Mcs"
}
