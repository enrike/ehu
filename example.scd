/* feedback
*/

( // run here the basic setup
f = Feedback1.new;
f.chord.postln;
f.nanok; // connect to nanokontrol2

// other utils I use
{// wait until everything is ready to go
	f.eq; // download and install from https://github.com/enrike/supercollider-channeleq requieres wslib and tabbed views
	//f.eq(10); // you can use the loop bus value to control the internal feedback EQ
	//f.gneck;
	//f.ch; // it takes the chord from main
	//f.ch(\default); //loads "chords_default.preset" config file
	//f.auto;
	//f.anotch;
	//f.trem;
	//f.comp;
}.defer(4) // for instance
)

Launcher.new; // experimental


(// in an instalation
f = Feedback1.new; // this opens feedback_unit_default.preset
//f = Feedback1.new(preset: \noisy); // this opens feedback_unit_noisy.preset

{// wait until everything is ready to go
	f.eq; // download and install from https://github.com/enrike/supercollider-channeleq requieres wslib and tabbed views
	//f.eq(10); // you can use the loop bus value to control the internal feedback EQ
	//f.gneck;
	//f.ch; // it takes the chord from main
	//f.ch(config: \default); //loads "chords_default.preset" config file
	//f.auto;
	//f.anotch;
	//f.trem;
	f.comp(\installation1);
}.defer(4) // for instance
)





// other controls
f.scramble; // chord

f.on;
f.off;
f.chord( ([0, 6.1, 10, 15.2, 22, 24 ]+40).scramble );
f.feedback(0.2);
f.deltime(61);
f.amp(0.3);
f.damp(1300);
f.mod(1);
f.vol(0.7);

f.in(10); // in channel
f.out(8); // out channel
f.gainin(0.5); // how much signal from the line in
f.loop(8); // channel used to do the feedback loop

// compressor expander
f.thresh(1);
f.slopeBelow(1);
f.slopeAbove(1);
f.clampTime(1);
f.relaxTime(1);

//tremolo
f.tremolo(0.2);
f.drywet(0.4);

// normalizer
f.norm(0.4);
f.normlvl(0.4);



// some examples playing with chord. Must run first f = Feedback1.new;

( // chooses chords from GuitarChords class
t.stop;
t = Task({
	inf.do({|i|
		f.chord( GuitarChords.chords.choose );
		[0.1, 2.7, 1.2].wrapAt(i).wait;
	});
});

t.start;
)



( // just make up clusters of notes
t = Task({
	inf.do({|i|
		f.chord(Array.fill(6, { 40+([0,3,6].choose+(0.1.rand)) }));
		[4, 3, 0.3].choose.wait
	})
});
t.start
)
t.stop




// different situations to be used with the task below
( // seq2 //
~times = [16, 7.1, 19.5];
~mel = [45.2,48, 46.5]/4;// [ 11.3, 12.0, 11.625 ]
~chord = [0, 6.1, 10, 15.2, 22, 24 ]; // chord
)

( // sequence 3 //
//~chord = [0,7,12,15,19,24]; // mi menor si 64
~chord = [0, 6.1, 10, 15.2, 22, 24 ]; // chord
//~chord = [0, 0.1, 0.15, 0.25, 0.5 ]; // chord
~mel = [45,48, 46]/4;//
~times = [1, 7.1, 3.5];
)

( // seq 1 //
//~chord = [0,7,12,15,19,24]; // mi menor si 64
~chord = [0, 6.1, 10, 15.2, 22, 24 ]; // chord
//~mel = [59, 64, 67, 69]/2;//
~mel = [64, 66, 70, 71]/4;
//~mel = [62, 64, 66, 76];
//~mel = [64,66,67];
~times = [10, 8.5, 7.7, 6, 5.1, 2.1];
//~times = [10, 8.5, 3.7, 3, 3.1, 1.1];
//~times = [1.7, 3, 2.1, 1.1];
//~times = [8, 8.1,8.2, 8.3, 8.4, 8.5];
)


( // now launch task
f.normlvl(-1); //off
f.feedback(0.155);
f.amp(0.2);

t = Task({
	var notes=Array.new(6), base=64, time=0;
	inf.do({ arg i;
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
t = nil







