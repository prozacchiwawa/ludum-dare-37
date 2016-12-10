all: copy

copy: out/production/oneroom/index.html \
	out/production/oneroom/three.min.js \
	out/production/oneroom/SkinnerRig1x1.json \
	out/production/oneroom/SkinnerWalk1x1.json \
	out/production/oneroom/SkinnerWalk2x1.json \
	out/production/oneroom/KolchakRig.json

out/production/oneroom/index.html: index.html
	cp index.html out/production/oneroom

out/production/oneroom/KolchakRig.json: assets/KolchakRig.json
	cp assets/KolchakRig.json out/production/oneroom

out/production/oneroom/SkinnerRig1x1.json: assets/SkinnerRig1x1.json
	cp assets/SkinnerRig1x1.json out/production/oneroom

out/production/oneroom/SkinnerWalk1x1.json: assets/SkinnerWalk1x1.json
	cp assets/SkinnerWalk1x1.json out/production/oneroom

out/production/oneroom/SkinnerWalk2x1.json: assets/SkinnerWalk2x1.json
	cp assets/SkinnerWalk2x1.json out/production/oneroom

out/production/oneroom/three.min.js: three.js-master/build/three.min.js
	cp three.js-master/build/three.min.js out/production/oneroom
