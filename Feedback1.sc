// synthdef based on https://sccode.org/1-U by Nathaniel Virgo

// to do:
// interpolate values?

// connect MIDI keyboard to \base
// connect S M R buttons in nanokontrol to actions R -> single random
// delegate slider/knobs conexions to a user editable json file
// Bend MIDI vales use the correct ranges


Feedback1 : EffectGUI {
	var auto, chord, <synth, path, utils;
	var vlay, levels, inOSCFunc, outOSCFunc, outmon;

	*new {
		^super.new.initFeedback1();
	}

	initFeedback1  {
		chord = [0,7,12,15,19,24]+40; //[0, 6.1, 10, 15.2, 22, 24 ];
		utils = List.new;//refs to GUI windows
		levels = List.new;
		path = thisProcess.nowExecutingPath.dirname;
		Server.default.waitForBoot{
			this.audio;
		}
	}

	audio {
		// BASED ON https://sccode.org/1-U by Nathaniel Virgo
		SynthDef(\feed, {|in=2, out=0, loop=10, gainin=0, feedback=0.02, deltime=75,
			revtimes=5, amp=0.6, damping=1360, mod=1, vol=0.9, chord=#[ 40, 47, 52, 55, 59, 64 ],
			thresh=0.5, slopeBelow=1, slopeAbove=0.5, clampTime=0.01, relaxTime=0.01,
			norm=0, normlvl= -1, freq=0, drywet= -1, on=0|

			var del, minfreqs, freqs, dry, nsig, sig, in_sig, outmon; //VARS
			var imp, delimp;

			imp = Impulse.kr(10);
			delimp = Delay1.kr(imp);

			in_sig = ((InFeedback.ar(loop, 2) + WhiteNoise.ar(0.001!2)) * feedback) + (In.ar(in, 2) * gainin);

			// measure rms and Peak
			SendPeakRMS.kr(in_sig, 10, 3, '/inlvl'); // this is to monitor the signal that enters the feedback unit

			// delay due to distance from amp - I chose 0.05s, or 20Hz
			sig = DelayN.ar(in_sig, 1/10-ControlDur.ir, 1/deltime-ControlDur.ir);

			freqs = chord.midicps;
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

		SynthDef(\outmon, {|out=2| // separated to monitor the absolute signal going out of SC after utils etc...
			SendPeakRMS.kr(In.ar(out, 2), 10, 3, '/outlvl');
		}).send;

		Server.default.sync; // wait until synthdef is loaded

		inOSCFunc = OSCFunc({|msg| {
			levels[..1].do({|lvl, i| // in levels
				lvl.peakLevel = msg[3..][i*2].ampdb.linlin(-80, 0, 0, 1, \min);
				lvl.value = msg[3..][(i*2)+1].ampdb.linlin(-80, 0, 0, 1);
			});
		}.defer;
		}, '/inlvl', Server.default.addr);

		outOSCFunc = OSCFunc({|msg| {
			levels[2..].do({|lvl, i| // out levels
				lvl.peakLevel = msg[3..][i*2].ampdb.linlin(-80, 0, 0, 1, \min);
				lvl.value = msg[3..][(i*2)+1].ampdb.linlin(-80, 0, 0, 1);
			});
		}.defer;
		}, '/outlvl', Server.default.addr);

		outmon = Synth.tail(Server.default, \outmon); // TO TAIL to monitor the absolute sound out
		synth = Synth(\feed, [\chord, chord]);

		Server.default.sync;
		this.gui;
	}


	gui {
		//Server.default.sync;
		// GUI ////////////////////////
		super.gui("Feedback unit", 430@465); // init super gui buttons
		w.onClose = {
			"closing down Feedback unit and utils".postln;

			inOSCFunc.free;
			outOSCFunc.free;
			synth.free;
			outmon.free;
			try { utils.collect(_.close) }
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
			outmon.set(\out, m.value);
		}.valueAction = 0; //

		controls[\on] = Button(w, 22@18)
		.states_([
			["on", Color.white, Color.black],
			["off", Color.black, Color.red]
		])
		.action_({ arg butt;
			synth.set(\on, butt.value)
		});

		vlay = VLayoutView(w, 150@17); // size
		4.do{|i|
			levels.add( LevelIndicator(vlay, 4).warning_(0.9).critical_(1.0).drawsPeak_(true) ); // 5 height each
			if (i==1, {CompositeView(vlay, 1)}); // plus 2px separator
		};


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

		ActionButton(w,"anotch",{
			utils.add( AutoNotchGUI.new(this, path) );
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
			ControlSpec(0, 2, \lin, 0.001, 0.6),     // controlSpec
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
			ControlSpec(0, 50, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\freq, ez.value) } // action
		);
		controls[\tremolo].numberView.maxDecimals = 3 ;

		order.add(\drywet);
		controls[\drywet] = EZSlider( w,         // parent
			420@20,    // bounds
			"dry/wet",  // label
			ControlSpec(-1, 1, \lin, 0.01, -1),     // controlSpec
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
			ControlSpec(-1, 1, \lin, 0.01, -1),     // controlSpec
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

		//{ super.preset( w.name.replace(" ", "_").toLower ) }.defer(0.05); // try to read and apply the default preset

		super.preset( w.name.replace(" ", "_").toLower );
		w.front;
	}

	nanok { // old code needs update
		{
			var setup = List.new;
			setup = [[\gainin, 0], [\feedback, 1], [\deltime, 2], [\amp, 3], [\damp, 4], [\mod, 5], [\vol, 6],
				[\tremolo,16], [\drywet, 17]
			];
			MIDIClient.init;
			MIDIIn.connectAll;
			MIDIdef.freeAll;


			// nanokontrol knobs

			/*			// effects
			MIDIdef.cc(\tremolo, {arg ...args;
			{ controls[\tremolo].valueAction_(args[0].linlin(0,127, 0, 60)) }.defer;
			}, 16); // match cc
			MIDIdef.cc(\drywet, {arg ...args;
			{ controls[\drywet].valueAction_(args[0].linlin(0,127, 1.neg, 1)) }.defer;
			}, 17); // match cc*/

			setup.do{|pair, i|
				("MIDI"+pair[1].asString+">"+pair[0].asString).postln;
				this.setupControl(pair[0], pair[1]);
			}
		}.defer(0.5);
	}

	setupControl {|control, chanel|
		MIDIdef.cc(control, {arg ...args;
			{
				var min = controls[control].controlSpec.minval;
				var max = controls[control].controlSpec.maxval;
				controls[control].valueAction_(args[0].linlin(0,127, min, max))
			}.defer;
		}, chanel); // match cc
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

	eq {|bus|
		utils.add( ChannelEQ.new(bus:bus) )
	}

	auto {|config|
		utils.add( AutoGUI.new(this, path, config) )
	}

	chords {|config|
		utils.add( ChordGUI.new(this, path, chord, config) )
	}

	anotch {|config|
		utils.add( AutoNotchGUI.new(this, path, config) )
	}
}
