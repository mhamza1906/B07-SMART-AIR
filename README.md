# Smart Air: Asthma Management App

## 1. Project Overview

Smart Air is a mobile application designed to help children and their parents manage asthma more effectively. The core functionality of the app revolves around providing real-time guidance during asthma-related events, logging critical health data, and facilitate communication between the child, parent, and healthcare providers. 

The project is built using native Android (Java) and leverages the Firebase suite for its backend, including Authentication, Firestore, and Realtime Database.

---

## 2. Core Features

The application is split into Three main user experiences: one for Child, one for Parent, one for Provider

### Child Features:
*   **Triage Mode:** A step-by-step process to assess symptoms during an asthma event. The child can log:
    *   Red flag symptoms (e.g., difficulty talking, blue lips).
    *   Their current PEF reading.
    *   Whether they have recently taken rescue medication.
*   **Dynamic Action Plans:** Based on the entered PEF value compared to their personal best, the app displays a Green, Yellow, or Red zone action plan.
*   **Emergency State:** Automatically triggers an emergency state if red flag symptoms are checked or if a 10-minute timer expires without improvement, displaying a prominent "Call Emergency Services" button.
*   **PEF Logging:** A dedicated screen for children to log their daily PEF readings, which are used to display their daily zone and record zone change history
*   **Medicine Log:** A table containing information regarding, the type of the medicine the child took, the dose, time, pre breath rating, post breath rating and the post checkup. This gets updated everytime the child enters the take medicine activity
*   **Daily Check Ins:** A screen for children to report daily symptoms and triggers, which are stored and displayed to Parents.
*   **Planned Schedule:** A page where child users can see the days where a planned controller session is planned, as well as if they have already completed those controller sessions
*   **Take Medicine:** allows children to record medication intake. They select the medication type, complete a pre-dose check, follow an interactive technique helper to ensure proper usage, and finish with a post-dose check. All data is recorded to medicine log.
*   **Achievements/Streak:** Children can earn badges based on different, parent-configurable achievements(eg: 0 rescue days per Month) and they also have a streak feature for consecutive technique helper sessions and planned controller days.





### Parent Features:
*   **Parent Dashboard:** A central hub for parents to view summaries of their children's recent asthma activity.
*   **Dashboard Tiles:** Displays a summary card for each child, providing a quick, at-a-glance overview of their key asthma metrics including weekly rescue count, last rescue use, Today's zone, and a graph showing daily PEF % for the past week/month.
*   **Alerts System:**
    *   **Real-time Toasts:** Parents receive an in-app toast notification when they open the app if new alerts have been generated since they last checked.
    *   **Alerts History Page:** A dedicated screen that displays a chronological list of all alerts, with the newest at the top.
    *   **Alert Triggers:** Alerts are automatically generated for:
        1.  The start of a child's triage session.
        2.  The triggering of an emergency state.
        3.  A high frequency of use (3 or more triage incidents within 3 hours).
*   **Account Management:** Parents create and manage accounts for their children.
*   **Inventory Log:** Allows the parent to see information regarding the amount left and the purchase and expiry date for the controller and rescue medicine in the form of a simple table
*   **Go to Child Dashboard:** A feature allowing parents to directly access their children's dashboard from the parent dashboard. Allows children to navigate to their own dashboard without credential authentication.
*   **Manage Child Activities:** A menu consisting of features like configure PEF settings, creating a planned controller schedule, configure rewards and perform daily check in
*   **Planned Schedule:** Allows the parent to set planned controller sessions for chosen days, determining the number of controller sessions on those days for linked children to follow.
*   **Configure PEF Settings:** Lets the Parent set the PEF PB for their child and toggle the date range for the PEF % graph on dashboard tiles.
*   **Configure Rewards:** Allows the Parent to set the benchmark for badges and achievements their children can earn.
*   **Perform Daily Check In:** Lets the Parent to perform daily symptom and trigger check in on behalf of their child.
*   **View Child Summary:** A menu consisting of summaries for different Child Data such as Rescue Logs, Controller Adherance, Symptoms and Triggers summary charts, Triage Incidents, Daily PEF Zone chart and a History Browser, with in-page checkboxes for quick datasharing with Provider.
*   **History Browser:** A screen allowing parents to see their Child's PEF Zone Change History and and Daily Check In history, which they can filter by Date range, Triggers and Symptoms. Parents also have the option of exporting history data as PDF/CSV files to externally share with Providers.
*   **Provider Report:** Allows the Parent to export a PDF of important details for a child user, including rescue frequency, controller adherence, zone distribution, symptom burden, and notable triage incidents within a 3-6 month range as decided by the parent.
*   **In App Data sharing with Providers:** Enables parents to search for healthcare providers within the app, use intuitive toggles to select specific data to share, and generate a secure, time-limited link for read-only access which can be revoked at any time.






### Provider Features:
*   **Provider Dashboard:** Provides healthcare providers with a centralized view of all child patients whose parents have actively shared health data, allowing for quick access to monitor their progress and history.
*   **Code/Link Sharing:**  A central dashboard for healthcare providers to view and manage all incoming data sharing requests from parents in the form of codes and links, and a dedicated area to enter provided codes to securely link accounts.
*   **Child Summary Menu:** Offers healthcare providers a detailed, at-a-glance dashboard for each linked child, visualizing key health metrics including PEF zone distribution, rescue medication usage, controller adherence percentages, symptom burdens, and notable triage incidents.


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
    *   **Glide:** For image loading (though may be used for future avatar features).
    *   **MPAndroidChart:** For displaying graphical representations of PEF and Zone Distribution data.
    *   **iText7:** For generating and exporting user history data as PDF files.



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

