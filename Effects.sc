
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

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		if (exepath.isNil, { // MUST FIND ANOTHER SOLUTION. it does not work if new inside a defer
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

		SimpleButton(w,"feedback",{
			~utils.add( Feedback1.new(path) );
		});

		SimpleButton(w,"EQ",{
			try { ~utils.add( ChannelEQ.new) }
			{"cannot find ChannelEQ class. try installing it from http://github.com/enrike/supercollider-channeleq".postln}
		});

		SimpleButton(w,"anotch",{
			~utils.add( AutoNotchGUI.new(path) );
		});

		SimpleButton(w,"Dcomp",{
			~utils.add( DCompanderGUI.new(path) );
		});

		SimpleButton(w,"comp",{
			~utils.add( CompanderGUI.new(path) );
		});

		SimpleButton(w,"tremolo",{
			~utils.add( TremoloGUI.new(path) );
		});

		SimpleButton(w,"normalizer",{
			~utils.add( NormalizerGUI.new(path) );
		});

		SimpleButton(w,"limiter",{
			~utils.add( LimiterGUI.new(path) );
		});

		SimpleButton(w,"player",{
			~utils.add( BufferPlayerGUI.new(path) );
		});

		SimpleButton(w,"fshift",{
			~utils.add( FreqShiftGUI.new(path) );
		});

		SimpleButton(w,"gain",{
			~utils.add( GainLimiterGUI.new(path) );
		});

		w.front
	}

	close {
		~utils.do{|ut|
			("-"+ut).postln;
			try {ut.close}
	};}
}


TremoloGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		midisetup = [[\tremolo, 16], [\xfade, 17]]; // control, MIDI effect channel

		synthdef = SynthDef(\trem, {|in=0, out=0, freq=0, xfade= 0|
			var sig = In.ar(in, 2);
			sig = sig * SinOsc.ar(freq);
			XOut.ar(out, xfade, sig);
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
			this.pbut(\tremolo);

			order.add(\xfade);
			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			controls[\xfade].numberView.maxDecimals = 3 ;
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}
}















////////////////////////////////////////////////
NormalizerGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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
			this.pbut(\xfadelevel);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}
}




LimiterGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		synthdef = SynthDef(\lim, {|in=0, out=0, level=0, xfade= 0|
			var sig = In.ar(in, 2) ;
			sig = Limiter(sig, level.asFloat);
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
				{ |ez| synth.set(\level, ez.value.asFloat) } // action
			);
			this.pbut(\level);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}
}




PatcherGUI : EffectGUI { // just read a bus and send that signal to another bus

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		synthdef = SynthDef(\patcher, {|in=0, out=0, level=0|
			Out.ar(out, In.ar(in, 2)*level);
		});

		Server.default.waitForBoot{
			this.audio;

			super.gui("Patcher", Rect(310,250, 430, 50)); // init super gui w

			w.view.decorator.nextLine;

			controls[\level] = EZSlider( w,         // parent
				slbounds,    // bounds
				"level",  // label
				ControlSpec(0, 1, \lin, 0.001, 1),     // controlSpec
				{ |ez| synth.set(\level, ez.value.asFloat) } // action
			);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}
}






CompanderGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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

			SimpleButton(w,"comp",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.5);
			});
			SimpleButton(w,"gate",{
				controls[\slopeBelow].valueAction_(5);
				controls[\slopeAbove].valueAction_(1);
			});
			SimpleButton(w,"lim",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.1);
			});
			SimpleButton(w,"sust",{
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
			this.pbut(\thresh);

			order.add(\slopeBelow);
			controls[\slopeBelow] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpBelow",  // label
				ControlSpec(-2, 5, \lin, 0.01, 1),     // controlSpec
				{ |ez| synth.set(\slopeBelow, ez.value) } // action
			);
			controls[\slopeBelow].numberView.maxDecimals = 3 ;
			this.pbut(\slopeBelow);

			order.add(\slopeAbove);
			controls[\slopeAbove] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpAbove",  // label
				ControlSpec(-2, 5, \lin, 0.01, 0.5),     // controlSpec
				{ |ez| synth.set(\slopeAbove, ez.value) } // action
			);
			controls[\slopeAbove].numberView.maxDecimals = 3 ;
			this.pbut(\slopeAbove);

			order.add(\clampTime);
			controls[\clampTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"clpTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\clampTime, ez.value) } // action
			);
			controls[\clampTime].numberView.maxDecimals = 3 ;
			this.pbut(\clampTime);

			order.add(\relaxTime);
			controls[\relaxTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"rlxTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\relaxTime, ez.value) } // action
			);
			controls[\relaxTime].numberView.maxDecimals = 3 ;
			this.pbut(\relaxTime);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}
}





AutoNotchGUI : EffectGUI {

	var synth, pitchOSCF, label, uid;
	classvar synthdef;

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
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

	init {|exepath, preset, autopreset|
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
			this.pbut(\rq);

			controls[\db] = EZSlider( w,         // parent
				slbounds,    // bounds
				"db",  // label
				ControlSpec(-100, 20, \lin, 0.001, -24),     // controlSpec
				{ |ez| synth.set(\db, ez.value) }, // action
				labelWidth:17
			);
			this.pbut(\db);

			controls[\lag] = EZSlider( w,         // parent
				slbounds,    // bounds
				"lag",  // label
				ControlSpec(0.1, 10, \lin, 0.001, 0.1),     // controlSpec
				{ |ez| synth.set(\lag, ez.value)	}, // action
				labelWidth:17
			);
			this.pbut(\lag);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
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

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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
			this.pbut(\freq);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}

}




PitchShiftGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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
			this.pbut(\freq);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}

}



ChaosPitchShiftGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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
			this.pbut(\a);

			order.add(\b);
			controls[\b] = EZSlider( w,         // parent
				slbounds,    // bounds
				"b",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.31),     // controlSpec
				{ |ez| synth.set(\b, ez.value) } // action
			);
			controls[\b].numberView.maxDecimals = 3 ;
			this.pbut(\b);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}

}




DCompanderGUI : EffectGUI {

	var freqs, rqs;

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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

			SimpleButton(w,"comp",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.5);
			});
			SimpleButton(w,"gate",{
				controls[\slopeBelow].valueAction_(5);
				controls[\slopeAbove].valueAction_(1);
			});
			SimpleButton(w,"lim",{
				controls[\slopeBelow].valueAction_(1);
				controls[\slopeAbove].valueAction_(0.1);
			});
			SimpleButton(w,"sust",{
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
			this.pbut(\thresh);

			order.add(\slopeBelow);
			controls[\slopeBelow] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpBelow",  // label
				ControlSpec(-2, 5, \lin, 0.01, 1),     // controlSpec
				{ |ez| synth.set(\slopeBelow, ez.value) } // action
			);
			controls[\slopeBelow].numberView.maxDecimals = 3 ;
			this.pbut(\slopeBelow);

			order.add(\slopeAbove);
			controls[\slopeAbove] = EZSlider( w,         // parent
				slbounds,    // bounds
				"slpAbove",  // label
				ControlSpec(-2, 5, \lin, 0.01, 0.5),     // controlSpec
				{ |ez| synth.set(\slopeAbove, ez.value) } // action
			);
			controls[\slopeAbove].numberView.maxDecimals = 3 ;
			this.pbut(\slopeAbove);

			order.add(\clampTime);
			controls[\clampTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"clpTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\clampTime, ez.value) } // action
			);
			controls[\clampTime].numberView.maxDecimals = 3 ;
			this.pbut(\clampTime);

			order.add(\relaxTime);
			controls[\relaxTime] = EZSlider( w,         // parent
				slbounds,    // bounds
				"rlxTime",  // label
				ControlSpec(0, 0.3, \lin, 0.001, 0.01),     // controlSpec
				{ |ez| synth.set(\relaxTime, ez.value) } // action
			);
			controls[\relaxTime].numberView.maxDecimals = 3 ;
			this.pbut(\relaxTime);

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
			this.pbut(\numBands);

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
			this.pbut(\rqs);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}
}


GainLimiterGUI : EffectGUI {
	var vlay, levels, inOSCFunc, outOSCFunc;

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
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
			this.pbut(\gain);

			order.add(\limiter);
			controls[\limiter] = EZSlider( w,         // parent
				slbounds,    // bounds
				"limit",  // label
				ControlSpec(0.001, 1, \lin, 0.01, 1),     // controlSpec
				{ |ez| synth.set(\limiter, ez.value) } // action
			);
			this.pbut(\limiter);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		}
	}
}





DelayGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		//midisetup = [[\freq, 23]];

		synthdef = SynthDef(\delay, {|in=0, out=0, delt=0.25, dect=2, xfade=1|
			var signal = In.ar(in, 2);
			signal = CombL.ar(signal, maxdelaytime: 2.5, delaytime: delt, decaytime: dect);
			XOut.ar(out, xfade, signal )
		}).add;

		Server.default.waitForBoot{
			this.audio;
			super.gui("Delay", Rect(310,0, 430, 100));

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\delt);
			controls[\delt] = EZSlider( w,         // parent
				slbounds,    // bounds
				"delay",  // label
				ControlSpec(0.01, 2.5, \lin, 0.01, 0.25),     // controlSpec
				{ |ez| synth.set(\delt, ez.value) } // action
			);
			controls[\delt].numberView.maxDecimals = 3 ;
			this.pbut(\delt);

			order.add(\dect);
			controls[\dect] = EZSlider( w,         // parent
				slbounds,    // bounds
				"decay",  // label
				ControlSpec(0.01, 10, \lin, 0.01, 2),     // controlSpec
				{ |ez| synth.set(\dect, ez.value) } // action
			);
			controls[\dect].numberView.maxDecimals = 3 ;
			this.pbut(\dect);

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);
			this.pbut(\xfade);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}

}







FreeverbGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		//midisetup = [[\freq, 23]];

		synthdef = SynthDef(\freeverb, {|in=0, out=0, mix= 1, room= 0.5, damp= 0.5, xfade=1|
			var signal = In.ar(in, 2);
			signal = FreeVerb.ar(signal, mix, room, damp);
			XOut.ar(out, xfade, signal )
		}).add;

		Server.default.waitForBoot{
			this.audio;
			super.gui("Freeverb", Rect(310,0, 430, 100));

			w.view.decorator.nextLine;

			////////////////////////

			order.add(\room);
			controls[\room] = EZSlider( w,         // parent
				slbounds,    // bounds
				"room",  // label
				ControlSpec(0, 1, \lin, 0, 2),     // controlSpec
				{ |ez| synth.set(\room, ez.value) } // action
			);
			controls[\room].numberView.maxDecimals = 3 ;

			order.add(\damp);
			controls[\damp] = EZSlider( w,         // parent
				slbounds,    // bounds
				"damp",  // label
				ControlSpec(0, 1, \lin, 0, 0),     // controlSpec
				{ |ez| synth.set(\damp, ez.value) } // action
			);
			controls[\damp].numberView.maxDecimals = 3 ;

			controls[\xfade] = EZSlider( w,         // parent
				slbounds,    // bounds
				"xfade",  // label
				ControlSpec(0, 1, \lin, 0.01, 0),     // controlSpec
				{ |ez| synth.set(\xfade, ez.value) } // action
			).valueAction_(0);

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}

}



M2stGUI : EffectGUI {
	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		//midisetup = [[\freq, 23]];

		synthdef = SynthDef(\m2st, {|in=3, out=0|
			Out.ar(out, In.ar(in, 1)!2);
		}).add;

		Server.default.waitForBoot{
			this.audio;
			super.gui("M2st", Rect(310,0, 330, 30));

			if (preset.isNil.not, { // not loading a preset file by default
				super.preset( w.name, preset ); // try to read and apply the default preset
			});

			if (autopreset.isNil.not, {
				{ this.auto(autopreset) }.defer(1) // not in a hurry
			});
		};
	}

}