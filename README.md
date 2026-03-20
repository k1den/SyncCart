#  Sync Cart

**Sync Cart** — это умное приложение для организации совместных покупок. Оно позволяет пользователям объединяться в группы, распределять товары между собой и находить лучшие места для шопинга, сохраняя при этом простоту обычного списка дел.

---

##  Ключевые функции

* Личные списки;
* Генерация списка покупок с помощью ИИ;
* Общие списки с семьей или друзьями;
* Встроенные чаты;
* Магазины на карте.

---

##  Архитектура проекта

Проект разделен на две независимые части:
* **`client/`**: Мобильное Android-приложение (Java/XML).
* **`server/`**: RESTful API сервис (Java) и работа с базой данных PostgreSQL.

---

##  Технологический стек

| Слой | Технологии                                   |
| :--- |:---------------------------------------------|
| **Frontend** | Android SDK, XML, Java, OpenStreetMap        |
| **Backend** | Java, Spring Boot (Maven), Llama |
| **Database** | PostgreSQL                                   |

---

##  Запуск

### 1. Клонирование репозиториев
Для работы над проектом тебе понадобятся **Android Studio** и **IntelliJ IDEA**.

```bash
# Клонирование репозитория андроид-приложения
git clone [https://github.com/k1den/SyncCart.git](https://github.com/k1den/SyncCart.git)

# Клонирование репозитория сервера
git clone [https://github.com/k1den/ServerCart.git](https://github.com/k1den/ServerCart.git)
```

### 2. Настройка базы данных (PostgreSQL)
Создайте БД с именем SyncCart и настройте доступ в server/src/main/resources/application.properties:

```Properties
spring.datasource.url=jdbc:postgresql://localhost:5432/SyncCart
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
```

### 3. Запуск Сервера (IntelliJ IDEA)
Откройте проект ServerCart.

Дождитесь загрузки Maven зависимостей.

Нажмите Run (зеленый треугольник) в главном классе приложения.

Сервер запустится на http://localhost:8080.

### 4. Запуск Приложения (Android Studio)
Откройте проект SyncCart.

Выполните Build -> Clean Project, затем Sync Project with Gradle Files.

Важно: Если используете эмулятор, в класс RetrofitClient укажите адрес http://10.0.2.2:8080. 
Если же нет, то http://ip_вашего_ноутбука:8080.

Нажмите Run и выберите ваше устройство/эмулятор.

## Видео по использованию

[![Смотреть демо Sync Cart](https://img.shields.io/badge/Смотреть_Видео-Yandex.Disk-blue?style=for-the-badge&logo=yandex)](https://disk.yandex.ru/d/AxSEE1hCcWxQCw)
> **Примечание:** Нажмите на кнопку выше, чтобы перейти к просмотру видео на Яндекс.Диске.