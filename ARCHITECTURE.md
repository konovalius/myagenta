# Архитектура

## Стек
- Kotlin + Jetpack Compose
- CameraX 1.6.2
- Coil 2.x (`io.coil-kt:coil-compose`, 2.7.0). **Не обновлять до Coil 3.**
- Hilt 2.60.1, KSP
- Navigation Compose 2.8.9
- osmdroid 6.1.20
- Room 2.8.5
- FusedLocationProviderClient
- `minSdk = 29`, `targetSdk = 36`, `compileSdk = 37`

## Архитектура
- Single Activity, Navigation Compose.
- MVVM: ViewModel + StateFlow, репозитории, DAO.
- Виртуальные папки — только в БД Room. На диске файлы лежат плоско.
- Все сущности в БД — с полем `uuid` (String, PK).
- Связи между сущностями строятся через `uuid`, не через пути.

## Хранение файлов
- Фото — системная галерея, MediaStore, альбом `Pictures/MyAgent`.
- Имя файла: `yyyy-MM-dd_HH-mm-ss`; при геометке — `<геометка>_yyyy-MM-dd_HH-mm-ss`.
- Слэши и двоеточия в имени файла запрещены.
- Удаление — через `ContentResolver.delete(uri)`, не `File.delete()`.
- EXIF: дата съёмки. GPS — отложен.

## Карта
- Прокси тайлов OSM: `https://hefty-mule-2745.konovalius.deno.net/` (Deno Deploy).
- **Не возвращать** `TileSourceFactory.MAPNIK` и не обращаться к `tile.openstreetmap.org` напрямую.
- В `XYTileSource` не использовать плейсхолдеры `{z}/{x}/{y}` в базовом URL.