@echo off

jar -cvf ../plain/rudp_plain.jar -C ../bin/ .

java -jar proguard.jar @config.pro

pause