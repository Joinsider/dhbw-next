# Generate MSI
To generate a new msi file which can be used to install the app on windows 10 & 11,
run the following commands:

Clean old build artifacts:
```shell
./gradlew.bat clean
```

Package app to msi
```shell
./gradlew.bat :composeApp:packageMsi
```

# Generate MSIX
Open the MSIX package manager and generate a new msix package from the .msi