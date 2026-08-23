<div align="center">

  <img src="app/src/main/res/drawable/img_logo_icon.png" />

  # iAttend

  **A free, offline-first attendance and timetable tracker for Android.**  
  Built with Jetpack Compose, Room, and Hilt.

  <p>
    <a href="#"><img src="https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-2ECC71?style=for-the-badge&logo=android&logoColor=white" alt="API Level" /></a>
    <a href="#"><img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
    <a href="#"><img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
    <a href="#"><img src="https://img.shields.io/badge/Material%20You-795548?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material You" /></a>
  </p>

  <p>
    <a href="https://github.com/shuaib-07/iAttend/releases"><img src="https://img.shields.io/github/v/release/shuaib-07/iAttend?style=for-the-badge&color=8E44AD&logo=github" alt="Release" /></a>
    <a href="https://github.com/shuaib-07/iAttend/stargazers"><img src="https://img.shields.io/github/stars/shuaib-07/iAttend?style=for-the-badge&color=F1C40F&logo=apachespark" alt="Stars" /></a>
    <a href="https://github.com/shuaib-07/iAttend/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0%20%2F%20MIT-blue?style=for-the-badge" alt="License" /></a>
  </p>

  <p>
    <a href="https://github.com/shuaib-07/iAttend/releases/latest">
      <img src="https://img.shields.io/badge/Download-Latest%20APK-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
    </a>
  </p>

</div>

---

## Why iAttend?

If you've ever tried using an attendance tracking app in college (especially in India), you know the drill: **almost every existing app assumes your schedule stays identical for the entire year.**

In reality, college schedules are pure chaos:
- Timetables change halfway through the semester, and apps force you to delete your timetable or wipe your entire attendance record just to update it.
- Sudden extra classes get scheduled on weekends or off-hours with no clean way to log them.
- Recurring off-days, public holidays, and subject credit weightages are rarely handled together.
- And of course, the most important question every student asks: *"How many classes can I afford to bunk right now without falling below the minimum percentage?"*

iAttend was built out of this exact frustration. It’s designed to handle dynamic, real-world college timetables—giving you total control over schedule swaps, extra classes, and safe bunk calculations so you can attend what you need to and stay fully eligible.

---

<div align="center">

## 🚀 Features

</div>

| 🗓️ **Multiple Timetable Versions** | 🧮 **Bunk Calculator** |
| :--- | :--- |
| Switch schedules mid-semester seamlessly without resetting past attendance history or total class counts. <br><br> `Timetables` `Mid-term Swaps` `History Preserved` | See exactly how many classes you can afford to skip (or need to attend) to meet your target percentage. <br><br> `Target %` `Real-time` `Safe Bunking` |
| ➕ **Extra Classes & One-Offs** | 🏖️ **Holidays & Off-Days** |
| Schedule ad-hoc or sudden weekend lectures on the fly without breaking your base timetable. <br><br> `One-offs` `Ad-hoc` `Weekend Sessions` | Configure weekly off-days and public holidays once so they don't count against your attendance. <br><br> `Recurring Off-Days` `Semester Breaks` |
| 📝 **Exams, Tests & Portions** | 🔔 **Smart Notifications** |
| Track test dates, marks, syllabus portions, and credit weightages with dedicated reminders. <br><br> `Exam Tracker` `Subject Weightage` `Syllabus` | Get alerts right before class starts, immediately after it ends to log attendance, and ahead of exams. <br><br> `Class Alerts` `Post-class Prompt` `Exam Reminders` |
| 📱 **Home Screen Widget** | 🎨 **Dynamic Themes** |
| Glance at your upcoming lectures and class schedule without opening the app. <br><br> `Android Widget` `Quick Glance` | Supports Material You dynamic colors, Light, Dark, pure AMOLED black, and accent palettes. <br><br> `Material You` `AMOLED` `Custom Accents` |
| 📦 **Backup & Share** | 🔒 **100% Offline & Private** |
| Export your timetable as a portable backup file to easily share schedules with classmates. <br><br> `Import / Export` `Share Schedule` | No account required, zero tracking, and no analytics. All your data stays strictly on your device. <br><br> `Room DB` `No Analytics` `Local-Only` |

---

## 🎨 Design Inspirations

Special thanks to the open-source community and the following projects for UI and architectural inspiration:

* [Cashiro](https://github.com/ritesh-kanwar/Cashiro) by ritesh-kanwar
* [minus](https://github.com/isaacsa51/minus) by isaacsa51
* [RvSystem-Monitor](https://github.com/Rve27/RvSystem-Monitor) by Rve27
* [Attendo](https://github.com/jarvis1704/Attendo) by jarvis1704

---

## 🛠️ Building & Installation

### Requirements
* **Android OS:** Compatible with **Android 7.0 (Nougat, API Level 24)** and above
* **JDK:** Version 11 or higher
* **Android SDK:** `compileSdk 36`, `minSdk 24`

### Build from Source

```bash
# Clone the repository
git clone [https://github.com/shuaib-07/iAttend.git](https://github.com/shuaib-07/iAttend.git)
cd iAttend

# Build debug APK
./gradlew assembleDebug