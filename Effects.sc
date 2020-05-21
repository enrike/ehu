
/*
Launcher.new
TremoloGUI.new;
NormalizerGUI.new;
LimiterGUI.new;
CompanderGUI.new;
AutoNotch.new;
*/


Launcher : BaseGUI {
	var utils;

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		utils = List.new;//refs to GUI windows

		super.gui("Launcher", 120@100);

		/*		w.onClose = {
		utils.do{|ut|
		("-"+ut).postln;
		ut.close
		};
		super.close;
		};*/

		w.view.decorator.nextLine;

		ActionButton(w,"feedback",{
			utils.add( Feedback1.new(path) );
		});

		ActionButton(w,"EQ",{
			try { utils.add( ChannelEQ.new) }
			{"cannot find ChannelEQ class. try installing it from http://github.com/enrike/supercollider-channeleq".postln}
		});

		ActionButton(w,"anotch",{
			utils.add( AutoNotchGUI.new(path) );
		});

		ActionButton(w,"compander",{
			utils.add( CompanderGUI.new(path) );
		});

		ActionButton(w,"tremolo",{
			utils.add( TremoloGUI.new(path) );
		});

		ActionButton(w,"normalizer",{
			utils.add( NormalizerGUI.new(path) );
		});

		ActionButton(w,"limiter",{
			utils.add( LimiterGUI.new(path) );
		});

		super.preset( w.name ); // try to load default preset
	}

	close {}
}


TremoloGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		Server.default.waitForBoot{
			SynthDef(\trem, {|in=0, out=0, freq=0, drywet=1|
				var sig = In.ar(in, 2);
				var dry = sig;
				sig = sig * SinOsc.ar(freq);
				sig = XFade2.ar(dry, sig, drywet);
				Out.ar(out, sig);
			}).load;
			Server.default.sync;
			synth = Synth.tail(Server.default, \trem) ;
			Server.default.sync;


			super.gui("Tremolo", 430@70); // init super gui w

			w.view.decorator.nextLine;

			order.add(\tremolo);
			controls[\tremolo] = EZSlider( w,         // parent
				420@20,    // bounds
				"freq",  // label
				ControlSpec(0.001, 50, \lin, 0.001, 0.1),     // controlSpec
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

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}
}















////////////////////////////////////////////////
NormalizerGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		Server.default.waitForBoot{
			SynthDef(\norm, {|in=0, out=0, level=0, drywet= -1|
				var sig = In.ar(in, 2) ;
				var dry = sig;
				sig = Normalizer.ar(sig, level, 0.01);
				sig = XFade2.ar(dry, sig, drywet);
				Out.ar(out, sig);
			}).load;

			Server.default.sync;

			synth = Synth.tail(Server.default, \norm);

			Server.default.sync;

			super.gui("Normalizer", Rect(310,320, 430, 70)); // init super gui w

			w.view.decorator.nextLine;

			controls[\level] = EZSlider( w,         // parent
				420@20,    // bounds
				"level",  // label
				ControlSpec(0, 1, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\level, ez.value) } // action
			);
			controls[\drywet] = EZSlider( w,         // parent
				420@20,    // bounds
				"dry/wet",  // label
				ControlSpec(-1, 1, \lin, 0.01, 1.neg),     // controlSpec
				{ |ez| synth.set(\drywet, ez.value) } // action
			).valueAction_(-1);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}
}



/*LimiterGUI : EffectGUI {

*new {|exepath, preset=\default|
^super.new.init(exepath, preset);
}

init {|exepath, preset|
super.init(path);*/

LimiterGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		Server.default.waitForBoot{
			SynthDef(\lim, {|in=0, out=0, level=0, drywet=1|
				var sig = In.ar(in, 2) ;
				var dry = sig;
				sig = Limiter(sig, level);
				sig = XFade2.ar(dry, sig, drywet);
				Out.ar(out, sig);
			}).load;

			Server.default.sync;
			synth = Synth.tail(Server.default, \lim);
			Server.default.sync;


			super.gui("Limiter", Rect(310,250, 430, 70)); // init super gui w

			w.view.decorator.nextLine;

			controls[\level] = EZSlider( w,         // parent
				420@20,    // bounds
				"level",  // label
				ControlSpec(0, 1, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\level, ez.value) } // action
			);
			controls[\drywet] = EZSlider( w,         // parent
				420@20,    // bounds
				"dry/wet",  // label
				ControlSpec(-1, 1, \lin, 0.01, 1.neg),     // controlSpec
				{ |ez| synth.set(\drywet, ez.value) } // action
			).valueAction_(1);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			})
		};
	}
}





CompanderGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		Server.default.waitForBoot{

			SynthDef(\comp, {|in=0, out=0, thresh=0.5, slopeBelow=1, slopeAbove=1, clampTime=0.01, relaxTime=0.01, drywet=1|
				var dry, limited, signal = In.ar(in, 2);
				dry = signal;
				signal = Compander.ar(signal, signal, thresh, slopeBelow.lag(0.1), slopeAbove, clampTime, relaxTime);
				signal = XFade2.ar(dry, signal, drywet);

				Out.ar(out, signal)
			}).add;

			Server.default.sync;

			synth = Synth.tail(Server.default, \comp);

			Server.default.sync;
			"run \comp synth".postln;



			super.gui("Compressor/Expander", Rect(310,0, 430, 155));

			w.view.decorator.nextLine;

			////////////////////////

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

			controls[\drywet] = EZSlider( w,         // parent
				420@20,    // bounds
				"dry/wet",  // label
				ControlSpec(-1, 1, \lin, 0.01, 1.neg),     // controlSpec
				{ |ez| synth.set(\drywet, ez.value) } // action
			).valueAction_(1);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}
}


AutoNotchGUI : EffectGUI {

	var synth, pitchOSCF, label, uid;

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		uid = UniqueID.next;

		Server.default.waitForBoot{
			SynthDef(\autonotch, {|in=0, out=0, rq=0.2, db= -24, xf=0.5, lag=5, uid=666|
				var freq=1000, has_freq, sig, env;
				sig = In.ar(in, 2);
				# freq, has_freq = Pitch.kr( Mix.new(sig), ampThreshold: 0.02, median: 7); // get the main resonant frequency
				SendReply.kr(Impulse.kr(10), '/pitch', [freq, has_freq], uid);
				sig = BPeakEQ.ar(sig, freq: freq.lag(lag), rq: rq.lag(lag), db: db.lag(lag));
				env = EnvGen.kr(Env.asr(0.05, 1, 0.05), 1, doneAction: 2);
				XOut.ar(out, env, sig);
			}).send;

			Server.default.sync;

			synth = Synth.tail(Server.default, \autonotch, [\uid, uid]);

			Server.default.sync;

			pitchOSCF = OSCFunc({|msg|
				synth.set(\uid, uid); // very bad code. make sure it is the right one
				if (msg[2] == uid, {
					{ label.string = msg[3].asString.split($.)[0] }.defer;
				})
			}, '/pitch', Server.default.addr);

			this.gui("AutoNotch", 300@90);

			label = StaticText(w, 80@18).string_("--").resize_(7);

			w.view.decorator.nextLine;

			controls[\rq] = EZSlider( w,         // parent
				290@20,    // bounds
				"rq",  // label
				ControlSpec(0.1, 10, \lin, 0.001, 0.1),     // controlSpec
				{ |ez| synth.set(\rq, ez.value)	}, // action
				labelWidth:17
			);
			controls[\db] = EZSlider( w,         // parent
				290@20,    // bounds
				"db",  // label
				ControlSpec(-30, 20, \lin, 0.001, -24),     // controlSpec
				{ |ez| synth.set(\db, ez.value) }, // action
				labelWidth:17
			);
			controls[\lag] = EZSlider( w,         // parent
				290@20,    // bounds
				"lag",  // label
				ControlSpec(0.1, 10, \lin, 0.001, 0.1),     // controlSpec
				{ |ez| synth.set(\lag, ez.value)	}, // action
				labelWidth:17
			);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		}
	}

	close { // extend
		pitchOSCF.free;
		super.close;
	}
}