# ScamLens – Scam Message Detector (website)

A Java 21 website that checks a message for scam warning signs, gives it a risk
score from 0 to 100, explains why, and keeps a private history.

**No Maven and no downloads.** It uses only Java 21 and Java's built-in web server.

## Run it on your laptop

1. Check Java 21 is installed: open a terminal and type `java -version`.
2. Double-click **`run.bat`** (or type `.\run.bat` in the VS Code terminal).
3. When it says *ScamLens is running*, open **http://localhost:8765** in your browser.
4. To stop it, press **Ctrl + C** in that window.

Mac or Linux: `sh run.sh`

## The Swing desktop version

The same app is also available as a desktop window built with Java Swing.
It uses the same detection engine and the same history file as the website.

1. Double-click **`run-desktop.bat`** (Mac/Linux: `sh run-desktop.sh`).
2. The ScamLens window opens. Close it to stop.

Its code is in `src/main/java/com/scamlens/desktop/`:

```
desktop/
├── DesktopApp.java     the window, the navigation bar, starts everything
├── ScannerPanel.java   screen 1: message box and Check message
├── ResultPanel.java    screen 2: verdict, highlighted message, reasons, tips
├── DashboardPanel.java screen 3: totals, risk mix, recent checks
├── HistoryPanel.java   screen 4: table with Delete and Delete all
├── Theme.java          colours, fonts, small helpers
├── Page.java           scrolling page with the blue glow
├── RoundedPanel.java   rounded "glass" card
├── PillButton.java     rounded buttons
├── Chip.java           small rounded tags
├── WrapText.java       paragraphs that wrap
└── LogoIcon.java       the logo, drawn with Java 2D
```

## Run the tests

Double-click **`test.bat`** (Mac/Linux: `sh test.sh`). It runs all 51 tests and
prints PASS or FAIL for each one.

## Put it online (free)

You need a free GitHub account and a free Render account.

1. **Upload to GitHub.** On github.com, create a new repository called `scamlens`.
   Click *uploading an existing file* and drag in everything inside this folder
   (not the `out`, `out-test` or `data` folders). Click *Commit changes*.
2. **Create the website on Render.** On render.com, sign in with GitHub, then
   *New* → *Web Service* → pick the `scamlens` repository.
   Render sees the `Dockerfile` and sets everything up. Choose the **Free** plan and
   click *Create Web Service*.
3. Wait a few minutes. Render gives you a link like
   `https://scamlens.onrender.com`. That's your live website.

Things to know about the free plan:
- The site goes to sleep after about 15 minutes with no visitors. The next visit
  takes about a minute to wake it up, so open it before you present.
- The scan history is cleared whenever Render restarts the site.
- Each visitor sees only their own history. The site gives every browser a random
  id in a cookie and saves each check with that id. A different browser, a private
  window or clearing cookies starts a new, empty history.
- The times are shown in Indian time (the Dockerfile sets `-Duser.timezone=Asia/Kolkata`).

## Project structure

```
src/main/java/com/scamlens/
├── Main.java                 starts the website
├── web/                      the website (presentation layer)
│   ├── WebServer.java        receives browser requests and decides what to do
│   ├── Pages.java            builds the HTML for each page
│   ├── Html.java             escapes user text so scripts can't run
│   └── FormData.java         reads what the browser sent
├── analyzer/                 MessageAnalyzer (runs everything), RiskScorer
├── detector/                 Detector interface, KeywordDetector + 5 detectors, URLAnalyzer
├── model/                    Category, RiskLevel, DetectionResult, AnalysisResult, ScanRecord, DashboardStats
├── service/                  HistoryService (saves history in data/scan-history.txt)
└── util/                     InputValidator, PreviewMaker, TextHighlighter, SampleMessages
src/test/java/com/scamlens/
└── TestRunner.java           all 51 tests
public/
├── styles.css                the dark theme
└── app.js                    character counter, Ctrl+Enter, delete confirmation
```

## How a scan works

Browser form → `POST /analyze` → `WebServer.handleAnalyze()` → `MessageAnalyzer.analyze()`
→ six detectors → `RiskScorer` → `AnalysisResult` → `HistoryService.save()` →
`Pages.results()` builds the result page → sent back to the browser.
