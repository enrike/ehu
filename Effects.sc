
/*
Launcher.new
TremoloGUI.new;
NormalizerGUI.new;
LimiterGUI.new;
CompanderGUI.new;
AutoNotch.new;
*/


Launcher {
	var path, w;

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		if (exepath.isNil, {
			try { path = thisProcess.nowExecutingPath.dirname} { path=Platform.userHomeDir}
		},{
			path = exepath;
		});

		path.postln;

		this.close;
		~utils = List.new;//refs to GUI windows

		"find launched utils --> ~utils".postln;

		w = Window.new("Launcher", 120@100).alwaysOnTop=true;
		w.view.decorator = FlowLayout(w.view.bounds);
		w.view.decorator.gap=2@2;
		w.onClose = {
			this.close;
		};

		w.view.decorator.nextLine;

		ActionButton(w,"feedback",{
			~utils.add( Feedback1.new(path) );
		});

		ActionButton(w,"EQ",{
			try { ~utils.add( ChannelEQ.new) }
			{"cannot find ChannelEQ class. try installing it from http://github.com/enrike/supercollider-channeleq".postln}
		});

		ActionButton(w,"anotch",{
			~utils.add( AutoNotchGUI.new(path) );
		});

		ActionButton(w,"Dcomp",{
			~utils.add( DCompanderGUI.new(path) );
		});

		ActionButton(w,"comp",{
			~utils.add( CompanderGUI.new(path) );
		});

		ActionButton(w,"tremolo",{
			~utils.add( TremoloGUI.new(path) );
		});

		ActionButton(w,"normalizer",{
			~utils.add( NormalizerGUI.new(path) );
		});

		ActionButton(w,"limiter",{
			~utils.add( LimiterGUI.new(path) );
		});

		ActionButton(w,"player",{
			~utils.add( BufferPlayerGUI.new );
		});

		ActionButton(w,"fshift",{
			~utils.add( FreqShiftGUI.new );
		});

		ActionButton(w,"gain",{
			~utils.add( GainLimiterGUI.new );
		});

		w.front
	}

	close {
		~utils.do{|ut|
			("-"+ut).postln;
			ut.close
	};}
}


TremoloGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		midisetup = [[\tremolo, 16], [\xfade, 17]]; // control, MIDI effect channel

		synthdef = SynthDef(\trem, {|in=0, out=0, freq=0, xfade= 0|
			var sig = In.ar(in, 2);
			var dry = sig;
			sig = sig * SinOsc.ar(freq);
			sig = XFade2.ar(dry, sig, xfade);
			Out.ar(out, sig);
		});

		Server.default.waitForBoot{
			this.audio;

			super.gui("Tremolo", 430@70); // init super gui w

			w.view.decorator.nextLine;

			order.add(\tremolo);
			controls[\tremolo] = EZSlider( w,         // parent
				slbounds,    // bounds
				"freq",  // label
				ControlSpec(0.001, 50, \lin, 0.001, 0.1),     // controlSpec
				{ |ez| synth.set(\freq, ez.value) } // action
			);
			controls[\tremolo].numberView.maxDecimals = 3 ;

			order.add(\xfade);
			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			controls[\xfade].numberView.maxDecimals = 3 ;

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

		synthdef = SynthDef(\norm, {|in=0, out=0, level=0, xfade= 0|
			var sig = In.ar(in, 2) ;
			sig = Normalizer.ar(sig, level, 0.01);
			XOut.ar(out, xfade, sig);
		});

		Server.default.waitForBoot{
			this.audio;

			super.gui("Normalizer", Rect(310,320, 430, 70)); // init super gui w

			w.view.decorator.nextLine;

			controls[\level] = EZSlider( w,         // parent
				slbounds,    // bounds
				"level",  // label
				ControlSpec(0, 1, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\level, ez.value) } // action
			);
			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

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

		synthdef = SynthDef(\lim, {|in=0, out=0, level=0, xfade= 0|
			var sig = In.ar(in, 2) ;
			sig = Limiter(sig, level);
			XOut.ar(out, xfade, sig);
		});

		Server.default.waitForBoot{
			this.audio;

			super.gui("Limiter", Rect(310,250, 430, 70)); // init super gui w

			w.view.decorator.nextLine;

			controls[\level] = EZSlider( w,         // parent
				slbounds,    // bounds
				"level",  // label
				ControlSpec(0, 1, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\level, ez.value) } // action
			);
			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

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

		midisetup = [[\thresh, 18], [\slopeBelow, 19], [\slopeAbove, 20], [\clampTime, 21],
			[\relaxTime, 22], [\xfade, 23]]; // control, MIDI effect channel

		synthdef = SynthDef(\comp, {|in=0, out=0, thresh=0.5, slopeBelow=1, slopeAbove=1, clampTime=0.01,
			relaxTime=0.01, xfade= 0|
			var signal = In.ar(in, 2);

			signal = Compander.ar(signal, signal, thresh, slopeBelow.lag(0.05),
				slopeAbove.lag(0.05), clampTime.lag(0.05), relaxTime.lag(0.05));

			XOut.ar(out, xfade, signal)
		});

		Server.default.waitForBoot{
			this.audio;

			super.gui("Compressor_Expander", Rect(310,0, 430, 155));

			StaticText(w, 20@18);

			ActionButton(w,"comp",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.5);
			});
			ActionButton(w,"gate",{
				controls[\slopeBelow].valueAction_(5);
				controls[\slopeAbove].valueAction_(1);
			});
			ActionButton(w,"lim",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.1);
			});
			ActionButton(w,"sust",{
				controls[\slopeBelow].valueAction_(0.1);
				controls[\slopeAbove].valueAction_(1);
			});

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\thresh);
			controls[\thresh] = EZSlider( w,         // parent
				slbounds,    // bounds
				"thresh",  // label
				ControlSpec(0.001, 1, \lin, 0.001, 0.5),     // controlSpec
				{ |ez| synth.set(\thresh, ez.value) } // action
			);
			controls[\thresh].numberView.maxDecimals = 3 ;

			order.add(\slopeBelow);
			controls[\slopeBelow] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpBelow",  // label
				ControlSpec(-2, 5, \lin, 0.01, 1),     // controlSpec
				{ |ez| synth.set(\slopeBelow, ez.value) } // action
			);
			controls[\slopeBelow].numberView.maxDecimals = 3 ;

			order.add(\slopeAbove);
			controls[\slopeAbove] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpAbove",  // label
				ControlSpec(-2, 5, \lin, 0.01, 0.5),     // controlSpec
				{ |ez| synth.set(\slopeAbove, ez.value) } // action
			);
			controls[\slopeAbove].numberView.maxDecimals = 3 ;

			order.add(\clampTime);
			controls[\clampTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"clpTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\clampTime, ez.value) } // action
			);
			controls[\clampTime].numberView.maxDecimals = 3 ;

			order.add(\relaxTime);
			controls[\relaxTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"rlxTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\relaxTime, ez.value) } // action
			);
			controls[\relaxTime].numberView.maxDecimals = 3 ;

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}
}





AutoNotchGUI : EffectGUI {

	var synth, pitchOSCF, label, uid;
	classvar synthdef;

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	send {
		synthdef = SynthDef(\autonotch, {|in=0, out=0, rq=0.2, db= -24, lag=5, uid=666|
			var freq=1000, has_freq, sig, env;
			sig = In.ar(in, 2);
			# freq, has_freq = Pitch.kr( Mix.new(sig), ampThreshold: 0.02, median: 7); // get the main resonant frequency
			SendReply.kr(Impulse.kr(10), '/pitch', [freq, has_freq], uid);
			sig = BPeakEQ.ar(sig, freq: freq.lag(lag), rq: rq.lag(lag), db: db.lag(lag));
			env = EnvGen.kr(Env.asr(0.05, 1, 0.05), 1, doneAction: 2);
			XOut.ar(out, env, sig);
		});
		synthdef.load;
	}

	init {|exepath, preset|
		super.init(exepath);

		uid = UniqueID.next;


		Server.default.waitForBoot{
			this.send;

			Server.default.sync;

			//this.audio;

			Server.default.sync;

			synth = Synth.tail(Server.default, \autonotch, [\uid, uid]);// ovewrite

			Server.default.sync;

			pitchOSCF = OSCFunc({|msg|
				//synth.set(\uid, uid); // very bad code. make sure it is the right one
				if (msg[2] == uid, {
					{ label.string = msg[3].asString.split($.)[0] }.defer;
				})
			}, '/pitch', Server.default.addr);

			this.gui("AutoNotch", 320@90);

			label = StaticText(w, 40@18).string_("--").resize_(3);

			w.view.decorator.nextLine;

			controls[\rq] = EZSlider( w,         // parent
				slbounds,    // bounds
				"rq",  // label
				ControlSpec(0.1, 10, \lin, 0.001, 0.1),     // controlSpec
				{ |ez| synth.set(\rq, ez.value)	}, // action
				labelWidth:17
			);
			controls[\db] = EZSlider( w,         // parent
				slbounds,    // bounds
				"db",  // label
				ControlSpec(-100, 20, \lin, 0.001, -24),     // controlSpec
				{ |ez| synth.set(\db, ez.value) }, // action
				labelWidth:17
			);
			controls[\lag] = EZSlider( w,         // parent
				slbounds,    // bounds
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

	audio {
		Server.default.waitForBoot{
			synth = Synth.tail(Server.default, synth.defName, [\uid, uid]);
			Server.default.sync;
			("run"+synth.defName+"synth").postln;
		};
	}

	close { // extend
		pitchOSCF.free;
		super.close;
	}
}



FreqShiftGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		midisetup = [[\freq, 23]];

		synthdef = SynthDef(\fshift, {|in=0, out=0, freq=0, phase=0, xfade= 0| //(0..2pi)
			var signal = In.ar(in, 2);
			signal = FreqShift.ar(signal, freq, phase);
			XOut.ar(out, xfade, signal)
		});

		Server.default.waitForBoot{
			this.audio;
			super.gui("FreqShift", Rect(310,0, 430, 75));

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\freq);
			controls[\freq] = EZSlider( w,         // parent
				slbounds,    // bounds
				"freq",  // label
				ControlSpec(-100, 100, \lin, 0.001, 0),     // controlSpec
				{ |ez| synth.set(\freq, ez.value) } // action
			);
			controls[\freq].numberView.maxDecimals = 3 ;

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}

}




PitchShiftGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		midisetup = [[\freq, 23]];

		synthdef = SynthDef(\pshift, {|in=0, out=0, freq=1, xfade= 0| //(0..2pi)
			var signal = In.ar(in, 2);
			signal = PitchShift.ar(signal, pitchRatio:freq);
			XOut.ar(out, xfade, signal )
		});

		Server.default.waitForBoot{
			this.audio;
			super.gui("PitchShift", Rect(310,0, 430, 75));

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\freq);
			controls[\freq] = EZSlider( w,         // parent
				slbounds,    // bounds
				"freq",  // label
				ControlSpec(0, 5, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\freq, ez.value) } // action
			);
			controls[\freq].numberView.maxDecimals = 3 ;

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}

}



ChaosPitchShiftGUI : EffectGUI {

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		//midisetup = [[\freq, 23]];

		synthdef = SynthDef(\chaoticPitchShift, {|in=0, out=0, a=1.4, b=0.3, xfade= 0|
			var signal = In.ar(in, 2);
			signal = PitchShift.ar(signal, pitchRatio:HenonN.ar(SampleRate.ir, a, b));
			XOut.ar(out, xfade, signal )
		}).add;

		Server.default.waitForBoot{
			this.audio;
			super.gui("ChaosPitchShift", Rect(310,0, 430, 100));

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\a);
			controls[\a] = EZSlider( w,         // parent
				slbounds,    // bounds
				"a",  // label
				ControlSpec(1, 1.4, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\a, ez.value) } // action
			);
			controls[\a].numberView.maxDecimals = 3 ;

			order.add(\b);
			controls[\b] = EZSlider( w,         // parent
				slbounds,    // bounds
				"b",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.31),     // controlSpec
				{ |ez| synth.set(\b, ez.value) } // action
			);
			controls[\b].numberView.maxDecimals = 3 ;

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}

}




DCompanderGUI : EffectGUI {

	var freqs, rqs;

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		var numBands = 10; // try with more bands.
		var startFreq = 30; // freq of the first band.
		var endFreq = 15360; // freq of the last one.

		freqs = Array.geom(numBands, startFreq, ((endFreq*2)/startFreq)**(1/numBands));
		rqs = 1.0/numBands; // tweak this

		super.init(exepath);

		midisetup = [[\thresh, 18], [\slopeBelow, 19], [\slopeAbove, 20], [\clampTime, 21],
			[\relaxTime, 22], [\numBands, 23]]; // control, MIDI effect channel

		synthdef = SynthDef(\dcomp, {|in=0, out=0, thresh=0.5, slopeBelow=1, slopeAbove=1, clampTime=0.01,
			relaxTime=0.01, xfade= 0|
			var signal = In.ar(in, 2);

			signal = BBandPass.ar(
				[signal], //needs to be a 2D array: [ [ left, right ] ]
				freqs,
				rqs
			);

			signal = Compander.ar(signal, signal, thresh, slopeBelow.lag(0.05),
				slopeAbove.lag(0.05), clampTime.lag(0.05), relaxTime.lag(0.05));
			signal = Mix.fill(2, signal);

			XOut.ar(out, xfade, signal)
		});

		Server.default.waitForBoot{
			this.audio([\freqs, freqs, \rqs, rqs]);

			super.gui("D Compressor_Expander", Rect(310,0, 430, 200));

			StaticText(w, 20@18);

			ActionButton(w,"comp",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.5);
			});
			ActionButton(w,"gate",{
				controls[\slopeBelow].valueAction_(5);
				controls[\slopeAbove].valueAction_(1);
			});
			ActionButton(w,"lim",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.1);
			});
			ActionButton(w,"sust",{
				controls[\slopeBelow].valueAction_(0.1);
				controls[\slopeAbove].valueAction_(1);
			});

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\thresh);
			controls[\thresh] = EZSlider( w,         // parent
				slbounds,    // bounds
				"thresh",  // label
				ControlSpec(0.001, 1, \lin, 0.001, 0.5),     // controlSpec
				{ |ez| synth.set(\thresh, ez.value) } // action
			);
			controls[\thresh].numberView.maxDecimals = 3 ;

			order.add(\slopeBelow);
			controls[\slopeBelow] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpBelow",  // label
				ControlSpec(-2, 5, \lin, 0.01, 1),     // controlSpec
				{ |ez| synth.set(\slopeBelow, ez.value) } // action
			);
			controls[\slopeBelow].numberView.maxDecimals = 3 ;

			order.add(\slopeAbove);
			controls[\slopeAbove] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpAbove",  // label
				ControlSpec(-2, 5, \lin, 0.01, 0.5),     // controlSpec
				{ |ez| synth.set(\slopeAbove, ez.value) } // action
			);
			controls[\slopeAbove].numberView.maxDecimals = 3 ;

			order.add(\clampTime);
			controls[\clampTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"clpTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\clampTime, ez.value) } // action
			);
			controls[\clampTime].numberView.maxDecimals = 3 ;

			order.add(\relaxTime);
			controls[\relaxTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"rlxTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\relaxTime, ez.value) } // action
			);
			controls[\relaxTime].numberView.maxDecimals = 3 ;

			order.add(\numBands);
			controls[\numBands] = EZSlider( w,         // parent
				slbounds,    // bounds
				"numBands",  // label
				ControlSpec(1, 20, \lin, 1, 10),     // controlSpec
				{ |ez|
					var numBands = ez.value; // try with more bands.
					var startFreq = 30; // freq of the first band.
					var endFreq = 15360; // freq of the last one.
					freqs = Array.geom(numBands, startFreq, ((endFreq*2)/startFreq)**(1/numBands));
					rqs = controls[\rqs].value/numBands; // tweak this

					synth.free;
					this.audio;

					//synth.set(\freqs, freqs);
					//synth.set(\rqs, rqs)
				} // action
			);

			order.add(\rqs);
			controls[\rqs] = EZSlider( w,         // parent
				slbounds,    // bounds
				"rqs",  // label
				ControlSpec(0.001, 1, \lin, 0.001, 1),     // controlSpec
				{ |ez|
					var rqs = ez.value/controls[\numBands].value; // tweak this
					synth.set(\rqs, rqs)
				} // action
			);
			controls[\rqs].numberView.maxDecimals = 3 ;

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		};
	}
}


GainLimiterGUI : EffectGUI {
	var vlay, levels, inOSCFunc, outOSCFunc;

	*new {|exepath, preset=\default|
		^super.new.init(exepath, preset);
	}

	init {|exepath, preset|
		super.init(exepath);

		midisetup = [[\gain, 16], [\limiter, 17]]; // control, MIDI effect channel

		synthdef = 	SynthDef(\gain, {|in=0, out=0, gain=1, limiter=1, xfade=1|
			var signal = In.ar(in, 2) ;
			SendPeakRMS.kr(signal, 10, 3, '/gaininlvl'); // to monitor incoming feedback signal
			signal = Limiter.ar(signal, limiter) * gain;
			SendPeakRMS.kr(signal, 10, 3, '/gainoutlvl'); // to monitor incoming feedback signal
			XOut.ar(out, xfade, signal);
		});

		Server.default.waitForBoot{
			this.audio;

			super.gui("Gain_Limiter", 430@70); // init super gui w

			levels = List.new;

			inOSCFunc = OSCFunc({|msg| {
				levels[..1].do({|lvl, i| // in levels
					lvl.peakLevel = msg[3..][i*2].ampdb.linlin(-80, 0, 0, 1, \min);
					lvl.value = msg[3..][(i*2)+1].ampdb.linlin(-80, 0, 0, 1);
				});
			}.defer;
			}, '/gaininlvl', Server.default.addr);

			outOSCFunc = OSCFunc({|msg| {
				levels[2..].do({|lvl, i| // out levels
					lvl.peakLevel = msg[3..][i*2].ampdb.linlin(-80, 0, 0, 1, \min);
					lvl.value = msg[3..][(i*2)+1].ampdb.linlin(-80, 0, 0, 1);
				});
			}.defer;
			}, '/gainoutlvl', Server.default.addr);

			vlay = VLayoutView(w, 150@17); // size
			4.do{|i|
				levels.add( LevelIndicator(vlay, 4).warning_(0.9).critical_(1.0).drawsPeak_(true) ); // 5 height each
				if (i==1, {CompositeView(vlay, 1)}); // plus 2px separator
			};

			w.view.decorator.nextLine; //////////////////

			order.add(\gain);
			controls[\gain] = EZSlider( w,         // parent
				slbounds,    // bounds
				"gain",  // label
				ControlSpec(0, 20, \lin, 0.1, 1),     // controlSpec
				{ |ez| synth.set(\gain, ez.value) } // action
			);
			order.add(\limiter);
			controls[\limiter] = EZSlider( w,         // parent
				slbounds,    // bounds
				"limit",  // label
				ControlSpec(0.001, 1, \lin, 0.01, 1),     // controlSpec
				{ |ez| synth.set(\limiter, ez.value) } // action
			);
			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});
		}
	}
}