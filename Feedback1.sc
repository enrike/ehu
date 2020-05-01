// synthdef based on https://sccode.org/1-U by Nathaniel Virgo

// to do:
// # EZSlider 3 decimals,
// normalize intialise to -1
// ChordGUI connect properly to Feedback
// ON/OFF button on init
// # XFade is this the best way to fade between two signals?
// # close utils windows on close
// autogui does not init poroperly after adding Normlvl widget


Feedback1 : EffectGUI {
	var auto, chord, <synth, path, utils;

	*new {
		^super.new.initFeedback1();
	}

	initFeedback1  {
		chord = [0,7,12,15,19,24]; //[0, 6.1, 10, 15.2, 22, 24 ];
		utils = List.new;//refs to GUI windows
		Server.default.waitForBoot{
			this.audio;
			this.gui;
			//{this.gui}.defer(0.5)
		}
	}

	audio {

		path = thisProcess.nowExecutingPath.dirname;

		// BASED ON https://sccode.org/1-U by Nathaniel Virgo
		SynthDef(\feed, {|in=2, out=0, loop=10, gainin=0, feedback=0.02, deltime=75, freqdiv=1,
			revtimes=5, amp=0.6, damping=1360, mod=1, vol=0.9, chord=#[ 40, 47, 52, 55, 59, 64 ],
			thresh=0.5, slopeBelow=1, slopeAbove=0.5, clampTime=0.01, relaxTime=0.01, limit=0.5,
			norm=0, normlvl=(1.neg), freq=0, drywet=(1.neg), on=0|

			var del, minfreqs, freqs, dry, nsig; //VARS
			var sig = ((InFeedback.ar(loop, 2) + WhiteNoise.ar(0.001!2)) * feedback) + (In.ar(in, 2) * gainin);

			// delay due to distance from amp - I chose 0.05s, or 20Hz
			sig = DelayN.ar(sig, 1/10-ControlDur.ir, 1/deltime-ControlDur.ir);

			// guitar string frequencies - for some reason I had to pitch them down
			freqs = chord.midicps/freqdiv;

			// whammy bar modulates freqs:
			minfreqs = freqs * 0.5;
			freqs = freqs * mod;

			// 6 comb filters emulate the strings' resonances
			// maxdelaytime, delaytime, decaytime
			sig = CombN.ar(sig!6, 1/minfreqs, 1/freqs, 8).mean;

			// a little filtering
			sig = LPF.ar(sig, 8000);
			sig = HPF.ar(sig * amp, 80);

			// and some not too harsh distortion
			sig = RLPFD.ar(sig, damping * [1, 1.1], 0.1, 0.5);
			sig = sig + sig.mean;

			// and finally a spot of reverb
			revtimes.do { // loop rev times
				del = 0.2.rand; // delayt and decayt
				sig = AllpassN.ar(sig, del, del, 5);
			};

			sig = Compander.ar(sig, sig, thresh, slopeBelow, slopeAbove, clampTime, relaxTime);

			Out.ar(loop, sig); // feedback loop before the main output

			dry = sig;
			sig = sig * SinOsc.ar(freq);
			sig = XFade2.ar(dry, sig, drywet);

			nsig = Normalizer.ar(sig, norm);
			sig = XFade2.ar(sig, nsig, normlvl);

			sig = Limiter.ar(sig * vol, 1);

			Out.ar(out, sig * on)
		}).send;

		//synth = Synth(\feed, [\chord, chord])
		{ synth = Synth(\feed, [\chord, chord]) }.defer(0.2); // should wait until load oe send is done
	}


	gui {
		// GUI ////////////////////////
		super.gui("Feedback unit", 430@465); // init super gui buttons
		w.onClose = {
			"closing down Feedback unit and utils".postln;
			synth.free;
			utils.collect(_.close)
		};

		StaticText(w, 12@18).align_(\right).string_("In").resize_(7);
		controls[\in] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\in, m.value);
		}.value = 2; // default to sound in

		StaticText(w, 30@18).align_(\right).string_("Loop").resize_(7);
		controls[\loop] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\loop, m.value);
		}.valueAction = 10;

		StaticText(w, 23@18).align_(\right).string_("Out").resize_(7);
		controls[\out] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\out, m.value);
		}.valueAction = 0; //

		controls[\on] = Button(w, 22@18)
		.states_([
			["on", Color.white, Color.black],
			["off", Color.black, Color.red]
		])
		.action_({ arg butt;
			synth.set(\on, butt.value)
		});

		w.view.decorator.nextLine;

		ActionButton(w,"auto",{
			utils.add( AutoGUI.new(this, path) )
		});

		ActionButton(w,"gneck",{
			utils.add( GNeckGUI.new(this, path) );
		});

		ActionButton(w,"chords",{
			utils.add( ChordGUI.new(this, path, chord) );
		});

		ActionButton(w,"EQ",{
			try { utils.add( ChannelEQ.new) }
			{"cannot find ChannelEQ class. try installing it from http://github.com/enrike/supercollider-channeleq".postln}
		});

		w.view.decorator.nextLine;

		// SLIDERS //
		order.add(\gainin);
		controls[\gainin] = EZSlider( w,         // parent
			420@20,    // bounds
			"gain in",  // label
			ControlSpec(0, 2, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\gainin, ez.value) } // action
		);
		controls[\gainin].numberView.maxDecimals = 3 ;

		StaticText(w, Rect(0,0, 80, 15)).string="Feedback";

		order.add(\feedback);
		controls[\feedback] = EZSlider( w,         // parent
			420@20,    // bounds
			"feedback",  // label
			ControlSpec(0, 1, \lin, 0.001, 0.02),     // controlSpec
			{ |ez| synth.set(\feedback, ez.value) } // action
		);
		controls[\feedback].numberView.maxDecimals = 3 ;

		order.add(\deltime);
		controls[\deltime] = EZSlider( w,         // parent
			420@20,    // bounds
			"deltime",  // label
			ControlSpec(0, 500, \lin, 0.001, 75),     // controlSpec
			{ |ez| synth.set(\deltime, ez.value) } // action
		);
		controls[\deltime].numberView.maxDecimals = 3 ;

		order.add(\amp);
		controls[\amp] = EZSlider( w,         // parent
			420@20,    // bounds
			"amp",  // label
			ControlSpec(0, 3, \lin, 0.001, 0.6),     // controlSpec
			{ |ez| synth.set(\amp, ez.value) } // action
		);
		controls[\amp].numberView.maxDecimals = 3 ;

		order.add(\damp);
		controls[\damp] = EZSlider( w,         // parent
			420@20,    // bounds
			"damp",  // label
			ControlSpec(200, 10000, \lin, 1, 1360),     // controlSpec
			{ |ez| synth.set(\damping, ez.value) } // action
		);
		controls[\damp].numberView.maxDecimals = 3 ;

		order.add(\mod);
		controls[\mod] = EZSlider( w,         // parent
			420@20,    // bounds
			"mod",  // label
			ControlSpec(0.85, 1.15, \lin, 0.001, 1),     // controlSpec
			{ |ez| synth.set(\mod, ez.value) } // action
		);
		controls[\mod].numberView.maxDecimals = 3 ;

		StaticText(w, Rect(0,0, 200, 15)).string="Compressor/Expander";

		//COMPRESSOR
		order.add(\thresh);
		controls[\thresh] = EZSlider( w,         // parent
			420@20,    // bounds
			"thresh",  // label
			ControlSpec(0.001, 1, \lin, 0.001, 0.5),     // controlSpec
			{ |ez| synth.set(\thresh, ez.value) } // action
		);
		controls[\thresh].numberView.maxDecimals = 3 ;

		order.add(\slopeBelow);
		controls[\slopeBelow] = EZSlider( w,         // parent
			420@20,    // bounds
			"slpBelow",  // label
			ControlSpec(-2, 2, \lin, 0.01, 1),     // controlSpec
			{ |ez| synth.set(\slopeBelow, ez.value) } // action
		);
		controls[\slopeBelow].numberView.maxDecimals = 3 ;

		order.add(\slopeAbove);
		controls[\slopeAbove] = EZSlider( w,         // parent
			420@20,    // bounds
			"slpAbove",  // label
			ControlSpec(-2, 2, \lin, 0.01, 0.5),     // controlSpec
			{ |ez| synth.set(\slopeAbove, ez.value) } // action
		);
		controls[\slopeAbove].numberView.maxDecimals = 3 ;

		order.add(\clampTime);
		controls[\clampTime] = EZSlider( w,         // parent
			420@20,    // bounds
			"clpTime",  // label
			ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
			{ |ez| synth.set(\clampTime, ez.value) } // action
		);
		controls[\clampTime].numberView.maxDecimals = 3 ;

		order.add(\relaxTime);
		controls[\relaxTime] = EZSlider( w,         // parent
			420@20,    // bounds
			"rlxTime",  // label
			ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
			{ |ez| synth.set(\relaxTime, ez.value) } // action
		);
		controls[\relaxTime].numberView.maxDecimals = 3 ;

		StaticText(w, Rect(0,0, 200, 15)).string="Tremolo";

		order.add(\tremolo);
		controls[\tremolo] = EZSlider( w,         // parent
			420@20,    // bounds
			"freq",  // label
			ControlSpec(0, 60, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\freq, ez.value) } // action
		);
		controls[\tremolo].numberView.maxDecimals = 3 ;

		order.add(\drywet);
		controls[\drywet] = EZSlider( w,         // parent
			420@20,    // bounds
			"dry/wet",  // label
			ControlSpec(-1, 1, \lin, 0.01, 1.neg),     // controlSpec
			{ |ez| synth.set(\drywet, ez.value) } // action
		).valueAction_(-1);
		controls[\drywet].numberView.maxDecimals = 3 ;

		controls[\drywet].valueAction = -1;

		StaticText(w);

		order.add(\norm);
		controls[\norm] = EZSlider( w,         // parent
			420@20,    // bounds
			"normalize",  // label
			ControlSpec(0, 1, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\norm, ez.value) } // action
		);
		controls[\norm].numberView.maxDecimals = 3 ;

		order.add(\normlvl);
		controls[\normlvl] = EZSlider( w,         // parent
			420@20,    // bounds
			"norm_lvl",  // label
			ControlSpec(-1, 1, \lin, 0.01, 1.neg),     // controlSpec
			{ |ez| synth.set(\normlvl, ez.value) } // action
		).valueAction_(-1);

		controls[\normlvl].valueAction = -1;


		order.add(\vol);
		controls[\vol] = EZSlider( w,         // parent
			420@20,    // bounds
			"vol",  // label
			ControlSpec(0, 2, \lin, 0.001, 0.9),     // controlSpec
			{ |ez| synth.set(\vol, ez.value) } // action
		);
		controls[\drywet].numberView.maxDecimals = 3 ;


		{ super.preset( w.name.replace(" ", "_").toLower ) }.defer(0.05); // try to read and apply the default preset

		w.front;
	}

	midi {
		MIDIClient.init;
		MIDIIn.connectAll;
		MIDIdesynth.freeAll;
	}


	nanok { // old code needs update
		{
			// First NanoKontrol2 sliders
			MIDIdesynth.cc(\gainin, {arg ...args;
				{ controls[\gainin].valueAction_(args[0].linlin(0,127, 0, 2)) }.defer;
			}, 0); // match cc

			MIDIdesynth.cc(\feedback, {arg ...args;
				{ controls[\feedback].valueAction_(args[0].linlin(0,127, 0, 2)) }.defer;
			}, 1); // match cc

			MIDIdesynth.cc(\deltime, {arg ...args;
				{ controls[\deltime].valueAction_(args[0].linlin(0,127, 0, 500)) }.defer;
			}, 2); // match cc

			MIDIdesynth.cc(\amp, {arg ...args;
				{ controls[\amp].valueAction_(args[0].linlin(0,127, 0, 5)) }.defer;
			}, 3); // match cc

			MIDIdesynth.cc(\damp, {arg ...args;
				{ controls[\damp].valueAction_(args[0].linlin(0,127, 20, 10000)) }.defer;
			}, 4); // match cc

			/*	MIDIdesynth.cc(\revtimes, {arg ...args;
			{ controls[\revtimes].valueAction_(args[0].linlin(0,127, 0, 20)) }.defer;
			}, 4); // match cc*/

			MIDIdesynth.cc(\mod, {arg ...args;
				{ controls[\mod].valueAction_(args[0].linlin(0,127, 0.75, 1.25)) }.defer;
			}, 5); // match cc

			MIDIdesynth.cc(\vol, {arg ...args;
				{ controls[\vol].valueAction_(args[0].linlin(0,127, 0, 0.125)) }.defer;
			}, 6); // match cc

			// nanokontrol knobs

			// effects
			MIDIdesynth.cc(\tremolo, {arg ...args;
				{ controls[\tremolo].valueAction_(args[0].linlin(0,127, 0, 60)) }.defer;
			}, 16); // match cc
			MIDIdesynth.cc(\drywet, {arg ...args;
				{ controls[\drywet].valueAction_(args[0].linlin(0,127, 1.neg, 1)) }.defer;
			}, 17); // match cc
		}.defer(0.5);
	}

	// control

	setc {|control, val| {controls[control].valueAction = val}.defer}

	on {|val| this.setc(\on, 1) }
	off {|val| this.setc(\on, 0) }

	in {|val| this.setc(\in, val) }
	gainin {|val| this.setc(\gainin, val) }

	out {|val| this.setc(\out, val) }
	loop {|val| this.setc(\loop, val) }
	feedback {|val| this.setc(\feedback, val) }
	deltime {|val| this.setc(\deltime, val) }
	amp {|val| this.setc(\amp, val) }
	damp {|val| this.setc(\damp, val) }
	mod {|val| this.setc(\mod, val) }
	norm {|val| this.setc(\norm, val) }
	normlvl {|val| this.setc(\normlvl, val) }
	vol {|val| this.setc(\vol, val) }

	thresh {|val| this.setc(\thresh, val) }
	slopeBelow {|val| this.setc(\slopeBelow, val) }
	slopeAbove {|val| this.setc(\slopeAbove, val) }
	clampTime {|val| this.setc(\clampTime, val) }
	relaxTime {|val| this.setc(\relaxTime, val) }

	tremolo {|val| this.setc(\tremolo, val) }
	drywet {|val| this.setc(\drywet, val) }

	chord {|achord|
		if (achord.isNil, {^chord}, {
			chord.postln;
			chord = achord;
			synth.set(\chord, chord)
		});
	}

	gneck {|config|
		utils.add( GNeckGUI.new(this, path, config) )
	}

	eq {
		utils.add( ChannelEQ.new )
	}

	auto {|config|
		utils.add( AutoGUI.new(this, path, config) )
	 }

	chords {|config|
		utils.add( ChordGUI.new(this, path, chord, config) )
	}
}
