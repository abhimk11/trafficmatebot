# 🚦 TrafficMateBot

**TrafficMateBot** is a Telegram bot built with Java and Spring Boot that allows users to:
- Compare travel time between two locations for different modes of transport (Car, Bike, Walk)
- Subscribe for automatic ETA updates every 6 hours
- Update their saved locations
- Unsubscribe anytime

---

## 📦 Features

- 📍 Share source & destination locations using Telegram's location feature
- 🚗 Compare ETA for Car, Bike, and Walk using OpenRouteService API
- 🔁 Get automatic updates every 6 hours if subscribed
- ✏️ Update location preferences anytime
- ❌ Unsubscribe from updates

---

## ⚙️ Tech Stack

- **Java 17**
- **Spring Boot**
- **MongoDB** for storing subscriptions
- **Telegram Bot API** using [telegrambots-spring-boot-starter](https://github.com/rubenlagus/TelegramBots)
- **OpenRouteService API** for travel data

---

## 🛠️ Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/TrafficMateBot.git
cd TrafficMateBot
