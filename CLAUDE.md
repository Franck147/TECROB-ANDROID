# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Full build with checks
./gradlew build

# Install on connected device
./gradlew installDebug

# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.example.tecrobsys.SomeTestClass"
```

Android Studio is the primary IDE. Use Logcat for runtime logs; filter by `TecrobSys` tag.

## Architecture

**MVVM + Single Activity + Multiple Fragments**

```
actividades/     → ActividadLogin (entry), ActividadPrincipal (container)
fragmentos/      → UI per feature: dashboard, ordenes, nueva_orden, catalogo
viewmodels/      → One ViewModel per fragment, AndroidViewModel for those needing Context
repositorios/    → HTTP abstraction; each method takes a callback
modelos/         → Plain Java POJOs with Gson @SerializedName annotations
red/             → SupabaseCliente (Retrofit singleton) + SupabaseServicio (interface)
utils/           → SesionManager, GeneradorPDF, UtilEstado, UtilFecha
```

**Navigation:** `ActividadPrincipal` hosts 4 fragments using `show()/hide()` (not `replace()`), which preserves scroll and filter state. The bottom nav central button opens `FragmentoNuevaOrden`.

**Data flow:** Fragment observes ViewModel LiveData → ViewModel calls Repository → Repository calls Retrofit service → callback returns to ViewModel → ViewModel updates LiveData.

## Supabase Integration

- Backend is Supabase (PostgreSQL + Auth).
- Credentials are in `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`, injected from `app/build.gradle`.
- `SupabaseCliente.java` creates two Retrofit instances: one for `/rest/v1/` (data) and one for `/auth/v1/` (auth).
- All requests include `apikey` and `Authorization: Bearer <jwt>` headers via OkHttp interceptors.
- Use `Prefer: return=representation` header on POST/PATCH to get the created/modified record back.
- Session token is stored in `EncryptedSharedPreferences` via `SesionManager`.

**Auth flow:** POST `/auth/v1/token` → receive JWT → GET `/rest/v1/tecnico?auth_user_id=eq.{uid}` to fetch the internal profile → store both in `SesionManager`.

## Key Models

| Model | Table | Notes |
|---|---|---|
| `Orden` | `orden` | Core entity; contains nested `ClienteResumen`, `UsuarioResumen`, `Equipo`, `List<ServicioCatalogo>` |
| `Cliente` | `cliente` | Scoped by `empresa_id` |
| `Tecnico` | `tecnico` | Linked to Supabase Auth via `auth_user_id`; has `rol` (administrador/tecnico) |
| `ServicioCatalogo` | `servicio_catalogo` | `seleccionado` is a transient field (not in DB) |
| `Equipo` | `equipo` | One-to-one with `Orden`, created after the order |

**Order states:** `pendiente → diagnostico → en_progreso → listo → entregado` (also `cancelado`, `sin_reparacion`). Use `UtilEstado` to get display text and colors.

## Conventions

- **Language:** All variable names, methods, and comments are in Spanish. UI strings belong in `res/values/strings.xml`.
- **ViewBinding:** Always use ViewBinding (never `findViewById`). Binding is inflated in `onCreateView` and released in `onDestroyView`.
- **Callbacks:** The project does not use coroutines or RxJava. All async work uses Retrofit `Call<T>` with inline `Callback<T>` lambdas in the repository layer.
- **Multi-empresa:** All data queries must include `empresa_id=eq.{id}` to scope data to the tenant. Obtain `empresa_id` from `SesionManager.getInstance(context).obtenerEmpresaId()`.
- **Admin checks:** Use `SesionManager.esAdministrador()` to gate admin-only UI (e.g., catalog CRUD, nav_config menu item).
- **PDF generation:** `GeneradorPDF` uses the native Android `PdfDocument` API and saves to `getExternalFilesDir()/pdfs/`. Sharing uses a `FileProvider` URI declared in `res/xml/file_provider_paths.xml`.
- **Date handling:** Supabase returns ISO 8601 strings. Use `UtilFecha` for formatting. Use `UtilFecha.obtenerFechaHoy()` when submitting dates.

## SDK & Dependencies

- `compileSdk`/`targetSdk`: 34, `minSdk`: 26, Java 21
- Networking: Retrofit 2.11 + OkHttp 4.12 + Gson 2.10
- UI: Material Design 3 (`com.google.android.material:material:1.11.0`), ConstraintLayout, RecyclerView, SwipeRefreshLayout
- Lifecycle: `lifecycle-viewmodel:2.8.2`, `lifecycle-livedata:2.8.2`
- Security: `security-crypto:1.1.0-alpha06` (EncryptedSharedPreferences)
