# Sway — interactive web preview

Sway is a mobile-first, clickable prototype for a mindful pause app. It demonstrates selecting several apps at once, toggling a pause on each, previewing a breath-and-choice screen, setting gentle rituals, and viewing sample insights. App choices and demo interactions are stored in the browser's local storage.

## Important scope note

This is a **web prototype**, not an installed Android app. It cannot detect when Instagram, TikTok, or another app opens, and its “Continue” action does not launch another app. The pause flow is simulated in the browser. The charts and initial counters are sample values. A real Android implementation would need native app-target detection and interruption behavior, app selection/usage permissions, clear disclosures and consent, device testing, and Google Play policy review.

## Preview it

### Run locally

1. Download and extract `Sway-Web-Preview.zip` (or use the project files directly).
2. Install Python 3 if it is not already available.
3. In a terminal, change into the extracted folder and run:

   ```sh
   python3 -m http.server 4173
   ```

4. Open `http://localhost:4173` in a modern browser.

On Windows, `py -m http.server 4173` can be used instead of `python3 -m http.server 4173`.

You can also open `index.html` directly, though serving the folder locally is the more reliable option. The prototype has no build step, package manager, or remote application backend.

## What to try

- **Today:** select “Try a pause,” then choose to continue or step away.
- **My apps:** add multiple apps at once, search the sample catalogue, turn pauses on/off, sort the list, or remove an app.
- **Rituals:** create a named reminder, choose days and time, and toggle it.
- **Insights:** view the sample progress dashboard.

Your demo data is saved in this browser only. Use the browser's site-data controls to reset it.
