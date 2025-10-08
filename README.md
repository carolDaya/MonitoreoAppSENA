Structure de Paquetes en app/src/main/java
com.protector.miss.ui

.auth: Este parquet es fundamental torque las panatellas de authenticate son comparatives por ambos roles.
      -LoginActivity.kt y activity_login.xml: Para el initio de session de usuarios y administradores.
      -ForgotPasswordActivity.kt y activity_forgot_password.xml: Para el process de recuperation de contrast, ambient comparative.
      -SignupActivity.kt y activity_signup.xml: Solo para el registry de usuarios regulares, ya que los administradores probablemente se crean de forma manual.

.user: Este es el paquete exclusivo para las pantallas que solo el usuario regular puede ver.
      -HomeUserActivity.kt y activity_home_user.xml: La pantalla de bienvenida del usuario.
      -SensorDataActivity.kt y activity_sensor_data.xml: Para visualizar las tablas de los sensores.
      -AlertsActivity.kt y activity_alerts.xml: Para recibir las alertas.

.admin: Este paquete contiene las pantallas específicas para el administrador.
      -HomeAdminActivity.kt y activity_home_admin.xml: La pantalla de bienvenida del administrador.
      -AdminDashboardActivity.kt y activity_admin_dashboard.xml: Para los gráficos y el menú de usuarios.
      -UserManagementActivity.kt y activity_user_management.xml: Si hay una pantalla dedicada para gestionar usuarios.

.common: Puedes crear este paquete para las Activities o Fragments que son compartidos en los menús laterales o en otras partes de la app.
    -SidebarMenuFragment.kt: Un Fragmento que puede ser reutilizado en las pantallas principales tanto de usuario como de administrador, solo mostrando u ocultando ciertos elementos según el rol.

com.sena.monitoreo/
│
├── ui/                      # Interfaz gráfica (Activities, Fragments)
│   ├── admin/               # Pantallas exclusivas del administrador
│   │   ├── AdminDashboardActivity.kt   # Panel con gráficas y gestión de usuarios
│   │   └── HomeAdminActivity.kt        # Inicio del administrador
│   │
│   ├── auth/                # Autenticación y seguridad
│   │   ├── LoginActivity.kt           # Login con email/celular + contraseña
│   │   ├── SignupActivity.kt          # Registro de nuevos usuarios
│   │   ├── ForgotPasswordActivity.kt  # Recuperar acceso
│   │   └── ResetPasswordActivity.kt   # Restablecer contraseña
│   │
│   └── user/                # Pantallas del usuario normal
│       ├── HomeUserActivity.kt        # Dashboard principal
│       ├── SensorDataActivity.kt      # Gráficas de temperatura, pH, biogás
│       └── AlertsActivity.kt          # Alertas y recomendaciones IA
│
├── data/                    # Comunicación con backend y datos locales
│   ├── api/                 
│   │   ├── ApiService.kt    # Definición de endpoints (Retrofit: /login, /lecturas, /predict)
│   │   └── RetrofitClient.kt# Configuración de Retrofit (con URL del backend)
│   │
│   ├── model/               # Clases que representan la información que viene del backend
│   │   ├── Usuario.kt       # id, correo, rol
│   │   ├── Lectura.kt       # id, sensor, valor, timestamp
│   │   └── Prediccion.kt    # estado, probabilidad, recomendación
│   │
│   └── repository/          # Capa intermedia entre UI y API
│       ├── UserRepository.kt    # Login, registro, gestión de usuarios
│       └── SensorRepository.kt  # Peticiones de datos de sensores y predicciones IA
│
├── utils/                   # Utilidades
│   ├── SessionManager.kt    # Manejo de sesión y token del usuario
│   └── Extensions.kt        # Funciones extra reutilizables
│
└── App.kt                   # Clase Application (inicializa config global)

