# 🌴 Nexus Telemetry Engine

**Nexus Telemetry Engine** is a high-performance, Discord-based study and productivity tracking system. Rebuilt entirely from the ground up over a 48-hour sprint, this engine replaces a 4-year-old legacy architecture, closing out the EEE era with a robust, scalable, and fully dynamic backend.

It provides live study telemetry, academic portfolio management, and a dual-track RPG-style progression system to gamify deep work and focus.

---

## ✨ Core Features

*   **💎 Live Session Telemetry** 
    Track active focus time, break durations, and uptime dynamically. Sessions are managed seamlessly via interactive Discord buttons (Start, Pause, Resume, Telemetry, and End).
*   **📈 Dual-Track Progression System**
    *   **Semester Levels (XP):** Seasonal progression tracking your focus for the current academic term.
    *   **Account Ranks (RP):** Lifetime prestige tracking your total hours invested. Unlock custom status emojis (from 🤍 to the coveted 🍩 at Rank 250+).
*   **📚 Academic Portfolio Management**
    Safely separate active **Semester Subjects** from your permanent **Academic Record**. The bot actively prevents cross-contamination and validates inputs dynamically.
*   **🔥 Intelligent Streak Tracking**
    Tracks active and longest daily study streaks with smart break-detection logic to accommodate natural sleep cycles and missed days.
*   **🍸 Executive Dashboards**
    Generates responsive, beautiful embeds detailing lifetime focus, semester statistics, average session times, and module-specific data.

---

## 🛠️ Tech Stack

*   **Language:** Java
*   **API Wrapper:** [JDA (Java Discord API)](https://github.com/discord-jda/JDA)
*   **Database:** MongoDB (using a custom `DB` wrapper for seamless document mapping)
*   **Architecture:** Event-driven architecture utilizing Discord's Slash Commands, Autocomplete Interactions, and Component (Button) Listeners.

---

## 💻 Key Commands & Interactions

### Slash Commands
*   `/timer start [module]` — Initializes a pending focus session for a specific subject. Features dynamic, fuzzy-search autocomplete limited to your active semester.
*   `/timer remove-subject [destination] [code]` — Safely drop a subject from your `Semester` or permanently delete it from your `Academic Record`. Includes aggressive backend validation to prevent accidental deletions.
*   `/timer stats` — Displays the Executive Dashboards, showcasing your current level, active streaks, and time invested per module.

### Interactive Terminal (Buttons)
When a session is initialized, the bot provides a control terminal:
*   ▶️ **Start:** Begins recording time.
*   ⏸️ **Pause:** Halts the timer and begins logging recovery/break time.
*   ▶️ **Resume:** Ends the break and resumes the active study telemetry.
*   💎 **Telemetry:** Sends an ephemeral, real-time update of your current session stats without cluttering the channel.
*   ⏹️ **End Session:** Stops the session, calculates all XP/RP earned, updates streaks, checks for broken records, and processes level-ups.

---

## ⚙️ Logic Highlights

*   **Engineered Progression Math:** XP scales dynamically using a `100 + 150 * level^1.15` formula, ensuring smooth, rewarding level-ups that get progressively harder. Includes hard-cap logic to catch extreme overflow XP.
*   **O(1) Progression Queries:** Uses pre-calculated prefix sum arrays (`CUMULATIVE_XP`) to instantly calculate total required points for any rank.
*   **Bulletproof Validation:** "Never trust the client." The bot intercepts all dynamic dropdown selections and verifies database state before mutating user records, easily overriding Discord UI caching glitches.

---

## 🌅 A New Era

> *"The old foundation is gone. A new one is built."*

This is the new standard.
