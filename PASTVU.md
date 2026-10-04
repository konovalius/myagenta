# API PastVu

## Endpoint

`POST https://api.pastvu.com/api2`
Content-Type: application/json

Обязательный заголовок `User-Agent`. GET с URL-encoded `params` тоже работает, но используется POST.

## Метод `photo.getByBounds`

Возвращает фото и кластеры в области карты.

**Запрос:**

```json
{
  "method": "photo.getByBounds",
  "params": {
    "z": 15,
    "geometry": {
      "type": "Polygon",
      "coordinates": [[[lon, lat], ...]]
    }
  }
}
```

**ВАЖНО:**

- Параметр называется `geometry`, не `bounds`. С `bounds` — ошибка.
- Формат координат — GeoJSON: `[lon, lat]`.

## Структура ответа

- `result.photos` — массив фото.
- `result.clusters` — массив кластеров (групп фото).
- До 16 зума — кластеры и фото. С 17 — только фото.
- **Максимальный зум API — 16.** На `z=17` и выше ответ всегда пустой (`photos: []`, `clusters: []`, ~64 байта), даже если в области есть фото. Проверено прямым запросом к API на одном и том же bbox: `z=15` → 1 фото + 4 кластера, `z=16` → 10 фото + 7 кластеров, `z=17`/`z=18` → 0 и 0. Зум карты при этом может быть больше — ограничение только на параметр `z` в запросе.

## Формат `photo`

- `cid` — ID.
- `file` — имя файла.
- `title` — название.
- `geo` — `[lat, lon]` ← lat первый.
- `year`, `year2` — годы.

## Формат `cluster`

- `p` — photo (превью кластера).
- `geo` — `[lat, lon]` (lon второй).
- `c` — количество фото в кластере.

**Ловушка (проверено на реальном ответе):** `[lon, lat]` — это `p.geo` внутри кластера, а сам `cluster.geo` — `[lat, lon]`. То есть в превью кластера порядок обратный относительно `result.photos[].geo`. Позицию маркера кластера брать из `cluster.geo` (порядок `[lat, lon]`), миниатюру — из `p.file`.

Пример:

```json
{
  "p": {
    "cid": 1828491,
    "file": "w/i/z/wizdmqn7x805kypsni.jpg",
    "title": "...",
    "year": 1977,
    "year2": 1977,
    "dir": "e",
    "geo": [30.498363, 59.971536]
  },
  "geo": [59.971468, 30.498489],
  "c": 13
}
```

## URL для файлов

- `https://img.pastvu.com/h/{file}` — миниатюра.
- `https://img.pastvu.com/d/{file}` — стандартный.
- `https://img.pastvu.com/a/{file}` — оригинал.

## Ошибки

- Неверное имя метода или параметра → HTTP 200 с телом
  `{"type":"ApplicationError","code":"UNHANDLED_ERROR","message":"A server error occurred"}`.
  Ошибка маскирует неверную геометрию — при `UNHANDLED_ERROR` сначала проверять вложенность
  полигона: кольцо `coordinates` — ровно `[[[lon, lat], ...]]`, без лишней обёртки.
- Несуществующий метод → `{"type":"NotFoundError","code":"NO_SUCH_RESOURCE"}`.
  Методов `photo.getByPoint` и `photo.search` нет.

## Реализация

`data/util/PastVuApi.kt` — object, POST JSON через HttpURLConnection.
Образец стиля: `data/util/OverpassGeocoder.kt`.

Экраны и файлы архива:

- `ui/map/MapScreen.kt` — карта с маркерами фото и кластеров, toggle «Архив»,
  Bottom Sheet `ArchivePhotoSheet` с превью (`h/`).
- `ui/map/ArchivePhotoViewerScreen.kt` — полноэкранный просмотр (`d/`), кнопка «Сохранить».
- `ui/map/ArchivePhotoViewerViewModel.kt` — скачивание `d/` через HttpURLConnection
  и запись в галерею через MediaStore: `RELATIVE_PATH = Pictures/PastVu`,
  `DISPLAY_NAME = PastVu_{cid}_{yyyy-MM-dd_HH-mm-ss}.jpg`, `IS_PENDING` 1 → 0.
  Разрешения не нужны: minSdk 29, запись идёт через `ContentResolver`.
