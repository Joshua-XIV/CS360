# CS 360 Event Tracker - Joshua Hernandez

## App Code Design Artifact

This repository contains the completed Event Tracker Android application developed for CS 360 Mobile Architecture and Programming.

---

## Reflection

**Briefly summarize the requirements and goals of the app you developed. What user needs was this app designed to address?**

Event Tracker was designed to help users organize and manage their personal events, deadlines, and appointments from a mobile device. The app needed to support secure account creation and login, a persistent database for storing event data, and a notification system to remind users of upcoming events. The core user need was a simple and reliable way to track events without losing data between sessions, while also receiving timely reminders without having to check the app manually.

**What screens and features were necessary to support user needs and produce a user-centered UI for the app? How did your UI designs keep users in mind? Why were your designs successful?**

The app required a login screen, a main event display screen, an add and edit event screen, an event detail screen, and a settings screen. The event display screen used filter tabs to separate upcoming, past, and deleted events so users could find what they needed quickly. Empty state messages were included to guide new users rather than showing a blank screen. The settings screen gave users control over notification preferences, reminder intervals, phone number management, and dark mode. Keeping the interface simple and limiting the number of steps to complete common actions made the designs successful.

**How did you approach the process of coding your app? What techniques or strategies did you use? How could those techniques or strategies be applied in the future?**

I approached coding by building one feature at a time and testing each before moving on. I separated concerns by keeping database logic in DatabaseHelper, session management in SessionManager, and scheduling logic in SmsScheduler. This made each class easier to maintain and debug independently. Breaking the app into focused, single-responsibility classes is a strategy that applies to any software project regardless of platform.

**How did you test to ensure your code was functional? Why is this process important, and what did it reveal?**

I used the Android Emulator throughout development to test each feature as it was completed. I tested both the SMS permission granted and denied paths to make sure the app continued functioning in both cases. I also tested edge cases like logging in with incorrect credentials, creating duplicate accounts, and editing or deleting events to verify reminders were properly rescheduled or canceled. Testing revealed a few cases where reminder scheduling needed to account for events already in the past, which I handled by skipping reminders with a trigger time earlier than the current time.

**Consider the full app design and development process from initial planning to finalization. Where did you have to innovate to overcome a challenge?**

The notification and reminder system required the most problem solving. Coordinating AlarmManager, BroadcastReceivers, and runtime permissions while also supporting multiple configurable reminder intervals per event was more complex than expected. I solved this by using a slot-based request code system to uniquely identify each alarm across all events and reminder intervals, and by centralizing all scheduling and cancellation logic in a single utility class so any part of the app could reliably manage reminders without duplicating code.

**In what specific component of your mobile app were you particularly successful in demonstrating your knowledge, skills, and experience?**

The database layer was where I felt most confident in the final result. The SQLite implementation went beyond basic CRUD by including salted and hashed password storage, soft deletion with a 24-hour retention window, event restoration, and versioned schema migrations. These features reflect real-world database design considerations and made the app more robust and user-friendly than a minimal implementation would have been.
