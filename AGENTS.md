# Правила работы

1. Пиши код и правь файлы только в этом проекте.
2. По команде «коммить» — сделай коммит и push на GitHub.
3. Сообщения коммитов пиши на русском в формате: «тип : краткое содержание» (например, «тест: изменение цвета фона»).
4. Перед каждым коммитом проверяй, что проект собирается.
5. Если что-то непонятно — сначала спроси, потом делай.

## Стек — использовать только это

- Уже используется:
  - Kotlin + Jetpack Compose
  - CameraX 1.6.2 (androidx.camera:camera-camera2, camera-lifecycle, camera-video, camera-view)
  - Coil 2.x (io.coil-kt:coil-compose, 2.7.0). Не обновлять до Coil 3 без явного указания
  - Hilt 2.60.1 (com.google.dagger:hilt-android)
  - KSP
  - Navigation Compose 2.8.9
  - osmdroid (org.osmdroid:osmdroid-android, 6.1.20)
  - Собственный прокси для тайлов OSM: https://hefty-mule-2745.konovalius.deno.net/ (Deno Deploy). Заменяет прямой доступ к tile.openstreetmap.org, который блокируется в России
  - FusedLocationProviderClient (com.google.android.gms:play-services-location)
- Запланировано:
  - Room 2.8.5 (androidx.room:room-runtime, room-ktx, room-compiler)
- minSdk = 29, targetSdk = 36, compileSdk = 37 (значения не обязаны совпадать)

## Запрещено

- Не использовать deprecated API: старый Camera API, синхронный Geocoder.getFromLocation(), WRITE_EXTERNAL_STORAGE
- Не использовать абсолютные пути как первичные идентификаторы в БД
- Не добавлять новые библиотеки без явного указания
- Не менять minSdk, targetSdk, compileSdk без явного указания
- Не менять координаты библиотек (io.coil-kt → io.coil-kt.coil3) без явного указания
- Не трогать файлы, не относящиеся к текущей задаче
- Не генерировать несуществующие API и классы
- Не возвращать TileSourceFactory.MAPNIK и не обращаться к tile.openstreetmap.org напрямую — только через прокси hefty-mule-2745.konovalius.deno.net. В XYTileSource не использовать плейсхолдеры {z}/{x}/{y} в базовом URL — osmdroid подставляет их сам

## Архитектура

- Single Activity, Navigation Compose
- MVVM: ViewModel + StateFlow, репозитории, DAO
- Виртуальные папки: структура папок существует только в БД Room, на диске файлы лежат плоско с uuid-именами
- Все сущности в БД имеют поле uuid (String, первичный ключ)
- Связи между сущностями строятся через uuid, не через пути

## Хранение файлов

- Фото: системная галерея, MediaStore, альбом Pictures/MyAgent
- Структура объектов существует только в БД Room, на диске структуры нет
- Имя файла: yyyy-MM-dd_HH-mm-ss; при наличии геометки — <геометка>_yyyy-MM-dd_HH-mm-ss
- Слэши и двоеточия в имени файла запрещены
- Удаление фото — через ContentResolver.delete(uri), а не File.delete()
- EXIF: дата съёмки записывается в каждый снимок; GPS-координаты отложены до шага геолокации

## После каждого шага

- Запустить сборку: ./gradlew assembleDebug
- Показать результат сборки (успех или полный текст ошибок)
- Не переходить к следующему шагу, если сборка не прошла