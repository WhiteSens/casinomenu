# Казино (Fabric 1.21.4, клиентский мод)

Открыть меню: клавиша **K** (меняется в настройках управления) или команда **/casino**.

Разделы: Рулетка, Гадалка, Лучший конь, Динамит (пока заглушка).

## Сборка
Нужен JDK 21 и интернет (при первой сборке Gradle скачает Minecraft и Fabric).

Вариант 1 (проще): открыть папку в IntelliJ IDEA -> Gradle подтянется сам -> задача `build`.
Вариант 2: установить Gradle 8.12 и выполнить в папке мода `gradle build`
(или один раз `gradle wrapper`, дальше `./gradlew build`).

Готовый мод: `build/libs/casinomod-1.0.0.jar` (файл `-sources` не нужен).
Положить в `mods/` вместе с Fabric API.

Если Gradle не находит версию Fabric API — возьмите актуальную для 1.21.4
на https://fabricmc.net/develop и вставьте в `gradle.properties` (`fabric_version`).
