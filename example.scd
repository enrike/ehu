/*
connect MIDI keyboard to \base
add buttons to launch utils from main interface

all windows to try to open a preset file by default (default_auto.preset for instance)
*/

(
f = Feedback1.new;
f.chord([0, 6.1, 10, 15.2, 22, 24 ]+40);
//[40, 45, 50, 55, 59, 64]
f.chord( ([0, 6.1, 10, 15.2, 22, 24 ]+40).scramble );
f.chord([0,7,12,15,19,24]+40);

//f.midi; // midi setup
//f.nanok; // connect to nanokontrol2

// other utils I use
s.waitForBoot({
	//f.on;
	f.eq;
	//f.gneck;
	//f.auto;
	//f.chords;
})
)

f.auto


// other controls
f.on; // f.off
f.chord( ([0, 6.1, 10, 15.2, 22, 24 ]+40).scramble );
f.deltime(61)
f.amp(0.3)
f.damp(1300)
f.mod(1)
f.vol(0.7)
f.tremolo(0.2)
f.drywet(0.4)

/* same goes for...
    in
	gainin
	out
	loop
	feedback

	thresh
	slopeBelow
	slopeAbove
	clampTime
	relaxTime
f.auto
f.gneck
f.chords
f.eq
*/




(
t.stop;
t = Task({
	inf.do({|i|
		f.chord( GuitarChords.chords.choose );
		[0.1, 2.7, 1.2].wrapAt(i).wait;
	});
});

t.start;
)





// sequence 2 //
(
//~chord = [0,7,12,15,19,24]; // mi menor si 64
~chord = [0, 6.1, 10, 15.2, 22, 24 ]+40; // chord
~chord = [0, 0.1, 0.15, 0.25, 0.5 ]+40; // chord
~mel = [45,48, 46];//
~times = [1, 7.1, 3.5];

t = Task({
	var notes=Array.new(6), base=64, time=0;

	inf.do({ arg i;
		// set the state
		f.chord(base+notes);
		// choose the next one, tell me and wait
		notes = ~chord.scramble;
		base = ~mel.choose;
		time = ~times.choose;
		[time, base.midiname, base, (notes+base).midiname].postln;
		time.wait;
	});
});
t.start
)
t.stop





(
c = ([0, 6.1, 10, 15.2, 22, 24]+40).scramble;
c.postln;
f.chord(c);
)
~chords = []

(
t = Task({
	inf.do({|i|
		f.chord(~chords[i].wrap)
		[4,5.5][i].wrap.wait
	})
})
)



(
c = ([0, 6.1, 10, 15.2, 22, 24]+40).scramble;
c.postln;
f.chord(c);
)
~chords = []

(

t = Task({
	inf.do({|i|
		f.chord(Array.fill(6, {[0,3,6].choose+(0.1.rand)}));
		[4,3, 0.3].choose.wait
	})
})
)
f.base(48)
t.start
t.stop

