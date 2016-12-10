all: copy

copy: out/production/ld37/index.html \
	out/production/ld37/three.min.js

out/production/ld37/index.html: index.html
	cp index.html out/production/ld37

out/production/ld37/three.min.js: three.js-master/build/three.min.js
	cp three.js-master/build/three.min.js out/production/ld37
