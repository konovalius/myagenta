# Технический долг и бэклог

⚠️ **ВАЖНО:** Это список известных проблем и возможных рефакторингов.

**НЕ ВЫПОЛНЯТЬ** автоматически. Каждый пункт — только по **явной команде** пользователя.

Файл нужен как **контекст** — чтобы понимать, что в проекте есть «дыры», но не бросаться их чинить.

## 1. Контекст

Проект — Android-приложение камеры с интеграцией PastVu API, построенное на Kotlin + Jetpack Compose, CameraX, Room, Hilt, osmdroid. Архитектура: Single Activity, MVVM, Navigation Compose.

Текущее состояние: код работает, но есть критические дыры, блокирующие продакшен-релиз и масштабирование.

## 2. Критические проблемы (блокеры)

| № | Проблема | Файлы | Риск |
|---|----------|-------|------|
| 1 | fallbackToDestructiveMigration при каждом изменении схемы БД | AppDatabase.kt:34, Di.kt:24 | Полная потеря пользовательских данных при обновлении приложения |
| 2 | Log.wtf используется как основной логгер | CameraViewModel.kt, MapViewModel.kt, PastVuApi.kt | Крашит приложение в debug-сборках (wtf = "What a Terrible Failure" → AssertionError) |
| 3 | Отсутствие тестов (unit / integration / UI) | app/src/test, app/src/androidTest | Нет регрессионной безопасности, нельзя включать CI/CD |
| 4 | CameraViewModel — God Object (935 строк) | CameraViewModel.kt | Неподдерживаемый код, невозможно тестировать, смешаны ответственности |
| 5 | Нет обработки ошибок в UI | везде Toast + swallowed exceptions | Молчаливые сбои, плохой UX |

## 3. Значительные проблемы (нужно исправить до релиза)

| № | Проблема | Описание |
|---|----------|----------|
| 6 | Дублирование логики geo-папок | findGeoFolderNear / findOrCreateGeoFolder в CameraViewModel и MapViewModel |
| 7 | Состояние гонки (race conditions) | Общие мутабельные флаги geoPromptShown, pendingNewPhotoUri, archiveTarget в асинхронных колбэках |
| 8 | Нет кэширования / offline-first для PastVu API | Каждый запрос — сеть, нет stale-while-revalidate |
| 9 | Жесткод констант | GEO_RADIUS_METERS=20f, ARCHIVE_DOWNLOAD_TIMEOUT_MS=20000 разбросаны |
| 10 | ProGuard/R8 отключён в release | isMinifyEnabled = false → большой APK, нет обфускации |
| 11 | Нет краш-репортинга | Нет Crashlytics / Play Console integration |
| 12 | Костыль навигации | isPreviewOpen mutex в AppNavHost — признак багов навигации |

## 4. Предлагаемые решения (для обсуждения)

### 4.1 Миграции Room (Приоритет 1)

Варианты:

- А) Написать настоящие миграции (Migration классы) + exportSchema = true → безопасно, но требует ручного написания SQL для каждого изменения схемы.
- Б) Включить exportSchema = true, генерировать схемы, писать миграции только при breaking changes. Для dev-веток оставить fallbackToDestructiveMigration, для release — строгие миграции.
- В) Использовать Room 2.8+ autoMigration для простых изменений (добавление колонок, таблиц).

Рекомендация: Вариант Б — баланс безопасности и скорости. Настроить CI проверку: если схема изменилась без миграции — фейл сборки.

### 4.2 Логирование (Приоритет 1)

Заменить Log.wtf на структурированное логирование: Timber или wrapper над android.util.Log с build-конфигурацией (в release — только w/e).

### 4.3 Тестовая стратегия (Приоритет 1)

| Уровень | Что тестировать | Инструменты |
|---------|-----------------|-------------|
| Unit | PastVuApi, PlaceNameResolver, репозитории, UseCases | JUnit4 + MockK + Turbine (Flow) |
| Integration | DAO, Room DB, FileRepository (MediaStore мок) | Room inMemoryDatabaseBuilder, Robolectric |
| UI | Критические флоу: съёмка, сохранение, навигация, карта | Compose Test Rules, Espresso |

Минимальный старт: покрыть PastVuApi.parseSnapshot, PlaceNameResolver, MediaRepository — 30-40% бизнес-логики за 1 день.

### 4.4 Рефакторинг CameraViewModel (Приоритет 1) — Strangler Fig

| Этап | Выносим в отдельный класс |
|------|---------------------------|
| 1 | CapturePhotoUseCase |
| 2 | RecordVideoUseCase |
| 3 | ArchiveDownloadUseCase |
| 4 | GeoFolderUseCase |
| 5 | GeoPromptManager |
| 6 | CameraState (UI state) |

Принцип: ViewModel становится тонким оркестратором.

### 4.5 Единый GeoFolderService (Приоритет 2)

Вынести дублированную логику в GeoFolderUseCase.

### 4.6 Обработка ошибок + UI State (Приоритет 2)

Ввести sealed-классы для UI состояний.

### 4.7 Сетевая устойчивость (Приоритет 2)

Retry, in-memory кэш с TTL, circuit breaker.

### 4.8 Константы → Конфиг (Приоритет 3)

AppConfig data class + @Singleton в Hilt.

### 4.9 ProGuard/R8 + Baseline Profiles (Приоритет 3)

### 4.10 Краш-репортинг (Приоритет 3)

Firebase Crashlytics (или Sentry).

## 5. План работ (Roadmap)

| Спринт | Фокус | Деливерабл |
|--------|-------|------------|
| 1 | Блокеры: миграции, логи, минимальные тесты | |
| 2 | Рефакторинг CameraVM (этапы 1-3) | |
| 3 | Рефакторинг CameraVM (этапы 4-6) + GeoFolderService | |
| 4 | Сетевая устойчивость + кэш PastVu | |
| 5 | ProGuard, Baseline Profiles, Crashlytics | |
| 6 | UI-полировка: ошибки, офлайн, пейджинг | |

## 6. Открытые вопросы

1. Миграции: SQL сейчас или exportSchema + автогенерация?
2. Тесты: Compose Test / Espresso / Paparazzi?
3. Краш-репортинг: Firebase Crashlytics или Sentry?
4. CameraViewModel: поэтапно или полный рерайт?
5. Константы: BuildConfig или DataStore?
6. Geo-папки: радиус 20м — финальный?

## 7. Критерии приёмки (Definition of Done)

- `./gradlew assembleDebug` без предупреждений
- `./gradlew testDebugUnitTest` зелёные (>70% coverage)
- `./gradlew connectedAndroidTest` проходят критические UI-флоу
- Обновление версии БД (schema +1) не теряет данные
- Release APK с minifyEnabled=true
- Crashlytics получает тестовый краш
- Нет Log.wtf в кодбейсе

## 8. Известные ограничения (не баги)

### 8.1 Файлы MyAgent не удаляются штатным проводником TECNO

Статус: НЕ БАГ. Штатное поведение Android 11+ (Scoped Storage).
Дата проверки: 2026-10-06 (TECNO CK7n, Android 14).

Причина:
- Приложение сохраняет файлы через MediaStore с owner_package_name=com.example.myagent.
- Сторонние приложения без MANAGE_EXTERNAL_STORAGE не могут удалять файлы,
  принадлежащие другому пакету.
- До 2026-09-30 файлы сохранялись с owner_package_name=NULL — поэтому проводник
  их удалял. После изменения кода (с 2026-09-30 16:06) — не удаляет.

Как проверили (adb):
  adb shell content query --uri content://media/external/images/media
    --projection _id:_display_name:relative_path:is_pending:owner_package_name
  → у файлов с 2026-09-30 owner_package_name=com.example.myagent, is_pending=0.

Обходные пути для пользователя:
- Files by Google, Solid Explorer, ZArchiver — удаляют.
- Проводник TECNO — не имеет нужных прав (ограничение прошивки).
- Удаление через ПК (MTP) или adb — работает.

Решение: ничего в коде не менять. Текущее поведение корректно.
