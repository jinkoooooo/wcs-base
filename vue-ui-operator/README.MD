# NEAR WCS Operator Application(IONIC Vue)

## Table of Contents

- [NEAR WCS Operator Application(IONIC Vue)](#near-wcs-operator-applicationionic-vue)
  - [Table of Contents](#table-of-contents)
  - [Getting Started](#getting-started)
  - [App build 환경 구성](#app-build-환경-구성)
    - [xcode 설치](#xcode-설치)
    - [Android 환경](#android-환경)
      - [Android Studio](#android-studio)
      - [Android SDK](#android-sdk)
    - [App Build](#app-build)
    - [engine package 충돌 처리](#engine-package-충돌-처리)
  - [기본 기능](#기본-기능)
    - [\[Menu\]](#menu)
    - [\[기타\]](#기타)

## Getting Started

- Install the ionic CLI globally: `npm install -g ionic`
- repository: `[git clone https://github.com/NearSolutionLab/near-wcs.git](https://github.com/NearSolutionLab/near-wcs.git)`.
- Run `cd vue-ui-operator`
- Run `yarn install` from the project root.
- Run `ionic serve` in a terminal from the project root.

## App build 환경 구성

### xcode 설치

```
  xcode-select --install
```

### Android 환경

- Android Studio
- Android SDK

#### Android Studio

[Android Studio 다운로드](https://developer.android.com/studio). Capacitor 5 지원 버전 Android Studio 2022.2.1. 이상

#### Android SDK

Android SDK package를 설치해야 한다.

Android SDK Tools을 설치해야하며 Android SDK Platforms for API 22이상 버전 필요.

**Tools -> SDK Manager** -> **SDK Platforms** tab:

### App Build

- android platform 동기화
  ```
    yarn cap add android
  ```
- build
  ```
    ionic build
  ```
- web to native
  ```
    yarn cap sync
  ```
- run
  ```
    yarn cap run android
  ```

### engine package 충돌 처리

yarn config set ignore-engines true
https://github.com/m0dch3n/vue-cli-plugin-cordova

```bash
  npm run cordova-serve-android # Development Android
  npm run cordova-build-android # Build Android
  npm run cordova-build-only-www-android # Build only files to src-cordova
```

## 기본 기능

### [Menu]

- Setting의 category를 변경하면 서버에서 Menu를 조회하여 화면에 표현

### [기타]

- dynamic font 관련
- rem = 숫자 \* html font-size
- rm = 숫자 \* parent font-size
