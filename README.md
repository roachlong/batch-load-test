# batch-load-test
This is a simple test harness to demonstrate throughput of a batch load into CRDB using single statement, multi-statement, and multi-value transactions

Clone the repository to your local workspace.  If you need to install Git see the Enviornment Setup below.
```
git clone https://github.com/roachlong/batch-load-test.git
```

## Environment Setup
To run the application you'll need Java, Maven and a few other tools installed.  The following steps can be followed on MacOS Silicon, Windows or similar environments.

<details>
  <summary>MacOS</summary>

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

### Git
```
brew install git
```

If this is your first time running git on your workstation then you'll want to set your global config.
```
git config --global user.name "Your Name"
git config --global user.email "youremail@yourdomain.com"
git config --list
```

### IntelliJ IDEA
Download and install the [Community Edition](https://www.jetbrains.com/idea/download/?section=mac)

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

### Docker
First install the dependencies for colima
```
brew install docker
docker --version
brew install qemu
bres install lima
brew install colima
colima start --edit
# look for Network configurations and change the address setting to true
# network:
#   address: true
# <esc>:wq
colima status
docker version
docker context ls
```

Then update your system properties to set the docker socker override and host values
```
echo -e '\nexport TEST_CONTAINERS_DOCKER_SOCKER_OVERRIDE="/var/run/docker.sock"' >> ~/.bashrc
echo -e 'export DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock"' >> ~/.bashrc
source ~/.bashrc
```

And install the plugin for docker compose
```
DOCKER_CONFIG=${DOCKER_CONFIG:-$HOME/.docker}
mkdir -p $DOCKER_CONFIG/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.32.4/docker-compose-darwin-aarch64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose
docker compose version
```

Then we need to install the buildx plugin
```
DOCKER_CONFIG=${DOCKER_CONFIG:-$HOME/.docker}
curl -SL https://github.com/docker/buildx/releases/download/v0.20.1/buildx-v0.20.1-darwin-arm64 -o $DOCKER_CONFIG/cli-plugins/docker-buildx
chmod +x $DOCKER_CONFIG/cli-plugins/docker-buildx
docker buildx install
```

### GraalVM (optional)
Find and download the package for the respective Java version you want from https://github.com/graalvm/graalvm-ce-builds/releases, i.e. for [Java 23](https://download.oracle.com/graalvm/23/latest/graalvm-jdk-23_macos-aarch64_bin.tar.gz)
```
cd ~/Downloads
tar -xzf graalvm-jdk-23_macos-aarch64_bin.tar.gz
sudo xattr -r -d com.apple.quarantine graalvm-jdk-23.0.2+7.1/
sudo mv graalvm-jdk-23.0.2+7.1/ /Library/Java/JavaVirtualMachines
/usr/libexec/java_home -V
export PATH=/Library/Java/JavaVirtualMachines/graalvm-jdk-23.0.2+7.1/Contents/Home/bin:$PATH
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-23.0.2+7.1/Contents/Home
java -version
```

**Note**: building a native image with GraalVM requires a lot of memory which will need to be temporarily allocated to colima. FYI, it is also recommended to stop other processes before running the native image build.
```
colima stop
colima start --cpu 4 --memory 8
mvn package -Dquarkus.container-image.build=true -Dquarkus.package.type=native -Dquarkus.native.container-build=true -Dquarkus.container-image.tag=native -Dquarkus.native.native-image-xmx=8g
colima stop
colima start --cpu 2 --memory 2
```

### Postico (optional)
```
brew install --cask postico
```
</details>

<details>
  <summary>Windows</summary>
  
### Cockroach
You can download and extract the latest binary from [here](https://www.cockroachlabs.com/docs/releases), then add the location of the cockroach.exe file (i.e. C:\Users\myname\AppData\Roaming\cockroach) to your Windows Path environment variable.

## Git
Download and install Git from https://git-scm.com/download/win.  After installation completes go to your Start menu and open a PowerShell terminal.  If this is your first time running git on your workstation then you'll want to set your global config.
```
git config --global user.name "Your Name"
git config --global user.email "youremail@yourdomain.com"
git config --list
```

### IntelliJ IDEA
Download and install the [Community Edition](https://www.jetbrains.com/idea/download/?section=windows)

### Java
Follow the instructions to download and install the latest JDK from https://www.oracle.com/java/technologies/downloads/#jdk24-windows, more detailed instructions are available [here](https://docs.oracle.com/en/java/javase/24/install/installation-jdk-microsoft-windows-platforms.html)

### Maven
Open a PowerShell window and you should be able to use the Chocolatey package manager
```
choco install maven
mvn -v
```

### Docker
You can install Docker Desktop and follow the instructions outlined [here](https://www.geeksforgeeks.org/how-to-install-docker-on-windows/).

</details>

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

Open http://localhost:8090/batch/service in a browser, and you should be presented with a form to submit simple batch workloads.  After the test is submitted the screen will refresh to show some basic stats for the batch process.

<img width="1324" alt="image" src="https://github.com/user-attachments/assets/525ac09e-9ef3-453f-ba76-28a26b436214" />


You can also monitor the workload from the CRDB dashboard, i.e. http://localhost:8080/ if running cockroachdb locally.

<img width="1192" alt="image" src="https://github.com/user-attachments/assets/c98ed0d9-385f-450a-8024-e4dca32f540c" />

