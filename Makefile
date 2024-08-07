jedis:
	git submodule update --init --recursive --remote
	cd vendor/jedis && make mvn-package-no-tests

all: jedis
