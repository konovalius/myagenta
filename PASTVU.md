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

## Формат `photo`

- `cid` — ID.
- `file` — имя файла.
- `title` — название.
- `geo` — `[lat, lon]` ← lat первый.
- `year`, `year2` — годы.

## Формат `cluster`

- `p` — photo (превью кластера).
- `geo` — `[lon, lat]` ← lon первый (ЛОВУШКА!).
- `c` — количество фото в кластере.

**Ловушка (проверено на реальном ответе):** `[lon, lat]` — это `p.geo` внутри кластера, а сам `cluster.geo` — `[lat, lon]`. То есть в превью кластера порядок обратный относительно `result.photos[].geo`. Для маркера кластера брать `p.geo`.

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
