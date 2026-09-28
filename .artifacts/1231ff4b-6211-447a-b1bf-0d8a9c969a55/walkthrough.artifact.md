# Walkthrough - Menú Semanal IA, Voz y Conciencia Horaria

He integrado características avanzadas y de alta tecnología en **Kalorias OS**:

## Nuevas Características

### [Menú Semanal IA & Conciencia de Hora]
- [MODIFY] [KaloriasViewModel.kt](file:///C:/Users/DJ-Ge/Desktop/Kalorias/app/src/main/java/com/example/kalorias/ui/KaloriasViewModel.kt):
  - **Conciencia del Momento del Día:** Detecta automáticamente si es *Mañana*, *Tarde* o *Noche* para ajustar los saludos y consejos.
  - **Generador de Menú Semanal por IA:** El apartado de "Menú & Nutrición" ahora muestra un planificador semanal completo de Lunes a Domingo adaptado al desgaste calórico y déficit del usuario.
- [MODIFY] [NutritionScreen.kt](file:///C:/Users/DJ-Ge/Desktop/Kalorias/app/src/main/java/com/example/kalorias/ui/screens/NutritionScreen.kt):
  - Añadido botón de voz (`TextToSpeech`) para escuchar en voz alta los consejos motivacionales, el balance y el resumen del día.

## Validación y Pruebas
- Compilación completada con éxito ejecutando `gradle_build("app:assembleDebug")` (**BUILD SUCCESSFUL**).
