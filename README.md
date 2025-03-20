# batch-load-test
This is a simple test harness to demonstrate throughput of a batch load into CRDB using single statement, multi-statement, and multi-value transactions

## Environment Setup
To run the application you'll need Java, Maven and a few other tools installed.  The following steps can be followed on MacOS Silicon or similar.

### Bash
```
brew install bash
ls "$(brew --prefix)/bin/bash"
cat /etc/shells
echo "$(brew --prefix)/bin/bash" | sudo tee -a /etc/shells;
chsh -s "$(brew --prefix)/bin/bash"
echo $BASH_VERSION
```

### Homebrew
```
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"
echo >> ${HOME}/.bash_profile
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ${HOME}/.bash_profile
eval "$(/opt/homebrew/bin/brew shellenv)"
brew help
```

### Cockroach
```
brew install cockroachdb/tap/cockroach
```

### IntelliJ IDEA
Download and install the [Community Edition](https://www.jetbrains.com/idea/download/download-thanks.html?platform=macM1&code=IIC)

### Java 11
```
brew install openjdk@11
sudo ln -sfn /opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-11.jdk
java --version
```

### Java 17
```
brew install openjdk@17
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
java --version
```

### Maven
```
brew install maven
mvn -v
```

### Postico (Optional)
```
brew install --cask postico
```

## Launch the App
You can execute the load test against a remote CRDB cluster, you'll just need to update the connection string url and user credentials in the batch-processor/src/resources/application.properties file.
```
# Dev
%dev.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:26257/defaultdb?sslmode=disable
%dev.quarkus.datasource.username=root
```

Or you can launch a single-node instance locally and the default connection string parameters should work
```
cockroach start-single-node --insecure --store=/tmp/cockroach-data
```

Then run the maven command to start the application.
```
cd batch-processor
mvn quarkus:dev
```

Open http://localhost:8090/service in a browser, and you should be presented with a form to submit simple batch workloads.  After the test is submitted the screen will refresh to show some basic stats for the batch process.

<img width="1324" alt="image" src="https://github.com/user-attachments/assets/525ac09e-9ef3-453f-ba76-28a26b436214" />


You can also monitor the workload from the CRDB dashboard, i.e. http://localhost:8080/ if running cockroachdb locally.

<img width="1192" alt="image" src="https://github.com/user-attachments/assets/c98ed0d9-385f-450a-8024-e4dca32f540c" />

