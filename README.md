📄 AutoPrintR Documentation
📘 Overview
AutoPrintR is a lightweight Windows application that automatically prints files added to a specified folder. It monitors the folder in real time and sends printable files to the default printer without user intervention.
________________________________________
🚀 Features
•	🔍 Monitors a selected folder for new files
•	🖨️ Automatically prints PDF, Office, text, and image files
•	📋 Maintains a log of printed files to avoid duplicates
•	🛑 Prevents multiple instances from running simultaneously
•	🧾 Installs with a default installation timestamp
•	🖥️ Minimizes to system tray (if supported)
________________________________________
🧰 Requirements
•	Windows 7 or later
•	Default printer installed
________________________________________
📦 Installation
1.	Run the installer
2.	Check for the app on your Desktop and double click it to open it.
3.	The app will launch and ask you to choose a folder to monitor.
4.	AutoPrintR minimizes to the system tray (if supported). Right-click the tray icon to open or exit.
________________________________________
 
📄 Supported File Types
AutoPrintR automatically prints these file types:
File Type	Method Used
PDF	SumatraPDF
DOC, DOCX	PowerShell Script + Office
XLS, XLSX	PowerShell Script + Office
PPT, PPTX	PowerShell Script + Office
TXT	Java Desktop Print
JPG, PNG, BMP	Java Image Print

________________________________________
🖱️ How to Use
Initial Setup
1.	Launch the application.
2.	Choose a folder to monitor.
3.	Drop files into the folder: printing will happen automatically.
4.	View printed logs inside printed_files.txt.
Changing Folder
•	Click "Choose/Change Folder" in the GUI.
•	Select a new folder and confirm.
System Tray (if available)
•	App minimizes to tray when closed.
•	Right-click tray icon to Show or Exit.
________________________________________
⚙️ Technical Details
•	Prevents multiple instances via a local port lock (port_number.txt)
•	Uses Java WatchService API to detect new files
•	Skips old files (based on installation timestamp)
•	Keeps track of already printed files
________________________________________
⚠️ Troubleshooting
Problem	Solution
App doesn't launch	Restart your machine and try launching it again
Files aren't printing	Check if your default printer is set and online
PDF not printing	Check if your default printer is set and online
Office files not printing	Confirm Microsoft Office is installed and PowerShell is enabled
App "disappears" after closing window	System Tray might not be supported; restart app manually
Tray icon not visible on Windows 7	Modify Windows tray settings 
________________________________________
🛑 Limitations
•	No support for multi-page print previews
•	No UI feedback on printer errors
•	System Tray required for full UX (auto-hide, tray menu)
________________________________________
👨‍💻 Developer Notes
•	Written in Java 8+
•	Uses Java AWT/Swing for UI and tray
•	File monitoring via WatchService
•	Office printing powered by PowerShell scripts
•	Can be extended to support:
o	Multiple folders
o	Printer selection
o	Print settings customization
________________________________________
📜 License
AutoPrintR is a proprietary software product. Unauthorized reproduction, distribution, reverse-engineering, modification, or resale of this application is strictly prohibited and may result in legal action under applicable laws.
By installing and using AutoPrintR, you agree to the following terms:
•	You may use this software for personal or internal business purposes only.
•	You may not decompile, disassemble, or reverse-engineer any part of the software.
•	Redistribution, either in original or modified form, is not allowed without explicit written permission from the developers.
•	You may not use this software to offer commercial printing services unless granted a commercial license.
The developers of AutoPrintR are not responsible for any data loss, printing errors, or damages arising from the use or misuse of the application.
All rights reserved © 2025 Tshepo Mpofu & Vutlhari Maswanganyi	 
________________________________________
📞 Contact / Support
For issues, email: mpofuglen23@gmail.com or vutlhari23maswanganyi@gmail.com
Or visit: https://github.com/Glen-Mpofu/AutoPrintR/issues


