completionProxy
===============

Proxy implementing an interface to the suggestion index (elastic search) and the analytics backend.

# Dependencies
To make this code compilable you have to generate the thrift files. To install the thrift compiler please see the following links:

## ebian Wheezy/Ubuntu
```
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
https://aur.archlinux.org/packages/thrift/

