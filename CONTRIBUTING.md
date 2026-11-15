# Contributing

Thank you for considering a contribution to this project. We welcome contributions
of all kinds, including bug reports, feature requests, documentation improvements,
and code changes.

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 17 or later
- Gradle (the project includes the `gradlew` wrapper)
- Node.js and npm (for linting and formatting)

### Clone the Repository

```sh
git clone <repository-url>
cd codespaces-blank
```

### Build the Project

```sh
./gradlew build
```

### Run Tests

```sh
./gradlew test
```

## How to Contribute

### Reporting Bugs

Before filing a bug report, please check the existing issues to see if the
problem has already been reported. When creating a new issue, include:

- A clear and descriptive title
- Steps to reproduce the issue
- Expected behavior
- Actual behavior
- Any relevant logs or screenshots

### Suggesting Enhancements

Feature requests are welcome. Please open an issue describing:

- The enhancement you would like to see
- Why it would be useful to most users
- Any possible implementation ideas you have

### Submitting Changes

1. Fork the repository and create your branch from `main`.
2. Make your changes in a dedicated feature branch.
3. Ensure your code follows the project's style and conventions.
4. Add or update tests as needed.
5. Run the full test suite to verify nothing is broken.
6. Commit your changes with a clear, descriptive message.
7. Push your branch and open a pull request.

## Pull Request Guidelines

- Keep pull requests focused on a single change or feature.
- Provide a clear description of what your change does and why.
- Reference any related issues in the pull request body.
- Ensure all checks pass, including linting and tests.
- Be responsive to review feedback.

## Code Style

This project uses Spotless for code formatting. Run the formatter before
submitting your changes:

```sh
./gradlew spotlessApply
```

Markdown files are linted with markdownlint. Run the linter to check your
documentation:

```sh
npx markdownlint-cli2 --config .markdownlint.yaml '**/*.md'
```

## Community

Please read and follow the [Code of Conduct](CODE_OF_CONDUCT.md) in all
interactions with the project and its community.

## License

By contributing to this project, you agree that your contributions will be
licensed under the project's license. See the [LICENSE](LICENSE) file for
details.
