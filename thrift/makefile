ifndef lang
$(error lang is not set (try e.g. make all lang=java))
endif

ifndef outDir
ifeq ($(lang),java)
outDir="../src/main/java/"
else
outDir="gen-$(lang)"
endif
endif

pre-build:
	echo "The main out-directory is $(outDir)"
	mkdir -p $(outDir)

admin: pre-build
	thrift --gen $(lang) --out $(outDir) $@.thrift
analytics: pre-build
	thrift --gen $(lang) --out $(outDir) $@.thrift
streaming: pre-build
	thrift --gen $(lang) --out $(outDir) $@.thrift
suggestion: pre-build
	thrift --gen $(lang) --out $(outDir) $@.thrift

all: admin analytics streaming suggestion