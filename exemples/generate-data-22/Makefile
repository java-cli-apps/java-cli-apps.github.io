APP_NAME=GenerateData
APP_DIR=generate-data

BUILD=build
BUILD_APP=$(BUILD)/$(APP_DIR)

test:
	APP_DIR=. ./$(APP_NAME).java \
		--column id::random \
		--column numero_client::file::clients.txt \
		--column montant::random \
		--count 10 \
		create_table.sql

package: build
	cd $(BUILD) && zip --recurse-paths $(APP_NAME).zip $(APP_DIR)/

install: check-install-dir
	unzip -d $(PREFIX) $(BUILD)/$(APP_NAME).zip

test-install: check-install-dir
	PATH=$(PREFIX)/$(APP_DIR)/bin:$(PATH) $(APP_NAME).sh --help

check-install-dir:
	@test -n '$(PREFIX)' || { echo "You need to specify an installation directory, for example 'PREFIX=~ make install'"; exit 1; }

build: prepare
	cp *.java $(BUILD_APP)/src
	cp $(APP_NAME).sh $(BUILD_APP)/bin
	cp --recursive lib/ $(BUILD_APP)

prepare: clean
	mkdir --parents $(BUILD_APP) $(BUILD_APP)/src $(BUILD_APP)/bin $(BUILD_APP)/lib

clean:
	rm --force --recursive $(BUILD)
