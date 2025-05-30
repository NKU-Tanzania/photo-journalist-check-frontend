
# siu_frontend repository

A Kotlin-based frontend application for the DPVSCJ project.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Scripts & Commands](#scripts--commands)
- [Contributing](#contributing)
- [License](#license)

## Overview

**siu_frontend** is the frontend component of the DPVSCJ project. Built entirely in Kotlin, this application aims to provide a robust and performant user interface, leveraging modern development practices.

## Features

- 100% Kotlin codebase
- Modular architecture
- Responsive UI
- Easy integration with backend services
- [Add more features as appropriate]

## Getting Started

### Prerequisites

- [JDK 11+](https://adoptopenjdk.net/)
- [Gradle](https://gradle.org/install/) (if not using the wrapper)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Msumali/siu_frontend.git
   cd siu_frontend
   ```

2. **Build the project:**
   ```bash
   ./gradle build
   ```

3. **Run the application:**
   ```bash
   ./gradle run
   ```

## Project Structure

```text
citizen-journalism-platform/
├── mobile-app/
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/nkutanzania/journalism/
│   │   │   │   │   ├── ui/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├──  BaseActivity.kt
│   │   │   │   │   ├──  LoginActivity.kt
│   │   │   │   │   ├──  RegisterActivity.kt
│   │   │   │   │   ├──  CameraActivity.kt
│   │   │   │   │   ├──  UploadResultActivity.kt
│   │   │   │   │   ├── UploadedImagesActivity.kt
│   │   │   │   │   ├──  ImageViewerActivity.kt
│   │   │   │   │   ├── ImageRepository.kt
│   │   │   │   │   ├── ImageModel.kt
│   │   │   │   │   ├── ApiService.kt
│   │   │   │   │   └── ApiUtils.kt
│   │   │   │   └── res/
│   │   │   │       └── layout/
│   │   │   │           ├── activity_main.xml
│   │   │   │           ├── activity_login.xml
│   │   │   │           ├── activity_register.xml
│   │   │   │           ├── activity_camera.xml
│   │   │   │           ├── activity_upload_result.xml
│   │   │   │           ├── activity_uploaded_images.xml
│   │   │   │           ├── activity_image_viewer.xml
│   │   │   │           └── item_image.xml
│   │   │   └──AndroidManifest.xml
│   │   ├── build.gradle
│   │   └── proguard-rules.pro
│   └── gradle/
            
```

## Scripts & Commands

- `./gradle build` – Build the project
- `./gradle run` – Run the application

## Contributing

1. Fork the repository
2. Create your feature branch: `git checkout -b feature/YourFeature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin feature/YourFeature`
5. Open a pull request

## License

[GNU](LICENSE)
