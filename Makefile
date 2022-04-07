VERSION = 1.0.2
DATE = $(shell date -R)

SYS_BINDIR = /usr/bin
SYS_MANDIR = /usr/share/man
SYS_DOCDIR = /usr/share/doc/geth
ICONDIR = /usr/share/icons/hicolor
SYS_GETHDIR = /usr/share/geth

SED_GETH = $(shell echo $(SYS_BINDIR)/geth | sed s/\\//\\\\\\\\\\//g)
SED_GETHDIR = $(shell echo $(SYS_GETHDIR) | sed  s/\\//\\\\\\\\\\//g)
SED_ICONDIR =  $(shell echo $(SYS_ICONDIR) | sed s/\\//\\\\\\\\\\//g)

APPS_DIR = apps
SYS_APPDIR = /usr/share/applications
SYS_ICON_DIR = /usr/share/icons/hicolor
SYS_APP_ICON_DIR = $(SYS_ICON_DIR)/scalable/$(APPS_DIR)

BINDIR=$(DESTDIR)$(SYS_BINDIR)
MANDIR = $(DESTDIR)$(SYS_MANDIR)
DOCDIR = $(DESTDIR)$(SYS_DOCDIR)
ICONDIR = $(DESTDIR)$(SYS_ICONDIR)
GETHDIR = $(DESTDIR)$(SYS_GETHDIR)
APPDIR = $(DESTDIR)$(SYS_APPDIR)
ICON_DIR = $(DESTDIR)$(SYS_ICON_DIR)
APP_ICON_DIR = $(DESTDIR)$(SYS_APP_ICON_DIR)

SOURCEICON = icons/geth.svg
TARGETICON = geth.svg
TARGETICON_PNG = geth.png

ICON_WIDTHS = 16 20 22 24 32 36 48 64 72 96 128 192 256 512

all: deb docs/manual/manual.html docs/manual/manual.xml

classes:
	mkdir -p classes

JFILES = $(wildcard src/*.java)

PROPERTY_DIRS = lpack

PROPERTIES = $(wildcard src/lpack/*.properties)


MANUAL = src/manual.xml src/manual.html src/manual.css

FLAGS = -Xlint:deprecation -Xlint:unchecked

geth.jar: $(JFILES) classes $(PROPERTIES) $(SOURCEICON) $(MANUAL)
	javac $(FLAGS) -d classes -classpath /usr/share/java/libbzdev.jar \
		$(JFILES)
	for i in $(MANUAL) ; do cp $$i classes; done
	for i in $(PROPERTY_DIRS) ; do \
		mkdir -p classes/$$i ; \
		cp src/$$i/*.properties classes/$$i; \
	done
	for i in  $(ICON_WIDTHS) ; do \
		inkscape -w $$i \
			--export-filename=classes/geth$${i}.png \
			$(SOURCEICON) ; \
	done
	jar cfe geth.jar HttpHeaders -C classes .

version:
	@echo $(VERSION)

install: geth.jar
	install -d $(BINDIR)
	install -d $(MANDIR)/man1
	install -d $(MANDIR)/man5
	install -d $(DOCDIR)
	install -d $(GETHDIR)
	install -d $(APP_ICON_DIR)
	install -d $(APPDIR)
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
	install -m 0644 -T $(SOURCEICON) $(APP_ICON_DIR)/$(TARGETICON)
	for i in $(ICON_WIDTHS) ; do \
		install -d $(ICON_DIR)/$${i}x$${i}/$(APPS_DIR) ; \
		inkscape -w $$i --export-filename=tmp.png $(SOURCEICON) ; \
		install -m 0644 -T tmp.png \
			$(ICON_DIR)/$${i}x$${i}/$(APPS_DIR)/$(TARGETICON_PNG); \
		rm tmp.png ; \
	done
	install -m 0644 geth.desktop $(APPDIR)
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
                deb/postinst deb/postrm \
		geth.jar geth.sh geth.1  Makefile
	mkdir -p BUILD
	(cd BUILD ; rm -rf usr DEBIAN)
	mkdir -p BUILD/DEBIAN
	cp deb/postinst BUILD/DEBIAN/postinst
	chmod a+x BUILD/DEBIAN/postinst
	cp deb/postrm BUILD/DEBIAN/postrm
	chmod a+x BUILD/DEBIAN/postrm
	$(MAKE) install DESTDIR=BUILD debLog
	sed -e s/VERSION/$(VERSION)/ deb/control > BUILD/DEBIAN/control
	fakeroot dpkg-deb --build BUILD
	mv BUILD.deb $(DEB)
	(cd inst; make)
	cp inst/geth-install.jar geth-install-$(VERSION).jar

icons/geth.svg: icons/geth.eptt icons/geth.epts
	(cd icons; epts -o geth.svg geth.eptt)

L1 = 's/<!-- link1 -->/<LINK HREF="print.css" REL="stylesheet" type="text\/css" MEDIA="print">/'

L2 = 's/<!-- link2 -->/<LINK HREF="manual.css" REL="stylesheet" type="text\/css" MEDIA="screen">/'

docs/manual/manual.html: src/manual.html
	cat src/manual.html \
	| sed -e 's/<!-- link1 -->/<LINK HREF="print.css" REL="stylesheet" type="text\/css" MEDIA="print">/' \
	| sed -e 's/<!-- link2 -->/<LINK HREF="manual.css" REL="stylesheet" type="text\/css" MEDIA="screen">/' > docs/manual/manual.html

docs/manual/manual.xml: src/manual.xml
	cat src/manual.xml \
	| sed -e 's/Help/GETH Manual /' > docs/manual/manual.xml

clean:
	rm -fr BUILD classes
	rm geth.jar
