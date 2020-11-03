/* feedback
*/

( // run here the basic setup
f = Feedback1.new;
f.chord.postln;

f.midi; // connect to nanokontrol2

f.gneck; // gui to change the chord
f.ch; // another gui to change the chord
f.ch(\default); //loads "chords_default.preset" config file

// effects I use in combination with the feedback unit
//ChannelEQ.new; // download and install from https://github.com/enrike/supercollider-channeleq requieres wslib and tabbed views
//ChannelEQ.new(10); // you can use the loop bus value to control the internal feedback EQ
//AutoNotchGUI.new;
//TremoloGUI.new;
//CompanderGUI.new;
//BufferPlayerGUI.new;
//LimiterGUI.new
//ChaosPitchShiftGUI.new
//PitchShiftGUI.new;
//GainLimiterGUI.new;
Launcher.new; // simple GUI to launch all utils



// control manually
f.on;
f.off;
f.chord( ([0, 6.1, 10, 15.2, 22, 24 ]+40) );
f.feedback(0.2);
f.deltime(61);
f.amp(0.3);
f.damp(1300);
f.mod(1);
f.vol(0.7);

f.notch; // activate autonotch eq
f.notch(0);

f.in(10); // in channel
f.gainin(0.5); // how much signal from the line in
f.out(8); // out channel
f.loop(8); // channel used to do the feedback loop

f.scramble; // chord
f.rcontrol(\feedback); // random value for \feedback control. other controls can be used too



// some examples playing with Feedback1 chord. It assumes f = Feedback1.new;

( // chooses chords from GuitarChords class
t.stop;
t = Task({
	inf.do({|i|
		f.chord( GuitarChords.chords.choose+40 );
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


( // scramble once in a while
t = Task({
	inf.do({|i|
		f.scramble;
		[4, 10, 8.3].choose.wait
	})
});
t.start
)
t.stop



( // scramble once in a while
var ch = [ 0, 7, 12, 15, 19, 24 ];
var mel = [40, 45, 43, 43.5];
var times = [3, 6, 8.3];
t = Task({
	inf.do({|i|
		f.chord( ch + mel.wrapAt(i) );
		times.wrapAt(i).wait
	})
});
t.start
)
t.stop

f.scramble


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
~mel = [45,48, 46]/2;//
~times = [1, 7.1, 3.5];
)

( // seq 1 //
//~chord = [0,7,12,15,19,24]; // mi menor si 64
~chord = [0, 6.1, 10, 15.2, 22, 24 ]; // chord
//~mel = [59, 64, 67, 69]/2;//
~mel = [64, 66, 70, 71]/2;
//~mel = [62, 64, 66, 76];
//~mel = [64,66,67];
~times = [10, 8.5, 7.7, 6, 15.1, 12.1];
//~times = [10, 8.5, 3.7, 3, 3.1, 1.1];
//~times = [1.7, 3, 2.1, 1.1];
//~times = [8, 8.1,8.2, 8.3, 8.4, 8.5];
)


( // now launch task
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



(
~mel = [40, 45, 47];
~times = [2, 3.5, 2.7, 1, 5.1, 2.1];
t = Task({
	var notes=Array.new(6), base=64, time=0;
	inf.do({ arg i;
		f.chord(base+[0,7,12,15,19,24]);
		base = ~mel.choose;
		time = ~times.choose;
		time.wait;
	});
});
t.start
)



(
~mel =  [41, 39, 42.5];
~times = [13, 2, 19];
t = Task({
	var notes=Array.new(6), base=64, time=0;
	inf.do({ arg i;
		f.chord(base+[ 0, 6, 10, 15, 22, 24 ]);
		base = ~mel.choose;
		time = ~times.choose;
		time.wait;
	});
});
t.start
)



(
//~mel = GuitarChords.chords[\Am7]+20;
~times = [37, 57, 44];
~chords = [ [ 0, 6, 10, 15, 22, 24 ], [ 0, 6.5, 10, 15, 20, 24 ] ];
t = Task({
	var base=30;
	inf.do({ arg i;
		f.chord(base+~chords.wchoose([0.85, 0.15]));
		~times.choose.wait;
	});
});
t.start
)
t.stop

