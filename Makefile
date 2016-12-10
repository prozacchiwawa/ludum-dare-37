all: copy

copy: out/production/oneroom/index.html \
	out/production/oneroom/three.min.js

out/production/oneroom/index.html: index.html
	cp index.html out/production/oneroom

out/production/oneroom/three.min.js: three.js-master/build/three.min.js
	cp three.js-master/build/three.min.js out/production/oneroom
