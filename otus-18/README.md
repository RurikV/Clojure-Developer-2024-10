### Урок #18

## Часть 1: Асинхронная работа с Pokemon API

core.async

https://github.com/clojure/core.async

API Docs

https://clojure.github.io/core.async/

### ДЗ
#### Научиться работать с https://pokeapi.co/ в асинхронном стиле.

- Научиться получать список покемонов — это отправная точка для сбора информации.
- Получить для каждого покемона, упомянутого в ответе на первый запрос, его имя, наименования типов на заданном языке (их может быть несколько). Эти данные можно получить асинхронно, но результата следует подождать и сохранить оный в подходящие структуры данных.
- Типы покемонов — справочные данные. Следует озаботиться получением полного перечня типов с их наименованиями, чтобы затем только обращаться к сохранённому справочнику при добавлении наименований типов к данным покемона.

## Часть 2: Работа с данными по покемонам в базе Postgres

### Цель
Научиться работать с распространёнными библиотеками для работы с реляционными базами данных.

### Описание
- Написать миграции для схемы базы данных
- Используя библиотеку ragtime применить миграции
- Сохранить данные по покемонам в базу данных
- Используя библиотеку honey-sql сделать конструктор запросов к базе данных

### Используемые библиотеки
- [next.jdbc](https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.874/doc/readme) - для работы с базой данных
- [ragtime](https://github.com/weavejester/ragtime) - для миграций
- [honeysql](https://cljdoc.org/d/com.github.seancorfield/honeysql/2.4.1026/doc/readme) - для построения SQL запросов

### Инструкция по запуску

#### Настройка базы данных
1. Установите PostgreSQL, если он еще не установлен
2. Создайте базу данных `pokemon_db`:
   ```sql
   CREATE DATABASE pokemon_db;
   ```
3. Настройте подключение к базе данных в файле `src/otus_18/homework/db.clj`, изменив параметры в `db-spec` при необходимости

#### Применение миграций
```clojure
(require '[otus-18.homework.migrations :as migrations])
(require '[otus-18.homework.db :as db])

;; Применить миграции
(migrations/migrate db/db-spec)

;; Откатить последнюю миграцию (если нужно)
(migrations/rollback db/db-spec)

;; Откатить все миграции (если нужно)
(migrations/rollback-all db/db-spec)
```

#### Загрузка данных в базу
```clojure
(require '[otus-18.homework.db :as db])

;; Загрузить данные о покемонах в базу
(db/load-pokemons-to-db! :limit 50 :lang "ja")
```

#### Выполнение запросов
```clojure
(require '[otus-18.homework.query :as query])

;; Получить все типы покемонов
(query/get-all-types)

;; Получить покемонов определенного типа
(query/get-pokemons-by-type "electric")

;; Получить количество покемонов каждого типа
(query/get-pokemon-count-by-type)

;; Получить покемонов с несколькими типами
(query/get-pokemons-with-multiple-types)

;; Получить распределение типов покемонов
(query/get-type-distribution)

;; Построить произвольный запрос
(query/execute-query 
  (query/custom-query
    :select [:p.name :t.name]
    :from [[:pokemons :p]]
    :join [[:pokemon_types :pt] [:= :p.id :pt.pokemon_id]
           [:types :t] [:= :pt.type_id :t.id]]
    :where [:= :t.name "electric"]
    :order-by [:p.name]
    :limit 5))
```
