@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%
node firestore_emulator_test\test_all_rules.js
