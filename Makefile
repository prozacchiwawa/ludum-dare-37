all: copy

copy: out/production/oneroom/index.html \
	out/production/oneroom/three.min.js \
	out/production/oneroom/WalterSheet.png \
	out/production/oneroom/CopSheet01.png \
	out/production/oneroom/KarlSheet.png \
	out/production/oneroom/ItemSprites01.png \
	out/production/oneroom/typewriter.png \
	out/production/oneroom/filecabinet.png \
	out/production/oneroom/keycard.png \
	out/production/oneroom/CopCar0001.png

out/production/oneroom/index.html: index.html
	cp index.html out/production/oneroom

out/production/oneroom/WalterSheet.png: assets/WalterSheet.png
	cp $< $@

out/production/oneroom/CopSheet01.png: assets/CopSheet01.png
	cp $< $@

out/production/oneroom/ItemSprites01.png: assets/ItemSprites01.png
	cp $< $@

out/production/oneroom/typewriter.png: assets/typewriter.png
	cp $< $@

out/production/oneroom/filecabinet.png: assets/filecabinet.png
	cp $< $@

out/production/oneroom/keycard.png: assets/keycard.png
	cp $< $@

out/production/oneroom/CopCar0001.png: assets/CopCar0001.png
	cp $< $@

out/production/oneroom/three.min.js: three.js-master/build/three.min.js
	cp three.js-master/build/three.min.js out/production/oneroom
