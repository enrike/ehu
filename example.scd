/*
connect MIDI keyboard to \base
*/

(
f = Feedback1.new;
f.base(64); //fundamental freq
f.chord([0, 6.1, 10, 15.2, 22, 24 ]);
f.audio;
f.gui;
//f.midi; // midi setup
//f.nanok; // connect to nanokontrol2

// other utils I use
s.waitForBoot({
	ChannelEQ.new;
	NeckGUI.new(f);
	//s.meter(2,2);

	//AutoGUI.new(f);
	//ChordGUI.new(f);
})
)


// other controls
f.chord([0, 6.1, 10, 15.2, 22, 24 ].scramble);
f.deltime(61)
f.amp(0.3)
f.damp(1300)
f.mod(1)
f.base([64, 66, 68].choose)
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
*/





// sequence 2 //
(
//~chord = [0,7,12,15,19,24]; // mi menor si 64
~chord = [0, 6.1, 10, 15.2, 22, 24 ]; // chord
~chord = [0, 0.1, 0.15, 0.25, 0.5 ]; // chord
~mel = [45,48, 46];//
~times = [1, 7.1, 3.5];

t = Task({
	var notes=Array.new(6), base=64, time=0;

	inf.do({ arg i;
		// set the state
		f.chord(notes);
		f.base(base);
		// choose the next one, tell me and wait
		notes = ~chord.scramble;
		base = ~mel.choose;
		time = ~times.choose;
		[time, base.midiname, base, (notes+64).midiname].postln;
		time.wait;
	});
});
t.start
)
t.stop





(
c = [0, 6.1, 10, 15.2, 22, 24].scramble;
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
