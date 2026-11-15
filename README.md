# Sobji Dokan

Sobji Dokan is a point-of-sale and inventory management app for small vegetable shops and neighborhood stores. Built with Android, Jetpack Compose, and Kotlin, it helps shop owners track sales, purchases, parties (customers/suppliers), stock levels, and daily business reports.

## Features

### Sales & Billing

- Quick POS screen for fast checkout at the counter
- Sales status filtering (pending, completed, cancelled)
- Sales history with search and filtering

### Purchases & Inventory

- New purchase entry for restocking inventory
- Stock and purchase screen to view current inventory levels
- Stock filtering by category or availability

### Parties & Ledger

- Parties ledger screen to track customer and supplier dues
- Party due filtering (overdue, upcoming, cleared)
- Party sheets for detailed transaction history

### Reports & Analytics

- Expenses and reports screen for daily/weekly/monthly summaries
- Business calculator for profit and loss calculations

### Settings & Security

- Settings screen for app configuration
- PIN lock controller to secure app access
- Onboarding flow for first-time setup
- Backup manager for data export and restore

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Navigation Compose
- **Database:** Room (local SQLite) with KSP code generation
- **Preferences:** Android DataStore Preferences
- **Dependency Injection:** Manual DI with AppModule
- **Concurrency:** Kotlin Coroutines
- **Serialization:** kotlinx.serialization JSON
- **Testing:** JUnit, Robolectric, Compose UI Test, Espresso
- **Code Quality:** Spotless (ktlint), Detekt, Kover

## Project Structure

```
app/src/main/java/com/jubayerhossain/sobjidokan/
├── app/                 # Application class, MainActivity, DI module
├── core/                # Common extensions
├── data/                # Room database, DAOs, entities, repositories, preferences, backup
├── domain/              # Domain models, repositories, use cases, calculator
├── feature/             # Feature screens (dashboard, sales, purchases, inventory, parties, reports, settings, onboarding)
├── navigation/          # App navigation graph
└── ui/                  # ViewModels, UI state, components, theme
```

## Getting Started

### Prerequisites

- Android Studio (latest stable release)
- JDK 17 or later
- Android SDK with compileSdk 36
- An Android device or emulator (minSdk 24)

### Clone the Repository

```sh
git clone <repository-url>
cd codespaces-blank
```

### Open in Android Studio

1. Launch Android Studio
2. Select **Open an existing project**
3. Choose the `codespaces-blank` directory
4. Wait for Gradle sync to complete

### Build the Project

```sh
./gradlew build
```

### Run on a Device or Emulator

```sh
./gradlew installDebug
```

### Run Tests

```sh
./gradlew test
```

### Run Instrumented Tests

```sh
./gradlew connectedAndroidTest
```

### Format Code

This project uses Spotless with ktlint for Kotlin code formatting:

```sh
./gradlew spotlessApply
```

### Run Static Analysis

```sh
./gradlew detekt
```

## Contributing

Contributions are welcome! Please read the [Contributing Guide](CONTRIBUTING.md) for details on how to submit bug reports, feature requests, and pull requests.

## Code of Conduct

Please read and follow the [Code of Conduct](CODE_OF_CONDUCT.md) in all interactions with the project and its community.

## License

This project is licensed under the terms found in the [LICENSE](LICENSE) file.
