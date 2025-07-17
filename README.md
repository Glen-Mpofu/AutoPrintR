# AutoPrintR

jpackage code

jpackage ^
  --name AutoPrintR ^
  --input "jar directory" ^
  --main-jar AutoPrintR.jar ^
  --main-class autoprintr.AutoPrintR ^
  --type exe ^
  --java-options "-Xmx512m" ^
  --win-menu ^
  --win-shortcut ^
  --verbose ^
  --dest "destination for the exe" ^
  --app-version "1.0.0" ^
  --vendor "TVA" ^
  --description "AutoPrintR automatically monitors and prints files from a selected folder." ^
  --win-dir-chooser ^
  --win-per-user-install



