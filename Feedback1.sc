// synthdef based on https://sccode.org/1-U by Nathaniel Virgo

// to do:
// amp jumps when opening tremolo and other effects
// f.midi not connecting the midi channels
// interpolate slider values?
// range sliders not to trigger action when selection is dragged, only when mouse up after drag

// connect MIDI keyboard to \base
// delegate slider/knobs conexions to a user editable json file?
// Bend MIDI vales use the correct ranges


Feedback1 : EffectGUI {
	var auto, chord, utils, notchsynth, notchosc, notchlabel;
	var vlay, levels, inOSCFunc, outOSCFunc;

	*new {|path, preset|
		^super.new.init(path, preset);
	}

	init  {|apath, preset|
		super.init(apath);
		"Feedback1 init".postln;

		chord = [0,7,12,15,19,24]+40; //[0, 6.1, 10, 15.2, 22, 24 ];
		utils = List.new;//refs to GUI windows
		levels = List.new;

		midisetup = [[\gainin, 0], [\feedback, 1], [\amp, 2], [\deltime, 3],
			[\damp, 4], [\mod, 5], [\vol, 6]]; // control, MIDI effect channel

		Server.default.waitForBoot{
			// BASED ON https://sccode.org/1-U by Nathaniel Virgo
			SynthDef(\feed, {|in=2, out=0, loop=10, gainin=0, feedback=0.02, deltime=75, revtimes=5,
				amp=0.6, damping=1360, mod=1, vol=0.9, chord=#[ 40, 47, 52, 55, 59, 64 ], on=0|

				var del, minfreqs, freqs, sig, in_sig; //VARS
				var imp, delimp;

				imp = Impulse.kr(10);
				delimp = Delay1.kr(imp);

				in_sig = ((InFeedback.ar(loop, 2) + WhiteNoise.ar(0.001!2)) * feedback) + (In.ar(in, 2) * gainin);

				SendPeakRMS.kr(in_sig, 10, 3, '/inlvl'); // to monitor incoming feedback signal

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
				sig = HPF.ar(sig * amp.lag(0.05), 80);

				// and some not too harsh distortion
				sig = RLPFD.ar(sig, damping.lag(0.05) * [1, 1.1], 0.1, 0.5);
				sig = sig + sig.mean;

				// and finally a spot of reverb
				revtimes.do { // loop rev times
					del = 0.2.rand; // delayt and decayt
					sig = AllpassN.ar(sig, del, del, 5);
				};

				Out.ar(loop, sig); // feedback loop before the main output

				sig = Limiter.ar(sig * vol.lag(0.05), 1);

				SendPeakRMS.kr(sig, 10, 3, '/outlvl');

				Out.ar(out, sig * on)
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

			this.audio;

			super.gui("Feedback unit", 430@220); // init super gui buttons


			controls[\play] = Button(w, 22@18)
			.states_([
				[">", Color.white, Color.black],
				["||", Color.black, Color.red]
			])
			.action_({ arg butt;
				if (synth.isNil.not, {
					synth.set(\on, butt.value)
				});
			}).valueAction=1;

			w.view.decorator.nextLine;

			controls[\notch] = Button(w, 40@18)
			.states_([
				["notch:", Color.white, Color.black],
				["notch:", Color.black, Color.red]
			])
			.action_({ arg butt;
				if (notchsynth.isNil, {
					var uid = UniqueID.next;
					notchsynth = Synth.tail(Server.default, \autonotch,
						[\uid, uid, \rq, 0.15, \db, -60, \lag, 0.1,
							\in, controls[\loop].value, \out, controls[\loop].value]);
					notchosc = OSCFunc({|msg|
						//synth.set(\uid, uid); // very bad code. make sure it is the right one
						if (msg[2] == uid, {
							{ notchlabel.string = msg[3].asString.split($.)[0] }.defer;
						})
					}, '/pitch', Server.default.addr);
				}, {
					notchsynth.free; notchosc.free; notchsynth=nil
				});
			});

			notchlabel = StaticText(w, 30@18).string_("--");

			StaticText(w, 30@15).align_(\right).string_("Loop").resize_(7);
			controls[\loop] = PopUpMenu(w, Rect(10, 10, 40, 17))
			.items_( Array.fill(16, { arg i; i }) )
			.action_{|m|
				synth.set(\loop, m.value);
			}.valueAction = 10;



			vlay = VLayoutView(w, 200@17); // size
			4.do{|i|
				levels.add( LevelIndicator(vlay, 4).warning_(0.9).critical_(1.0).drawsPeak_(true) ); // 5 height each
				if (i==1, {CompositeView(vlay, 1)}); // plus 2px separator
			};

			w.view.decorator.nextLine;
			StaticText(w, 40@18).string_("Tools:").resize_(3);


			ActionButton(w,"gneck",{
				utils.add( GNeckGUI.new(this, path) );
			});

			ActionButton(w,"chord",{
				utils.add( ChordGUI.new(this, path, chord) );
			});

			//w.view.decorator.nextLine;
			StaticText(w, 5@18).string_("    ");

			ActionButton(w,"EQ",{
				try { utils.add( ChannelEQ.new) }
				{"cannot find ChannelEQ class. try installing it from http://github.com/enrike/supercollider-channeleq".postln}
			});

			ActionButton(w,"anotch",{
				path.postln;
				utils.add( AutoNotchGUI.new(path) );
			});

			ActionButton(w,"comp",{
				utils.add( CompanderGUI.new(path) );
			});

			ActionButton(w,"Dcomp",{
				utils.add( DCompanderGUI.new(path) );
			});

			ActionButton(w,"tremolo",{
				utils.add( TremoloGUI.new(path) );
			});

			ActionButton(w,"normalizer",{
				utils.add( NormalizerGUI.new(path) );
			});

			ActionButton(w,"fshift",{
				utils.add( FreqShiftGUI.new(path) );
			});


			w.view.decorator.nextLine;

			// SLIDERS //
			order.add(\gainin);
			controls[\gainin] = EZSlider( w,         // parent
				slbounds,    // bounds
				"gain in",  // label
				ControlSpec(0, 2, \lin, 0.001, 0),     // controlSpec
				{ |ez| synth.set(\gainin, ez.value) }
			);
			controls[\gainin].numberView.maxDecimals = 3 ;

			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"gainin", func:{|data|
					controls[\gainin].valueAction = controls[\gainin].controlSpec.map(data.asFloat);
				} );
			});

			order.add(\feedback);
			controls[\feedback] = EZSlider( w,         // parent
				slbounds,    // bounds
				"feedback",  // label
				ControlSpec(0, 1, \lin, 0.001, 0.02),     // controlSpec
				{ |ez| synth.set(\feedback, ez.value) }
			);
			controls[\feedback].numberView.maxDecimals = 3 ;


			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"feedback", func:{|data|
					controls[\feedback].valueAction = controls[\feedback].controlSpec.map(data.asFloat);
				} );
			});

			order.add(\amp);
			controls[\amp] = EZSlider( w,         // parent
				slbounds,    // bounds
				"amp",  // label
				ControlSpec(0, 2, \lin, 0.001, 0.6),     // controlSpec
				{ |ez| synth.set(\amp, ez.value) } // action
			);
			controls[\amp].numberView.maxDecimals = 3 ;


			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"amp", func:{|data|
					controls[\amp].valueAction = controls[\amp].controlSpec.map(data.asFloat);
				} );
			});

			order.add(\deltime);
			controls[\deltime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"deltime",  // label
				ControlSpec(0, 500, \lin, 0.001, 75),     // controlSpec
				{ |ez| synth.set(\deltime, ez.value) } // action
			);
			controls[\deltime].numberView.maxDecimals = 3 ;

			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"deltime", func:{|data|
					controls[\deltime].valueAction = controls[\deltime].controlSpec.map(data.asFloat);
				} );
			});

			order.add(\damp);
			controls[\damp] = EZSlider( w,         // parent
				slbounds,    // bounds
				"damp",  // label
				ControlSpec(200, 10000, \lin, 1, 1360),     // controlSpec
				{ |ez| synth.set(\damping, ez.value) } // action
			);
			controls[\damp].numberView.maxDecimals = 3 ;

			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"damp", func:{|data|
					controls[\damp].valueAction = controls[\damp].controlSpec.map(data.asFloat);
				} );
			});

			order.add(\mod);
			controls[\mod] = EZSlider( w,         // parent
				slbounds,    // bounds
				"mod",  // label
				ControlSpec(0.85, 1.15, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\mod, ez.value) } // action
			);
			controls[\mod].numberView.maxDecimals = 3 ;

			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"mod", func:{|data|
					controls[\mod].valueAction = controls[\mod].controlSpec.map(data.asFloat);
				} );
			});

			order.add(\vol);
			controls[\vol] = EZSlider( w,         // parent
				slbounds,    // bounds
				"vol",  // label
				ControlSpec(0, 2, \lin, 0.001, 0.9),     // controlSpec
				{ |ez| synth.set(\vol, ez.value) } // action
			);
			controls[\vol].numberView.maxDecimals = 3 ;

			ActionButton(w,"p",{
				ParamWinGUI.new(path:path, name:"vol", func:{|data|
					controls[\vol].valueAction = controls[\vol].controlSpec.map(data.asFloat);
				} );
			});

			if (preset.isNil.not, { // not loading a config file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
				this.updateall; // make sure synth is updated AFTER all presets are read
			});

			["FEEDBACK1 READY", synth].postln;
		}
	}

	updateall {//
		[\gainin, \feedback, \amp, \deltime, \damp, \mod, \vol, \in, \out].do{|ctrl|
			synth.set(ctrl, controls[ctrl].value);
		}
	}

	audio {
		synth = Synth(\feed, [\chord, chord]);
		synth.postln;
		Server.default.sync;
	}

	midi {
		super.midi(midisetup);
		MIDIdef.cc(\scramble, {arg ...args;
			if (args[0]==127, {
				{ this.scramble }.defer;
			})
		}, 46); // match cc
		"MIDI 26 > scramble chord".postln;

		MIDIdef.noteOn(\noteon, {arg ...args; // match any noteOn
			var notes = chord - chord[0];
			this.chord(notes + args[1])
		});
	}

	close {
		"disconnecting MIDIin".postln;
		MIDIIn.disconnectAll;

		"closing down Feedback unit and all opened utils:".postln;
		utils.postln;

		inOSCFunc.free;
		outOSCFunc.free;
		notchsynth.free;
		utils.do{|ut|
			("-"+ut).postln;
			ut.close
		};
		super.close;
	}

	// control
	scramble { this.chord(chord.scramble) }

	setc {|control, val| { controls[control].valueAction = val}.defer }

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
	vol {|val| this.setc(\vol, val) }

	notch {|flag=1|
		controls[\notch].valueAction_(flag)
	}

	chord {|achord|
		if (achord.isNil, {^chord}, {
			chord.postln;
			chord = achord;
			synth.set(\chord, chord)
		});
	}

	rcontrol {|name|
		var rmin = controls[name.asSymbol].controlSpec.clipLo.asFloat;
		var rmax = controls[name.asSymbol].controlSpec.clipHi.asFloat;
		controls[name.asSymbol].valueAction = rrand(rmin, rmax)
	}

	gneck {|config=\default|
		utils.add( GNeckGUI.new(path, config) )
	}

	eq {|bus|
		utils.add( ChannelEQ.new(bus:bus) )
	}

	auto {|config=\default|
		utils.add( AutoGUI.new(path, config) )
	}

	ch {|config=\default|
		utils.add( ChordGUI.new(path, chord, config) )
	}

	anotch {|config=\default|
		utils.add( AutoNotchGUI.new(path, config) )
	}

	comp {|config=\default|
		utils.add( CompanderGUI.new(path, config) )
	}
	dcomp {|config=\default|
		utils.add( DCompanderGUI.new(path, config) )
	}
	trem {|config=\default|
		utils.add( TremoloGUI.new(path, config) )
	}

	fshift {|config=\default|
		utils.add( FreqShiftGUI.new(path, config) )
	}
}
