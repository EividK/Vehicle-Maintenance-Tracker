**VehicleHealth App**

**Overview**
--
VehicleHealth is an Android app designed to help vehicle owners efficiently track and manage their vehicles service history, maintenance reminders, and mileage. It simplifies vehicle maintenance, enhancing reliability and longevity.

**Features**
--
- Service History Management: Easily record and access your vehicles past services.

- Maintenance Reminders: Get notifications for upcoming/overdue services.

- VIN Decoding: Easily capture and decode VIN numbers using your devices camera.

- Firebase Integration: Uses Firebase for secure authentication and messaging services.

- Custom Validation: Specifically for Irish vehicle registration plates and mileage entries.

**Project Structure**
--

- src/main/java: Core application source code including models, controllers, components, and services.

- Models: Defines data structures, Vehicle, ServiceHistory, etc.

- Services: Contains logic for handling notifications, Firebase messaging, and reminders.

- Components: UI components and validation fields.

- src/main/AndroidManifest.xml: Android app configuration and permissions.

**Requirements**
--

Android Studio
Kotlin & Jetpack Compose

Firebase setup (Authentication and Messaging)

Installation

Clone this repository.

Open the project in Android Studio.

Sync Gradle dependencies.

Run the app on an emulator or a physical device.

**Usage**
--

Sign up or log in using Firebase Authentication.

Add your vehicle details.

Log service history and set maintenance reminders.


**Technologies Used**
---

**Android & Jetpack Compose:** Modern toolkit for building native Android UI.

**CameraX:** Simplified API for camera integration and image capturing.

**ML Kit Text Recognition:** Googles machine learning kit for extracting text from images for VIN decoding.

**Kotlin:** Primary programming language for robust and concise Android development.

**Firebase Firestore:** Cloud-hosted database for efficient data storage and retrieval.

**Firebase Authentication:** Secure user authentication and authorization.

**Firebase Cloud Messaging (FCM):** Push notifications for reminders and alerts.

**AlarmManager & BroadcastReceiver:** Scheduling and handling periodic reminders and notifications.

**Custom Validators & Formatters:** Input validation specifically to Irish registration plates and mileage tracking.

**Navigation Components:** Structured app navigation and flow management.



---

**Author:** EIVIDAS KIRSTUKAS

**KNumber:** K00275021

**Course:** SOFTWARE DEVELOPMENT SD4

**Date:** 30/04/2025
