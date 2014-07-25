outDir=../src/main/java/ 

admin:
	thrift --gen java --out $(outDir) $@.thrift
analytics:
	thrift --gen java --out $(outDir) $@.thrift
streaming:
	thrift --gen java --out $(outDir) $@.thrift
suggestion:
	thrift --gen java --out $(outDir) $@.thrift

all: admin analytics streaming suggestion