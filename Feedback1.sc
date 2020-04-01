// synthdef based on https://sccode.org/1-U by Nathaniel Virgo


EffectGUI {

	classvar effectsGroup;

	var <controls, path, w;

	/*
	a base class for a GUI window with widgets that can save/restore the configuration of the widgets.
	To be extended adding widgets and calling .front

	widgets must be added to controls dictionary like this

	controls[\gainin] = EZSlider( w,         // parent
	300@20,    // bounds
	"in gain",  // label
	ControlSpec(0, 2, \lin, 0.001, 0, \amp),     // controlSpec
	{ |ez| synth.set(\gain, ez.value) } // action
	);
	*/

	*new {
		^super.new.initEffectGUI;
	}

	initEffectGUI {
		controls = Dictionary.new;
		path = thisProcess.nowExecutingPath.dirname;
		effectsGroup = Group.new(Server.default, \addToTail); // was addAfter
	}

	gui { |name="", bounds=#[0,0, 310, 120]| // run this if you want to have open and save buttons in the window
		w = Window.new(name, bounds).alwaysOnTop=true;
		w.view.decorator=FlowLayout(w.view.bounds);
		w.view.decorator.gap=2@2;

		ActionButton(w,"S",{
			this.save;
		});
		ActionButton(w,"O",{
			this.open;
		});
	}

	update {|name, value| // control widgets remotely
		{controls[name].valueAction = value}.defer
	}

	save {
		var data = Dictionary.new, name="", filename;
		if (w.isNil.not, {name=w.name.replace(" ", "_")}); //prepend the windows name
		filename = name++"_"++Date.getDate.stamp++".preset";

		controls.keysValuesDo { |key, widget|
			data.put(key, widget.value)
		};

		data.writeArchive(path ++ Platform.pathSeparator ++ filename);
	}

	open {
		var data;
		FileDialog({ |path|
			data = Object.readArchive(path);
			data.keysValuesDo{ |key, value|
				try {
					{controls[key].valueAction = value}.defer // wait for QT
				}{|er| er.postln}
			};
		},
		fileMode: 0,
		stripResult: true,
		path: path);
	}
}


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

			SynthDef(\feed, {|in=2, out=0, loop=10, gainin=0, feedback=0, deltime=75, freqdiv=4, revtimes=5, amp=0.4,
				damping=1360, mod=1, base=64, vol=0.3, chord=#[0,7,12,15,19,24],
				thresh=0.5, slopeBelow=1, slopeAbove=0.5, clampTime=0.01, relaxTime=0.01, limit=0.5, norm=0.9,
				freq=0, drywet=(1.neg)|

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
				revtimes.do { // loop rev times
					del = 0.2.rand; // delayt and decayt
					sig = AllpassN.ar(sig, del, del, 5);
				};

				sig = Compander.ar(sig, sig, thresh, slopeBelow, slopeAbove, clampTime, relaxTime);

				Out.ar(loop, sig); // feedback loop before the main output

				dry = sig;
				sig = sig * SinOsc.ar(freq);
				sig = XFade2.ar(dry, sig, drywet);

				//sig = Limiter.ar(sig, limit);
				sig = Normalizer.ar(sig, norm, 0.01);

				Out.ar(out, sig * vol)
			}).load;

			{ synth = Synth(\feed, [\chord, chord]) }.defer(0.01);
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
		super.gui("Feedback unit", Rect(0,0, 310, 470)); // init super gui buttons
		w.onClose = {
			synth.free;
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
			ControlSpec(0, 2, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\gainin, ez.value) } // action
		);

		StaticText(w, Rect(0,0, 80, 15)).string="Feedback";

		controls[\feedback] = EZSlider( w,         // parent
			300@20,    // bounds
			"feedback",  // label
			ControlSpec(0, 2, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\feedback, ez.value) } // action
		);

		controls[\deltime] = EZSlider( w,         // parent
			300@20,    // bounds
			"deltime",  // label
			ControlSpec(0, 500, \lin, 0.001, 75),     // controlSpec
			{ |ez| synth.set(\deltime, ez.value) } // action
		);

		controls[\amp] = EZSlider( w,         // parent
			300@20,    // bounds
			"amp",  // label
			ControlSpec(0, 3, \lin, 0.001, 0.6),     // controlSpec
			{ |ez| synth.set(\amp, ez.value) } // action
		);

		controls[\damp] = EZSlider( w,         // parent
			300@20,    // bounds
			"damp",  // label
			ControlSpec(200, 10000, \lin, 1, 1360),     // controlSpec
			{ |ez| synth.set(\damping, ez.value) } // action
		);

		controls[\mod] = EZSlider( w,         // parent
			300@20,    // bounds
			"mod",  // label
			ControlSpec(0.75, 1.25, \lin, 0.01, 1),     // controlSpec
			{ |ez| synth.set(\mod, ez.value) } // action
		);

		controls[\base] = EZSlider( w,         // parent
			300@20,    // bounds
			"base",  // label
			ControlSpec(1, 128, \lin, 1, 64),     // controlSpec
			{ |ez| synth.set(\base, ez.value) } // action
		);

		StaticText(w, Rect(0,0, 200, 15)).string="Compressor";

		//COMPRESSOR
		controls[\thresh] = EZSlider( w,         // parent
			300@20,    // bounds
			"thresh",  // label
			ControlSpec(0.001, 1, \lin, 0.01, 0.5),     // controlSpec
			{ |ez| synth.set(\thresh, ez.value) } // action
		);

		controls[\slopeBelow] = EZSlider( w,         // parent
			300@20,    // bounds
			"slpBelow",  // label
			ControlSpec(-2, 2, \lin, 0.01, 1),     // controlSpec
			{ |ez| synth.set(\slopeBelow, ez.value) } // action
		);

		controls[\slopeAbove] = EZSlider( w,         // parent
			300@20,    // bounds
			"slpAbove",  // label
			ControlSpec(-2, 2, \lin, 0.01, 0.5),     // controlSpec
			{ |ez| synth.set(\slopeAbove, ez.value) } // action
		);

		controls[\clampTime] = EZSlider( w,         // parent
			300@20,    // bounds
			"clpTime",  // label
			ControlSpec(0, 1, \lin, 0.01, 0.01),     // controlSpec
			{ |ez| synth.set(\clampTime, ez.value) } // action
		);

		controls[\relaxTime] = EZSlider( w,         // parent
			300@20,    // bounds
			"rlxTime",  // label
			ControlSpec(0, 1, \lin, 0.01, 0.01),     // controlSpec
			{ |ez| synth.set(\relaxTime, ez.value) } // action
		);

		StaticText(w, Rect(0,0, 200, 15)).string="Tremolo";

		controls[\tremolo] = EZSlider( w,         // parent
			300@20,    // bounds
			"freq",  // label
			ControlSpec(0, 60, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\freq, ez.value) } // action
		);

		controls[\drywet] = EZSlider( w,         // parent
			300@20,    // bounds
			"dry/wet",  // label
			ControlSpec(-1, 1, \lin, 0.01, 1.neg),     // controlSpec
			{ |ez| synth.set(\drywet, ez.value) } // action
		).valueAction_(-1);

		StaticText(w);

		controls[\norm] = EZSlider( w,         // parent
			300@20,    // bounds
			"normalize",  // label
			ControlSpec(0, 1, \lin, 0.001, 0.5),     // controlSpec
			{ |ez| synth.set(\norm, ez.value) } // action
		);

		controls[\vol] = EZSlider( w,         // parent
			300@20,    // bounds
			"vol",  // label
			ControlSpec(0, 2, \lin, 0.001, 0.9),     // controlSpec
			{ |ez| synth.set(\vol, ez.value) } // action
		);


		w.front;
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

			MIDIdesynth.cc(\base, {arg ...args;
				{ controls[\base].valueAction_(args[0]+1) }.defer;
			}, 6); // match cc

			MIDIdesynth.cc(\vol, {arg ...args;
				{ controls[\vol].valueAction_(args[0].linlin(0,127, 0, 0.125)) }.defer;
			}, 7); // match cc

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

	in {|val| this.setc(\in, val) }
	gainin {|val| this.setc(\gainin, val) }

	out {|val| this.setc(\out, val) }
	loop {|val| this.setc(\loop, val) }
	feedback {|val| this.setc(\feedback, val) }
	deltime {|val| this.setc(\deltime, val) }
	amp {|val| this.setc(\amp, val) }
	damp {|val| this.setc(\damp, val) }
	mod {|val| this.setc(\mod, val) }
	base {|val| this.setc(\base, val) }
	vol {|val| this.setc(\vol, val) }

	thresh {|val| this.setc(\thresh, val) }
	slopeBelow {|val| this.setc(\slopeBelow, val) }
	slopeAbove {|val| this.setc(\slopeAbove, val) }
	clampTime {|val| this.setc(\clampTime, val) }
	relaxTime {|val| this.setc(\relaxTime, val) }

	tremolo {|val| this.setc(\tremolo, val) }
	drywet {|val| this.setc(\drywet, val) }
}


// superclass
