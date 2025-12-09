InmuebleCheck 2.0:
InmuebleCheck es una aplicación Android nativa diseñada para facilitar la gestión y búsqueda de propiedades inmobiliarias. Conecta a arrendadores (gerentes) con arrendatarios (agentes).

Tecnologías Utilizadas:
Lenguaje: Java 11 / 17
Arquitectura: MVVM (Model-View-ViewModel)
Base de Datos: Firebase Firestore & Realtime Database
Autenticación: Firebase Auth
Almacenamiento Local: Room Database (para funcionamiento offline)
Mapas: Google Maps SDK
UI: Material Design 3, ConstraintLayout, RecyclerView

Configuración del Proyecto:
Para ejecutar este proyecto localmente, necesitas configurar Firebase:
Clona el repositorio.
Crea un proyecto en Firebase Console.
Habilita Authentication (Email/Password) y Firestore Database.
Descarga el archivo google-services.json de tu proyecto Firebase.
Pega el archivo en la carpeta app/ del proyecto (este archivo está ignorado por git por seguridad).
Sincroniza el proyecto con Gradle y ejecuta.
