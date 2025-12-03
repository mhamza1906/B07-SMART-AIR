# Smart Air: Asthma Management App

## 1. Project Overview

Smart Air is a mobile application designed to help children and their parents manage asthma more effectively. The app provides real-time guidance during asthma-related events, logs critical health data, and facilitates communication between the child, parent, and healthcare providers. The core functionality revolves around a "Triage Mode" that guides a child through assessing their symptoms and provides an appropriate action plan based on their personal best Peak Expiratory Flow (PEF) readings.

The project is built using native Android (Java) and leverages the Firebase suite for its backend, including Authentication, Firestore, and Realtime Database.

---

## 2. Core Features

The application is split into two main user experiences: one for the child and one for the parent.

### Child Features:
*   **Triage Mode:** A step-by-step process to assess symptoms during an asthma event. The child can log:
    *   Red flag symptoms (e.g., difficulty talking, blue lips).
    *   Their current PEF reading.
    *   Whether they have recently taken rescue medication.
*   **Dynamic Action Plans:** Based on the entered PEF value compared to their personal best, the app displays a Green, Yellow, or Red zone action plan.
*   **Emergency State:** Automatically triggers an emergency state if red flag symptoms are checked or if a 10-minute timer expires without improvement, displaying a prominent "Call Emergency Services" button.
*   **PEF Logging:** A dedicated screen for children to log their daily PEF readings, which are stored and used to track their personal best.
*   **Medicine Log:** A table containing information regarding, the type of the medicine the child took, the dose, time, pre breath rating, post breath rating and the post checkup. This gets updated everytime the child enters the take medicine activity

### Parent Features:
*   **Parent Dashboard:** A central hub for parents to view summaries of their children's recent asthma activity.
*   **Alerts System:**
    *   **Real-time Toasts:** Parents receive an in-app toast notification when they open the app if new alerts have been generated since they last checked.
    *   **Alerts History Page:** A dedicated screen that displays a chronological list of all alerts, with the newest at the top.
    *   **Alert Triggers:** Alerts are automatically generated for:
        1.  The start of a child's triage session.
        2.  The triggering of an emergency state.
        3.  A high frequency of use (3 or more triage incidents within 3 hours).
*   **Account Management:** Parents can create and manage accounts for their children.
*   **Inventory Log:** Allows the parent to see information regarding the amount left and the purchase and expiry date for the controller and rescue medicine in the form of a simple table

---

## 3. Technical Stack & Dependencies

This project utilizes a modern Android development stack.

*   **Language:** **Java**
*   **Platform:** **Android (minSDK specified in Gradle)**
*   **Architecture:** Follows a standard Android Activity/Fragment structure.
*   **Backend & Database:**
    *   **Firebase Authentication:** For user login and account management.
    *   **Cloud Firestore:** The primary database for storing structured, queryable data like:
        *   `parent-child` relationships.
        *   `triage_incidents` and their associated `incident_log`.
        *   `parent_alerts` for the parent's alert page.
        *   `PEF` personal bests and daily logs.
    *   **Firebase Realtime Database:** Used for storing user profile information (`/users/{userId}`) such as names and the parent-child ID link.
*   **Key Libraries:**
    *   **Firebase BoM (`firebase-bom`):** Ensures all Firebase libraries are version-compatible.
    *   **AndroidX Libraries (`appcompat`, `recyclerview`, `constraintlayout`):** For UI components and modern Android features.
    *   **Google Material Design (`material`):** For UI elements like BottomSheetDialogs.
    *   **MPAndroidChart:** For displaying graphical representations of PEF data.
    *   **Glide:** For image loading (though may be used for future avatar features).

---

## 4. Firebase Database Structure

The app relies on a specific data structure across two Firebase databases.

### Cloud Firestore:
*   `parent-child/{parentId}/child/{childId}`
    *   Stores the link between a parent and their children.
*   `triage_incidents/{childId}/incident_log/{incidentId}`
    *   Stores detailed logs for each triage session, including timestamps, PEF values, symptoms, and medication status.
*   `parent_alerts`
    *   A top-level collection where alert documents are created for the parent to view. Each document contains a message, timestamp, and `parentId`.
*   `PEF/{childId}`
    *   Stores the child's Personal Best (`PB`) and a subcollection for daily PEF logs.

### Realtime Database:
*   `users/{userId}`
    *   Stores basic user profile information like `fName`, `lName`, `email`, and for child users, a `parentId` field.

---

## 5. Project Setup

To run this project, you will need to connect it to your own Firebase project.

1.  **Firebase Project:** Create a new project in the [Firebase Console](https://console.firebase.google.com/).
2.  **Add Android App:** Add a new Android app to your Firebase project. Make sure the package name matches the one in your `build.gradle.kts` file (`com.example.smart_air`).
3.  **Download `google-services.json`:** Download the generated `google-services.json` file and place it in the `app/` directory of your Android Studio project.
4.  **Enable Databases:**
    *   In the Firebase Console, go to the **Firestore Database** section and create a new database.
    *   Go to the **Realtime Database** section and create a new database.
5.  **Enable Authentication:** Go to the **Authentication** section, click "Get started," and enable the "Email/Password" sign-in method.
6.  **Build and Run:** Sync your Gradle files in Android Studio and run the app on an emulator or a physical device.

**Note:** The app expects the database structures outlined above to be created programmatically as users are created and interact with the features.


