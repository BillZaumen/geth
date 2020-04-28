VERSION = 0.3

DATE = $(shell date -R)

SYS_BINDIR = /usr/bin
SYS_MANDIR = /usr/share/man
SYS_DOCDIR = /usr/share/doc/geth
# ICONDIR = /usr/share/icons/hicolor
SYS_GETHDIR = /usr/share/geth

SED_GETH = $(shell echo $(SYS_BINDIR)/geth | sed  s/\\//\\\\\\\\\\//g)
SED_GETHDIR = $(shell echo $(SYS_GETHDIR) | sed  s/\\//\\\\\\\\\\//g)
# SED_ICONDIR =  $(shell echo $(SYS_ICONDIR) | sed  s/\\//\\\\\\\\\\//g)

# APPS_DIR = apps
# SYS_APPDIR = /usr/share/applications
# SYS_ICON_DIR = /usr/share/icons/hicolor
# SYS_APP_ICON_DIR = $(SYS_ICON_DIR)/scalable/$(APPS_DIR)

BINDIR=$(DESTDIR)$(SYS_BINDIR)
MANDIR = $(DESTDIR)$(SYS_MANDIR)
DOCDIR = $(DESTDIR)$(SYS_DOCDIR)
# ICONDIR = $(DESTDIR)$(SYS_ICONDIR)
GETHDIR = $(DESTDIR)$(SYS_GETHDIR)
# APPDIR = $(DESTDIR)$(SYS_APPDIR)
# ICON_DIR = $(DESTDIR)$(SYS_ICON_DIR)
# APP_ICON_DIR = $(DESTDIR)$(SYS_APP_ICON_DIR)

# SOURCEICON = geth.svg
# TARGETICON = geth.svg
# TARGETICON_PNG = geth.png

# ICON_WIDTHS = 16 20 22 24 32 36 48 64 72 96 128 192 256

all: deb

classes:
	mkdir -p classes

JFILES = $(wildcard src/*.java) $(wildcard src/org/bzdev/swing/*.java) \
	$(wildcard src/org/bzdev/protocols/*java) \
	$(wildcard src/org/bzdev/protocols/resource/*java) \
	$(wildcard src/org/bzdev/protocols/sresource/*java) \
	$(wildcard src/org/bzdev/util/*.java)

PROPERTY_DIRS = lpack \
		org/bzdev/swing/lpack \
		org/bzdev/swing/io/lpack \
		org/bzdev/protocols/resource/lpack \
		org/bzdev/protocols/sresource/lpack \
		org/bzdev/util/lpack

PROPERTIES = $(wildcard src/lpack/*.properties) \
	     $(wildcard src/org/bzdev/swing/lpack/*.properties) \
	     $(wildcard src/org/bzdev/swing/io/lpack/*.properties) \
	     $(wildcard src/org/bzdev/protocols/resource/lpack/*.properties) \
	     $(wildcard src/org/bzdev/protocols/sresource/lpack/*.properties) \
	     $(wildcard src/org/bzdev/util/lpack/*.properties)


ICONS = $(wildcard src/org/bzdev/swing/icons/*.gif)
MANUAL = src/manual.xml src/manual.html src/manual.css

FLAGS = -Xlint:deprecation -Xlint:unchecked

geth.jar: $(JFILES) classes $(PROPERTIES) $(ICONS) $(MANUAL)
	javac $(FLAGS) -d classes -sourcepath src $(JFILES)
	for i in $(MANUAL) ; do cp $$i classes; done
	mkdir -p classes/org/bzdev/swing/icons
	for i in $(ICONS) ; do cp $$i classes/org/bzdev/swing/icons ; done
	for i in $(PROPERTY_DIRS) ; do \
		mkdir -p classes/$$i ; \
		cp src/$$i/*.properties classes/$$i; \
	done
	jar cfe geth.jar HttpHeaders -C classes .

install: geth.jar
	install -d $(BINDIR)
	install -d $(MANDIR)/man1
	install -d $(MANDIR)/man5
	install -d $(DOCDIR)
	install -d $(GETHDIR)
	install -m 0644 geth.jar $(GETHDIR)
	sed -e s/GETHDIR/$(SED_GETHDIR)/ < geth.sh > geth.tmp
	install -m 0755 -T geth.tmp $(BINDIR)/geth
	rm geth.tmp
	sed -e s/VERSION/$(VERSION)/ geth.1 | gzip -n -9 > geth.1.gz
	sed -e s/VERSION/$(VERSION)/ geth.5 | \
		gzip -n -9 > geth.5.gz
	install -m 0644 geth.1.gz $(MANDIR)/man1
	install -m 0644 geth.5.gz $(MANDIR)/man5
	rm geth.1.gz
	rm geth.5.gz
	gzip -n -9 < changelog > changelog.gz
	install -m 0644 changelog.gz $(DOCDIR)
	rm changelog.gz
	install -m 0644 copyright $(DOCDIR)

DEB = geth_$(VERSION)_all.deb

deb: $(DEB)

debLog:
	sed -e s/VERSION/$(VERSION)/ deb/changelog.Debian \
		| sed -e "s/DATE/$(DATE)/" \
		| gzip -n -9 > changelog.Debian.gz
	install -m 0644 changelog.Debian.gz $(DOCDIR)
	rm changelog.Debian.gz

$(DEB): deb/control copyright changelog deb/changelog.Debian \
		geth.jar geth.sh geth.1  Makefile
	mkdir -p BUILD
	(cd BUILD ; rm -rf usr DEBIAN)
	mkdir -p BUILD/DEBIAN
	$(MAKE) install DESTDIR=BUILD debLog
	sed -e s/VERSION/$(VERSION)/ deb/control > BUILD/DEBIAN/control
	fakeroot dpkg-deb --build BUILD
	mv BUILD.deb $(DEB)

clean:
	rm -fr BUILD classes
	rm geth.jar
