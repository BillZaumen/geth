all: geth.jar
	rm -rf TBUILD
	mkdir TBUILD
	javac -d TBUILD Installer.java
	(cd .. ; git log -64 ) > TBUILD/changelog
	echo >> TBUILD/changelog
	echo --- Earlier changes not shown --- >> TBUILD/changelog
	cp geth.jar TBUILD/geth.jar
	cp /usr/share/java/libbzdev-base.jar TBUILD/libbzdev-base.jar
	cp /usr/share/java/libbzdev-desktop.jar TBUILD/libbzdev-desktop.jar
	cp ../gethdrs.1 TBUILD/gethdrs.1
	cp ../gethdrs.5 TBUILD/gethdrs.5
	cp ../copyright TBUILD/copyright
	cp ../gethdrs.desktop TBUILD/gethdrs.desktop
	gzip -9 < TBUILD/gethdrs.1 > TBUILD/gethdrs.1.gz
	groff -T html ../gethdrs.1 > TBUILD/gethdrs.html
	gzip -9 < TBUILD/gethdrs.5 > TBUILD/gethdrs.5.gz
	groff -T html ../gethdrs.5 > TBUILD/gethdrs-files.html
	mkdir -p icons/hicolor/scalable/apps
	cp ../icons/geth.svg icons/hicolor/scalable/apps/gethdrs.svg
	mkdir -p icons/hicolor
	for i in 512 256 128 96 72 64 48 32 24 16 8 ; do \
	mkdir -p icons/hicolor/$${i}x$${i}/apps ; \
	inkscape -w $$i \
	  --export-filename=icons/hicolor/$${i}x$${i}/apps/gethdrs.png \
	  ../icons/geth.svg ; done
	(zfile=`pwd`/TBUILD/share.zip ; zip -r -q $$zfile icons)
	rm -r icons
	cp finish.txt TBUILD/finish.txt
	echo '#!/bin/sh' > TBUILD/gethdrs.sh
	echo 'JAVA -jar GETHDIR/geth.jar "$$@"' >> TBUILD/gethdrs.sh
	echo '@JAVA -jar GETHDIR/geth.jar %*' > TBUILD/gethdrs.cmd
	echo '@JAVAW -jar GETHDIR/geth.jar %*'> TBUILD/gethdrsw.cmd
	jar cfm gethdrs-install.jar install.mf -C TBUILD .

geth.jar: ../classes/HttpHeaders.class manifest.mf
	jar cfm geth.jar manifest.mf  -C ../classes .

../classes/HttpHeaders.class: ../src//HttpHeaders.java ../icons/geth.svg
	(cd .. ; make geth.jar)

clean:
	rm -rf TBUILD geth.jar

superclean: clean
	rm -f geth-install.jar
