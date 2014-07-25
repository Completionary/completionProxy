completionProxy
===============

Proxy implementing an interface to the suggestion index (elastic search) and the analytics backend.

# Dependencies
To make this code compilable you have to generate the thrift files. To install the thrift compiler please see the following links:

## Debian Wheezy/Ubuntu
```bash
sudo apt-get install git build-essential cmake pkg-config libboost-dev libboost-test-dev \
libboost-program-options-dev libevent-dev automake libtool flex bison pkg-config \
libssl-dev libsoup2.4-dev libboost-system-dev libboost-filesystem-dev \
libogg-dev libtheora-dev libasound2-dev libvorbis-dev libpango1.0-dev \
libvisual-0.4-dev libffi-dev libgmp-dev

git clone https://git-wip-us.apache.org/repos/asf/thrift.git thrift
cd thrift/
git checkout 0.9.1 -b build
./bootstrap.sh
./configure
make -j
sudo make install
```

## Archlinux
```bash
wget https://aur.archlinux.org/packages/th/thrift/thrift.tar.gz
tar -xvzf thrift.tar.gz
cd thrift
makepkg -s
sudo pacman -U thrift*.tar.xz
```

# Installation and running
```bash
git clone git@github.com:Completionary/completionProxy.git
cd completionProxy/thrift
make all lang=java
cd ..
mvn exec:java -Dexec.mainClass="de.completionary.proxy.CompletionProxy"
```

## Thrift API
If you want to communicate with our thrift API, you can execute `make all lang=$language` to generate all files you need for your prefered programming language $language (e.g. js:node, erlang, php, py...). This will generate a directory called `gen-$language` with all the API files in your language. Please see the following link for mor information on thrift: https://thrift.apache.org/tutorial/

### Java
Here you can find a simple example of how to connect to the admin service:

```java
TTransport transport =
    new TFramedTransport(new TSocket("localhost", ProxyOptions.ADMIN_SERVER_PORT));
TProtocol protocol = new TCompactProtocol(transport);

client = new AdminService.Client(protocol);
transport.open();

client.addSingleTerm(index, ID, inputString, outputString, payload, wight);

```
