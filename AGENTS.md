# Правила работы

1. Пиши код и правь файлы только в этом проекте.
2. По команде «коммить» — сделай коммит и push на GitHub.
3. Сообщения коммитов пиши на русском в формате: «тип : краткое содержание» (например, «тест: изменение цвета фона»).
4. Перед каждым коммитом проверяй, что проект собирается.
5. Если что-то непонятно — сначала спроси, потом делай.

## Стек — использовать только это

- Kotlin + Jetpack Compose
- CameraX (androidx.camera:camera-camera2, camera-lifecycle, camera-video, camera-view)
- Room (androidx.room:room-runtime, room-ktx, room-compiler)
- osmdroid (org.osmdroid:osmdroid-android)
- Coil (io.coil-kt:coil-compose)
- Hilt (com.google.dagger:hilt-android)
- FusedLocationProviderClient (com.google.android.gms:play-services-location)
- minSdk = 29, targetSdk = 36, compileSdk = 36

## Запрещено

- Не использовать deprecated API: старый Camera API, синхронный Geocoder.getFromLocation(), WRITE_EXTERNAL_STORAGE
- Не использовать абсолютные пути как первичные идентификаторы в БД
- Не добавлять новые библиотеки без явного указания
- Не менять minSdk, targetSdk, compileSdk без явного указания
- Не трогать файлы, не относящиеся к текущей задаче
- Не генерировать несуществующие API и классы

## Архитектура

- Single Activity, Navigation Compose
- MVVM: ViewModel + StateFlow, репозитории, DAO
- Виртуальные папки: структура папок существует только в БД Room, на диске файлы лежат плоско с uuid-именами
- Все сущности в БД имеют поле uuid (String, первичный ключ)
- Связи между сущностями строятся через uuid, не через пути

## Хранение файлов

- Рабочие файлы: app-specific storage (getExternalFilesDir или filesDir)
- Имя файла на диске: uuid.jpg / uuid.mp4 / uuid.txt
- При экспорте: копировать файлы в MediaStore (Pictures/AppName/...) или в место, выбранное пользователем через SAF
- В EXIF каждого фото записывать GPS-координаты и дату

## После каждого шага

- Запустить сборку: ./gradlew assembleDebug
- Показать результат сборки (успех или полный текст ошибок)
- Не переходить к следующему шагу, если сборка не прошла