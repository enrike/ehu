// synthdef based on https://sccode.org/1-U by Nathaniel Virgo




Feedback1 : EffectGUI {
	var autotask, chord, <synth;

	*new {
		^super.new.initFeedback1();
	}

	initFeedback1  {
		this.auto;
		chord = [0,7,12,15,19,24]; //[0, 6.1, 10, 15.2, 22, 24 ];
	}

	chord {|achord|
		//if (achord.insNil, {^chord});
		chord = achord;
		synth.set(\chord, chord)
	}

	audio {
		Server.default.waitForBoot({

			// BASED ON https://sccode.org/1-U by Nathaniel Virgo

			SynthDef(\feed, {|in=0, out=0, loop=10, gainin=0, feedback=0, deltime=75, freqdiv=4,
				revtimes=5, amp=0.4, damping=1360, mod=0.75, base=64, drywet=(0.8.neg),
				vol=0.125, chord=#[0,7,12,15,19,24]|

				var del, minfreqs, freqs, dry; //VARS
				var sig = ((InFeedback.ar(loop, 2) + WhiteNoise.ar(0.001!2)) * feedback) + (In.ar(in, 2) * gainin);

				// delay due to distance from amp - I chose 0.05s, or 20Hz
				sig = DelayN.ar(sig, 1/10-ControlDur.ir, 1/deltime-ControlDur.ir);

				// guitar string frequencies - for some reason I had to pitch them down
				freqs = (base+chord).midicps/freqdiv;

				// whammy bar modulates freqs:
				minfreqs = freqs * 0.5;
				freqs = freqs * mod;

				// 6 comb filters emulate the strings' resonances
				sig = CombN.ar(sig!6, 1/minfreqs, 1/freqs, 8).mean;

				// a little filtering
				sig = LPF.ar(sig, 8000);
				sig = HPF.ar(sig * amp, 80);

				// and some not too harsh distortion
				sig = RLPFD.ar(sig, damping * [1, 1.1], 0.1, 0.5);
				sig = sig + sig.mean;

				// and finally a spot of reverb
				dry = sig;

				revtimes.do { //rev times
					del = 0.2.rand; // delayt and decayt
					sig = AllpassN.ar(sig, del, del, 5);
				};

				Out.ar(loop, sig); // feedback loop before the main output

				sig = XFade2.ar(dry, sig, drywet);
				sig = Limiter.ar(sig);
				sig = Normalizer.ar(sig, 1, 0.01);

				Out.ar(out, sig * vol)
			}).load;

			// launch synth
			synth = Synth(\feed, [\chord, chord]); // e minor for instance
			"-------AUDIO READY-------".postln
		})
	}

	auto {
		autotask = Task({
			inf.do({ arg i;
				//{controls[\deltime].valueAction_(500.rand)}.defer; //update GUI
				this.update(\deltime, 500.rand);
				[1,5,8].choose.wait;
			});
		});
	}

	gui {
		// GUI ////////////////////////
		super.gui("Feedback unit", Rect(0,0, 310, 260)); // init super gui buttons
		w.onClose = {
			synth.free;
		};


		StaticText(w, 12@18).align_(\right).string_("In").resize_(7);
		controls[\in] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\in, m.value);
		}
		.value_(2); // default to sound in

		StaticText(w, 30@18).align_(\right).string_("Loop").resize_(7);
		controls[\loop] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\loop, m.value);
		}
		.value_(10);

		StaticText(w, 23@18).align_(\right).string_("Out").resize_(7);
		controls[\out] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\out, m.value);
		}
		.value_(0); //


		Button(w, Rect(20, 20, 55, 20))
		.states_([
			["Auto delT", Color.white, Color.black],
			["Auto delT", Color.black, Color.red],
		])
		.action_({ arg butt;
			if (butt.value==1, {
				autotask.start
			},{
				autotask.stop
			});
		});

		ActionButton(w,"scramble",{
			chord = chord.scramble;
			synth.set(\chord, chord);
		});

		 // SLIDERS //

		controls[\gainin] = EZSlider( w,         // parent
			300@20,    // bounds
			"gain in",  // label
			ControlSpec(0, 2, \lin, 0.001, 0, \amp),     // controlSpec
			{ |ez| synth.set(\gainin, ez.value) } // action
		);

		StaticText(w);

		controls[\feedback] = EZSlider( w,         // parent
			300@20,    // bounds
			"feedback",  // label
			ControlSpec(0, 2, \lin, 0.001, 0, \amp),     // controlSpec
			{ |ez| synth.set(\feedback, ez.value) } // action
		);

		controls[\deltime] = EZSlider( w,         // parent
			300@20,    // bounds
			"deltime",  // label
			ControlSpec(0, 500, \lin, 0.001, 75, \del),     // controlSpec
			{ |ez| synth.set(\deltime, ez.value) } // action
		);
		/*	 controls[\del] = EZSlider( w,         // parent
		300@20,    // bounds
		"del",  // label
		ControlSpec(0, 3, \lin, 0.001, 0.05, \del),     // controlSpec
		{ |ez| synth.set(\del, ez.value) } // action
		);*/
		controls[\amp] = EZSlider( w,         // parent
			300@20,    // bounds
			"amp",  // label
			ControlSpec(0, 3, \lin, 0.001, 0.6, \amp),     // controlSpec
			{ |ez| synth.set(\amp, ez.value) } // action
		);
		controls[\damp] = EZSlider( w,         // parent
			300@20,    // bounds
			"damp",  // label
			ControlSpec(200, 10000, \lin, 1, 1360, \damp),     // controlSpec
			{ |ez| synth.set(\damping, ez.value) } // action
		);
		/*controls[\revtimes] = EZSlider( w,         // parent
		300@20,    // bounds
		"revtimes",  // label
		ControlSpec(0, 20, \lin, 1, 3, \revtimes),     // controlSpec
		{ |ez| synth.set(\revtimes, ez.value) } // action
		);*/
		// FREQ //
		controls[\mod] = EZSlider( w,         // parent
			300@20,    // bounds
			"mod",  // label
			ControlSpec(0.75, 1.25, \lin, 0.001, 1, \mod),     // controlSpec
			{ |ez| synth.set(\mod, ez.value) } // action
		);
		controls[\base] = EZSlider( w,         // parent
			300@20,    // bounds
			"base",  // label
			ControlSpec(1, 128, \lin, 1, 64, \base),     // controlSpec
			{ |ez| synth.set(\base, ez.value) } // action
		);
		/*	 controls[\freqdiv] = EZSlider( w,         // parent
		300@20,    // bounds
		"freqdiv",  // label
		ControlSpec(1, 10, \lin, 1, 4, \freqdiv),     // controlSpec
		{ |ez| synth.set(\freqdiv, ez.value) } // action
		);*/
		controls[\vol] = EZSlider( w,         // parent
			300@20,    // bounds
			"vol",  // label
			ControlSpec(0, 2, \lin, 0.001, 0.3, \vol),     // controlSpec
			{ |ez| synth.set(\vol, ez.value) } // action
		);

	//	StaticText(w);

/*		controls[\tremolo] = EZSlider( w,         // parent
			300@20,    // bounds
			"tremolo",  // label
			ControlSpec(0, 60, \lin, 0.001, 4.25, \tremolo),     // controlSpec
			{ |ez| synth.set(\trem, ez.value) } // action
		);*/
		controls[\drywet] = EZSlider( w,         // parent
			300@20,    // bounds
			"rev",  // label
			ControlSpec(-1, 1, \lin, 0.01, 0.96.neg, \drywet),     // controlSpec
			{ |ez| synth.set(\drywet, ez.value) } // action
		);

		w.front;


		controls[\drywet].valueAction = -0.95; // for some reason the controlspec does not take negative values as defailt values.

	}


	midi {
		MIDIClient.init;
		MIDIIn.connectAll;
		MIDIdesynth.freeAll;
	}

	nanok {
		{
			// First NanoKontrol2 sliders
			MIDIdesynth.cc(\gainin, {arg ...args;
				{ controls[\gainin].valueAction_(args[0].linlin(0,127, 0, 2)) }.defer;
			}, 0); // match cc

			MIDIdesynth.cc(\deltime, {arg ...args;
				{ controls[\deltime].valueAction_(args[0].linlin(0,127, 0, 500)) }.defer;
			}, 1); // match cc

			MIDIdesynth.cc(\amp, {arg ...args;
				{ controls[\amp].valueAction_(args[0].linlin(0,127, 0, 5)) }.defer;
			}, 2); // match cc

			MIDIdesynth.cc(\damp, {arg ...args;
				{ controls[\damp].valueAction_(args[0].linlin(0,127, 20, 10000)) }.defer;
			}, 3); // match cc

			/*	MIDIdesynth.cc(\revtimes, {arg ...args;
			{ controls[\revtimes].valueAction_(args[0].linlin(0,127, 0, 20)) }.defer;
			}, 4); // match cc*/

			MIDIdesynth.cc(\mod, {arg ...args;
				{ controls[\mod].valueAction_(args[0].linlin(0,127, 0.75, 1.25)) }.defer;
			}, 4); // match cc

			MIDIdesynth.cc(\base, {arg ...args;
				{ controls[\base].valueAction_(args[0]+1) }.defer;
			}, 5); // match cc

			// nanokontrol knobs

			/*MIDIdesynth.cc(\freqdiv, {arg ...args;
			{ controls[\freqdiv].valueAction_(args[0].linlin(0,127, 1, 10)) }.defer;
			}, 7); // match cc
			*/

			MIDIdesynth.cc(\vol, {arg ...args;
				{ controls[\vol].valueAction_(args[0].linlin(0,127, 0, 0.125)) }.defer;
			}, 7); // match cc

			// effects
			MIDIdesynth.cc(\tremolo, {arg ...args;
				{ controls[\tremolo].valueAction_(args[0].linlin(0,127, 0, 60)) }.defer;
			}, 16); // match cc
			MIDIdesynth.cc(\effect, {arg ...args;
				{ controls[\effect].valueAction_(args[0].linlin(0,127, 1.neg, 1)) }.defer;
			}, 17); // match cc
		}.defer(0.5);
	}

	// control
	in {|val| controls[\in].valueAction = val }
	feedback {|val| controls[\feedback].valueAction = val }
	ingain {|val| controls[\gainin].valueAction = val }
	deltime {|val| controls[\deltime].valueAction = val }
	amp {|val| controls[\amp].valueAction = val }
	damp {|val| controls[\damp].valueAction = val }
	mod {|val| controls[\mod].valueAction = val }
	base {|val| controls[\base].valueAction = val }
	vol {|val| controls[\vol].valueAction = val }
	tremolo {|val| controls[\tremolo].valueAction = val }
	drywet {|val| controls[\effect].valueAction = val }
}