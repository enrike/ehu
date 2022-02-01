
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
			try { path = thisProcess.nowExecutingPath.dirname} { path=Platform.userHomeDir }
		},{
			path = exepath;
		});

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

		/*		SimpleButton(w,"Dcomp",{
		~utils.add( DCompanderGUI.new(path) );
		});*/

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
			sig = Limiter.ar(sig, level.asFloat);
			XOut.ar(out, xfade, sig);
		});

		super.gui("Limiter", Rect(310,250, 430, 70)); // init super gui w

		w.view.decorator.nextLine;

		controls[\level] = EZSlider( w,         // parent
			slbounds,    // bounds
			"level",  // label
			ControlSpec(0, 1, \lin, 0.001, 1),     // controlSpec
			{ |ez| synth.set(\level, ez.value.asFloat) } // action
		).valueAction_(0);
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
	}
}

/*
MultiCompGUI : EffectGUI {

*new {|exepath, preset=\default, autopreset|
^super.new.init(exepath, preset, autopreset);
}

init {|exepath, preset, autopreset|
var compressor;
super.init(exepath);

compressor = { |snd, attack, release, threshold, ratio|
var amplitudeDb, gainDb;
amplitudeDb = Amplitude.ar(snd, attack, release).ampdb;
gainDb = ((amplitudeDb - threshold) * (1 / ratio - 1)).min(0);
snd * gainDb.dbamp;
};

synthdef = SynthDef(\multib, { |in=0, out=0, attack=0.01, release=0.1, threshold= -6,
ratio=4, makeup=1, xfade= 0|
var amplitudeDb, gainDb;
var sig, low, mid, high;
var lowFreq = 300, highFreq = 3200;
sig = In.ar(in, 2);
low = LPF.ar(LPF.ar(sig, lowFreq), lowFreq);
sig = sig - low;
mid = LPF.ar(LPF.ar(sig, highFreq), highFreq);
high = sig - mid;

//low = compressor.(low, attack, release, threshold, ratio);
amplitudeDb = Amplitude.ar(low, attack, release).ampdb;
gainDb = ((amplitudeDb - threshold) * (1 / ratio - 1)).min(0);
low = low * gainDb.dbamp * makeup;

//mid = compressor.(mid, attack, release, threshold, ratio);
amplitudeDb = Amplitude.ar(mid, attack, release).ampdb;
gainDb = ((amplitudeDb - threshold) * (1 / ratio - 1)).min(0);
mid = mid * gainDb.dbamp * makeup;

//high = compressor.(high, attack, release, threshold, ratio);
amplitudeDb = Amplitude.ar(high, attack, release).ampdb;
gainDb = ((amplitudeDb - threshold) * (1 / ratio - 1)).min(0);
high = high * gainDb.dbamp * makeup;

sig = low + mid + high;
XOut.ar(out, xfade, sig);
});

/*		synthdef = SynthDef(\multib, { |in=0, out=0, attack=0.01, release=0.1, threshold= -6,
ratio=4, makeup=1, xfade= 0|
var amplitudeDb, gainDb;
var freqs = [120, 600, 2000, 6000];
var clean, signal, compressed, band, bands=Array.fill(freqs.size+1, {0});
clean = In.ar(in, 2);
signal = clean;
compressed = clean;
freqs.do{|freq, i|
band = LPF.ar(LPF.ar(signal, freq), freq);
bands[i] = band;
signal = signal - band;
};
bands[bands.size-1] = signal - band; // and the last one

bands.do{|ba, i|
amplitudeDb = Amplitude.ar(ba, attack, release).ampdb;
gainDb = ((amplitudeDb - threshold) * (1 / ratio - 1)).min(0);
compressed = compressed + (ba * gainDb.dbamp * makeup);
};

compressed = compressed.sum;
XOut.ar(out, xfade, compressed);
});*/

super.gui("Multiband Compressor", Rect(310,0, 430, 155));

order.add(\attack);
controls[\attack] = EZSlider( w,         // parent
slbounds,    // bounds
"attack",  // label
ControlSpec(0.001, 1, \lin, 0.001, 0.01),     // controlSpec
{ |ez| synth.set(\attack, ez.value) } // action
);
controls[\attack].numberView.maxDecimals = 3 ;
this.pbut(\attack);

order.add(\release);
controls[\release] = EZSlider( w,         // parent
slbounds,    // bounds
"release",  // label
ControlSpec(0.001, 1, \lin, 0.001, 0.1),     // controlSpec
{ |ez| synth.set(\release, ez.value) } // action
);
controls[\release].numberView.maxDecimals = 3 ;
this.pbut(\release);

order.add(\thresh);
controls[\thresh] = EZSlider( w,         // parent
slbounds,    // bounds
"thresh",  // label
ControlSpec(-10, 1, \lin, 0.001, -6),     // controlSpec
{ |ez| synth.set(\thresh, ez.value) } // action
);
//controls[\thresh].numberView.maxDecimals = 3 ;
this.pbut(\thresh);

order.add(\ratio);
controls[\ratio] = EZSlider( w,         // parent
slbounds,    // bounds
"ratio",  // label
ControlSpec(0.1, 6, \lin, 0.001, 4),     // controlSpec
{ |ez| synth.set(\ratio, ez.value) } // action
);
controls[\ratio].numberView.maxDecimals = 3 ;
this.pbut(\ratio);

order.add(\makeup);
controls[\makeup] = EZSlider( w,         // parent
slbounds,    // bounds
\makeup,  // label
ControlSpec(1, 3, \lin, 0.001, 1),     // controlSpec
{ |ez| synth.set(\makeup, ez.value) } // action
);
//controls[\makeup].numberView.maxDecimals = 3 ;
this.pbut(\makeup);

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
}
}*/



CompressorGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		synthdef = SynthDef(\comp, { |in=0, out=0, attack=0.01, release=0.1, thresh= -6,
			ratio=4, makeup=1, xfade= 0|
			var amplitudeDb, gainDb, sig;
			sig = In.ar(in, 2);
			amplitudeDb = Amplitude.ar(sig, attack, release).ampdb;
			gainDb = ((amplitudeDb - thresh) * (1 / ratio - 1)).min(0);
			sig = sig * gainDb.dbamp * (makeup+1);
			XOut.ar(out, xfade, sig);
		});

		super.gui("Compressor", Rect(310,0, 430, 155));

		order.add(\attack);
		controls[\attack] = EZSlider( w,         // parent
			slbounds,    // bounds
			"attack",  // label
			ControlSpec(0.001, 1, \lin, 0.001, 0.01),     // controlSpec
			{ |ez| synth.set(\attack, ez.value) } // action
		);
		controls[\attack].numberView.maxDecimals = 3 ;
		this.pbut(\attack);

		order.add(\release);
		controls[\release] = EZSlider( w,         // parent
			slbounds,    // bounds
			"release",  // label
			ControlSpec(0.001, 1, \lin, 0.001, 0.1),     // controlSpec
			{ |ez| synth.set(\release, ez.value) } // action
		);
		controls[\release].numberView.maxDecimals = 3 ;
		this.pbut(\release);

		order.add(\thresh);
		controls[\thresh] = EZSlider( w,         // parent
			slbounds,    // bounds
			"thresh",  // label
			ControlSpec(-10, 1, \lin, 0.001, -6),     // controlSpec
			{ |ez| synth.set(\thresh, ez.value) } // action
		);
		//controls[\thresh].numberView.maxDecimals = 3 ;
		this.pbut(\thresh);

		order.add(\ratio);
		controls[\ratio] = EZSlider( w,         // parent
			slbounds,    // bounds
			"ratio",  // label
			ControlSpec(0.1, 10, \lin, 0.001, 4),     // controlSpec
			{ |ez| synth.set(\ratio, ez.value) } // action
		);
		controls[\ratio].numberView.maxDecimals = 3 ;
		this.pbut(\ratio);

		order.add(\makeup);
		controls[\makeup] = EZSlider( w,         // parent
			slbounds,    // bounds
			\makeup,  // label
			ControlSpec(0, 5, \lin, 0.001, 0),     // controlSpec
			{ |ez| synth.set(\makeup, ez.value) } // action
		);
		//controls[\makeup].numberView.maxDecimals = 3 ;
		this.pbut(\makeup);

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

		synthdef = SynthDef(\compexp, {|in=0, out=0, thresh=0.5, slopeBelow=1, slopeAbove=1, clampTime=0.01,
			relaxTime=0.01, xfade= 0|
			var sig = In.ar(in, 2);

			sig = Compander.ar(sig, sig, thresh, slopeBelow.lag(0.05),
				slopeAbove.lag(0.05), clampTime.lag(0.05), relaxTime.lag(0.05));
			XOut.ar(out, xfade, sig);
		});

		super.gui("Compressor_Expander", Rect(310,0, 430, 155));

		//StaticText(w, 20@18);

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
	}
}



AutoMultiNotchGUI : EffectGUI {

	var synth, pitchOSCF, label, uid;
	classvar synthdef;

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	send {
		synthdef = SynthDef(\automultinotch, {|in=0, out=0, nodes=4, rq=0.2, db= -24, lag=5, uid=666|
			var freq=1000, has_freq, sig, env;
			sig = In.ar(in, 2);

			nodes.do{|i|
				# freq, has_freq = Pitch.kr( Mix.new(sig), ampThreshold: 0.02, median: 7); // get the main resonant frequency
				sig = BPeakEQ.ar(sig, freq: freq.lag(lag), rq: rq.lag(lag), db: db.lag(lag));
			};

			env = EnvGen.kr(Env.asr(0.05, 1, 0.05), 1, doneAction: 2);
			XOut.ar(out, env, sig);
		});
		synthdef.load;
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		uid = UniqueID.next;

		{
			this.send;
			synth = Synth.tail(Server.default, \automultinotch, [\uid, uid]);// ovewrite
		}.defer(1);

		this.gui("AutoMultiNotch", 360@115);

		//label = StaticText(w, 40@18).string_("--").resize_(3);

		w.view.decorator.nextLine;

		controls[\nodes] = EZSlider( w,         // parent
			slbounds,    // bounds
			"n",  // label
			ControlSpec(1, 8, \lin, 1, 4),     // controlSpec
			{ |ez| synth.set(\nodes, ez.value)	}, // action
			labelWidth:17
		);
		this.pbut(\nodes);

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

	audio {
		Server.default.waitForBoot{
			synth = Synth.tail(Server.default, synthdef.name, [\uid, uid]);
			Server.default.sync;
			("run"+synth.defName+"synth").postln;
		};
	}

	close { // extend
		pitchOSCF.free;
		super.close;
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

		{
			this.send;
			synth = Synth.tail(Server.default, \autonotch, [\uid, uid]);// ovewrite
		}.defer(1);

		pitchOSCF = OSCFunc({|msg|
			//synth.set(\uid, uid); // very bad code. make sure it is the right one
			if (msg[2] == uid, {
				{ label.string = msg[3].asString.split($.)[0] }.defer;
			})
		}, '/pitch', Server.default.addr);

		this.gui("AutoNotch", 360@90);

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

	audio {
		Server.default.waitForBoot{
			synth = Synth.tail(Server.default, synthdef.name, [\uid, uid]);
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

			XOut.ar(out, xfade, signal);
		});

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
			XOut.ar(out, xfade, signal);
		});

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
			XOut.ar(out, xfade, signal);
		}).add;

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
	}

}



/*
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
).add;

signal = Compander.ar(signal, signal, thresh, slopeBelow.lag(0.05),
slopeAbove.lag(0.05), clampTime.lag(0.05), relaxTime.lag(0.05));
signal = Mix.fill(2, signal);

XOut.ar(out, xfade, signal);
});

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
ControlSpec(-2, 5, \lin, 0.001, 1),     // controlSpec
{ |ez| synth.set(\slopeBelow, ez.value) } // action
);
controls[\slopeBelow].numberView.maxDecimals = 3 ;
this.pbut(\slopeBelow);

order.add(\slopeAbove);
controls[\slopeAbove] = EZSlider( w,         // parent
slbounds,    // bounds
"slpAbove",  // label
ControlSpec(-2, 5, \lin, 0.001, 0.5),     // controlSpec
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
}
}*/


GainLimiterGUI : EffectGUI {
	var vlay, levels, inOSCFunc, outOSCFunc;

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		midisetup = [[\gain, 16], [\limiter, 17]]; // control, MIDI effect channel

		synthdef = 	SynthDef(\gain, {|in=0, out=0, gain=1, limit=1, xfade=1|
			var signal = In.ar(in, 2) * gain;
			SendPeakRMS.kr(signal, 10, 3, '/gaininlvl'); // to monitor incoming feedback signal
			signal = Limiter.ar(signal, limit);
			SendPeakRMS.kr(signal, 10, 3, '/gainoutlvl'); // to monitor incoming feedback signal
			XOut.ar(out, xfade, signal);
		}).add;

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

		vlay = VLayoutView(w, 120@17); // size
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

		order.add(\limit);
		controls[\limit] = EZSlider( w,         // parent
			slbounds,    // bounds
			"limit",  // label
			ControlSpec(0.001, 1, \lin, 0.01, 1),     // controlSpec
			{ |ez| synth.set(\limit, ez.value) } // action
		);
		this.pbut(\limit);

		if (preset.isNil.not, { // not loading a preset file by default
			super.preset( w.name, preset ); // try to read and apply the default preset
		});

		if (autopreset.isNil.not, {
			{ this.auto(autopreset) }.defer(1) // not in a hurry
		});
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

		super.gui("Delay", Rect(310,0, 430, 100));

		w.view.decorator.nextLine;

		////////////////////////

		order.add(\delt);
		controls[\delt] = EZSlider( w,         // parent
			slbounds,    // bounds
			"delay",  // label
			ControlSpec(0.01, 1.5, \lin, 0.01, 0.25),     // controlSpec
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
			//signal = FreeVerb.ar(signal, mix, room, damp)
			signal = FreeVerb2.ar(signal[0], signal[1], mix, room, damp);
			XOut.ar(out, xfade, signal)
		}).add;

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
		this.pbut(\room);

		order.add(\damp);
		controls[\damp] = EZSlider( w,         // parent
			slbounds,    // bounds
			"damp",  // label
			ControlSpec(0, 1, \lin, 0, 0),     // controlSpec
			{ |ez| synth.set(\damp, ez.value) } // action
		);
		controls[\damp].numberView.maxDecimals = 3 ;
		this.pbut(\damp);

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
	}
}

GVerbGUI : EffectGUI {

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);

		//midisetup = [[\freq, 23]];

		synthdef = SynthDef(\gverb, {|in=0, out=0, roomsize=10, revtime=3, damping=0.5, inputbw=0.5, spread=15, earlyreflevel=0.7, taillevel=0.5, maxroomsize=300 xfade=1|
			var signal = In.ar(in, 1);//(In.ar(in, 2).sum)/2;
			signal = GVerb.ar(signal, roomsize, revtime, damping, inputbw, spread, 1, earlyreflevel, taillevel, maxroomsize);
			XOut.ar(out, xfade, signal)
		}).add;

		super.gui("GVerb", Rect(310,0, 430, 250));

		w.view.decorator.nextLine;

		////////////////////////

		order.add(\roomsize);
		controls[\roomsize] = EZSlider( w,         // parent
			slbounds,    // bounds
			"roomsize",  // label
			ControlSpec(0, 100, \lin, 0, 10),     // controlSpec
			{ |ez| synth.set(\roomsize, ez.value) } // action
		);
		//controls[\room].numberView.maxDecimals = 3 ;
		this.pbut(\roomsize);

		order.add(\revtime);
		controls[\revtime] = EZSlider( w,         // parent
			slbounds,    // bounds
			"revtime",  // label
			ControlSpec(0, 3, \lin, 0, 2),     // controlSpec
			{ |ez| synth.set(\revtime, ez.value) } // action
		);
		controls[\revtime].numberView.maxDecimals = 3 ;
		this.pbut(\revtime);

		order.add(\damping);
		controls[\damping] = EZSlider( w,         // parent
			slbounds,    // bounds
			"damping",  // label
			ControlSpec(0, 1, \lin, 0, 0),     // controlSpec
			{ |ez| synth.set(\damping, ez.value) } // action
		);
		controls[\damping].numberView.maxDecimals = 3 ;
		this.pbut(\damping);

		order.add(\inputbw);
		controls[\inputbw] = EZSlider( w,         // parent
			slbounds,    // bounds
			"inputbw",  // label
			ControlSpec(0, 1, \lin, 0, 0.5),     // controlSpec
			{ |ez| synth.set(\inputbw, ez.value) } // action
		);
		controls[\inputbw].numberView.maxDecimals = 3 ;
		this.pbut(\inputbw);

		order.add(\spread);
		controls[\spread] = EZSlider( w,         // parent
			slbounds,    // bounds
			"spread",  // label
			ControlSpec(0, 30, \lin, 0, 15),     // controlSpec
			{ |ez| synth.set(\spread, ez.value) } // action
		);
		//controls[\spread].numberView.maxDecimals = 3 ;
		this.pbut(\spread);

		order.add(\earlyreflevel);
		controls[\earlyreflevel] = EZSlider( w,         // parent
			slbounds,    // bounds
			"earlyreflevel",  // label
			ControlSpec(0, 1, \lin, 0, 0.7),     // controlSpec
			{ |ez| synth.set(\earlyreflevel, ez.value) } // action
		);
		controls[\earlyreflevel].numberView.maxDecimals = 3 ;
		this.pbut(\earlyreflevel);

		//earlyreflevel=0.7, taillevel=0.5, maxroomsize=300
		order.add(\taillevel);
		controls[\taillevel] = EZSlider( w,         // parent
			slbounds,    // bounds
			"taillevel",  // label
			ControlSpec(0, 1, \lin, 0, 0.5),     // controlSpec
			{ |ez| synth.set(\taillevel, ez.value) } // action
		);
		controls[\taillevel].numberView.maxDecimals = 3 ;
		this.pbut(\taillevel);

		order.add(\maxroomsize);
		controls[\maxroomsize] = EZSlider( w,         // parent
			slbounds,    // bounds
			"maxroomsize",  // label
			ControlSpec(0, 600, \lin, 0, 300),     // controlSpec
			{ |ez| synth.set(\maxroomsize, ez.value) } // action
		);
		//controls[\maxroomsize].numberView.maxDecimals = 3 ;
		this.pbut(\maxroomsize);

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
	}

}


M2stGUI : EffectGUI {
	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);
		//midisetup = [[\freq, 23]];

		synthdef = SynthDef(\m2st, {|in=3, out=5|
			Out.ar(out, In.ar(in, 1)!2);
		}).add;

		super.gui("M2st", Rect(310,0, 330, 30));

		if (preset.isNil.not, { // not loading a preset file by default
			super.preset( w.name, preset ); // try to read and apply the default preset
		});

		if (autopreset.isNil.not, {
			{ this.auto(autopreset) }.defer(1) // not in a hurry
		});
	}
}



Mirror : EffectGUI {
	var num = 5;
	var in=0;
	var out=0;
	var maxlen = 5; //secs
	var rate = -1;
	var thr = 0.05;
	var amp=1;
	//var del=0;
	var flash;
	var recbufs;
	var players;
	var delay, delaybus=30;
	var listener, recorder, index=0, sttime=0;

	*new {|exepath, preset=\default, autopreset|
		^super.new.init(exepath, preset, autopreset);
	}

	init {|exepath, preset, autopreset|
		super.init(exepath);
		//midisetup = [[\freq, 23]];

		recbufs = List.newClear(num);
		players = List.newClear(num);

		listener.free;
		recorder.free;

		listener = nil;
		recorder = nil;

		index=0;
		sttime=0;

		super.gui("Mirror", Rect(310,0, 330, 110));

		flash = Button(w, 18@18).states_([
			["", Color.black, Color.grey],
			["", Color.black, Color.red],
		]);

		controls[\thres] = EZSlider( w,         // parent
			slbounds,    // bounds
			"thres",  // label
			ControlSpec(0, 1, \lin, 0.01, 0.05),     // controlSpec
			{ |ez| thr = ez.value; listener.set(\silen_thres, ez.value) } // action
		);
		this.pbut(\thres);

		controls[\del] = EZSlider( w,         // parent
			slbounds,    // bounds
			"del",  // label
			ControlSpec(0, 0.5, \lin, 0.01, 0.07),     // controlSpec
			{ |ez|
				delay.set(\delay, ez.value)
			} // action
		);
		this.pbut(\del);

		controls[\rate] = EZSlider( w,         // parent
			slbounds,    // bounds
			"rate",  // label
			ControlSpec(-1, 1, \lin, 0.01, -1),     // controlSpec
			{ |ez| rate = ez.value } // action
		);
		this.pbut(\ratel);

		controls[\amp] = EZSlider( w,         // parent
			slbounds,    // bounds
			"amp",  // label
			ControlSpec(0, 1, \lin, 0.01, amp),     // controlSpec
			{ |ez| amp = ez.value } // action
		);
		this.pbut(\amp);

		// overwrite
		//controls[\in].postln;
		controls[\in].action = {|m|
			in = m.value;
			try{listener.set(\in, in)};
			try{delay.set(\in, in)}
		};
		//controls[\out].postln;
		controls[\out].action = {|m|
			out = m.value;
			try{listener.set(\out, in)}
		};

		OSCdef(\mirrorsilenceOSCdef).clear;
		OSCdef(\mirrorsilenceOSCdef).free;

		if (preset.isNil.not, { // not loading a preset file by default
			super.preset( w.name, preset ); // try to read and apply the default preset
		});

		if (autopreset.isNil.not, {
			{ this.auto(autopreset) }.defer(1) // not in a hurry
		});
	}

	close {
		("freeing").postln;
		listener.free;
		recorder.free;

		recbufs.collect(_.free);
		players.collect(_.free);
		super.close;

		utils.do{|ut|
			ut.close
		};
		~ehu_effects.remove(this)
	}

	audio {
		Server.default.waitForBoot{

			listener.free;
			recorder.free;
			delay.free;

			recbufs.collect(_.free);
			players.collect(_.free);

			recbufs = List.newClear(num);
			players = List.newClear(num);

			SynthDef(\tempdelay, {|in=0, out=0, delay=0.07|
				Out.ar(out, DelayC.ar(In.ar(in, 2), delaytime:delay))
			}).load;

			SynthDef(\ehurecorder,{ arg in=0, bufnum=0, del=0.01;
				var signal = In.ar(in, 2);
				RecordBuf.ar(signal, bufnum, doneAction: Done.freeSelf, loop:0);
			}).add;

			SynthDef(\ehuplayermirror, {|bufnum, rate=1, st=0, out=0, amp=1|
				var len = BufFrames.kr(bufnum);
				Out.ar(out, PlayBuf.ar(2, bufnum, rate, startPos:st, loop:0, doneAction: Done.freeSelf) * amp)
			}).add;

			SynthDef(\sil_listener, { |in=0, gain=1, silen_thres=0.45, falltime=0.03, checkrate=60 |
				var signal, detected;
				signal = In.ar(in, 1)*gain;
				detected = DetectSilence.ar( signal, amp:silen_thres, time:falltime );
				SendReply.kr(Impulse.kr(checkrate), '/mirrorsilence', detected); // report
			}).add;

			recbufs.size.do{|i|
				recbufs[i] = Buffer.alloc(Server.default, Server.default.sampleRate * maxlen, 2 ); // st buffer
			};

			Server.default.sync;
			///////////////////////

			delay = Synth(\tempdelay, [\in, in, \out, delaybus, \amp, amp]);

			listener = Synth.tail(Server.default, \sil_listener, [
				\in, in,
				\silen_thres, thr,// usually you just need to tweak this parameter
				\falltime, 0.25,
				\checktime, 60,
			]);

			// silence
			OSCdef(\mirrorsilenceOSCdef).clear;
			OSCdef(\mirrorsilenceOSCdef).free;
			OSCdef(\mirrorsilenceOSCdef, {|msg, time, addr, recvPort|
				if (msg[3]==0, {
					if (recorder.isNil, {
						sttime = SystemClock.seconds;
						{flash.value=1}.defer;
						recbufs[index].zero;
						recorder = Synth.tail(Server.default, \ehurecorder,
							[\in, delaybus, \bufnum, recbufs[index].bufnum, \loop, 0]);
					})
				},{
					if (recorder.isNil.not, {
						var len = ((SystemClock.seconds - sttime) * recbufs[index].sampleRate).asInteger;
						if (rate>0, {len=0}); // start from start point
						players[index] = Synth(\ehuplayermirror, [\out, out, \bufnum, recbufs[index].bufnum,
							\rate, -1, \st, len, \rate, rate, \amp, amp]);
						{flash.value=0}.defer;
						recorder.free;
						recorder = nil;
						index = index + 1;
						if (index >= recbufs.size, {index=0})
					})
				});
			}, '/mirrorsilence', Server.default.addr);
		}
	}
}



Shooter : EffectGUI {
	var in=0;
	var out=0;
	var thr = 0.6;
	var num=5;
	var amp=0.9;

	var flash;
	var players;
	var listener;
	var filepath;
	var files;
	var buffer;
	var index = 0;

	*new {|exepath, preset=\default, autopreset, filepath=""|
		^super.new.init(exepath, preset, autopreset, filepath);
	}

	init {|exepath, preset, autopreset, afilepath|
		super.init(exepath);

		filepath = afilepath;
		players = List.newClear(num);

		super.gui("Shooter", Rect(310,0, 330, 90));

		flash = Button(w, 18@18).states_([
			["", Color.black, Color.grey],
			["", Color.black, Color.red],
		]);

		controls[\thres] = EZSlider( w,         // parent
			slbounds,    // bounds
			"thres",  // label
			ControlSpec(0, 1, \lin, 0.01, 0.05),     // controlSpec
			{ |ez| thr = ez.value; listener.set(\silen_thres, ez.value) } // action
		);
		this.pbut(\thres);

		controls[\amp] = EZSlider( w,         // parent
			slbounds,    // bounds
			"amp",  // label
			ControlSpec(0, 1, \lin, 0.01, amp),     // controlSpec
			{ |ez| amp = ez.value } // action
		);
		this.pbut(\amp);


		if (PathName.new(filepath).isFolder, { // if a folder apply wildcards
			files = PathName.new(filepath).files.collect(_.fileName);
		});


		StaticText(w, 60@18).align_(\right).string_("shoot").resize_(7);
		controls[\file] = PopUpMenu(w, Rect(0, 10, 260, 18))
		.items_( files.asArray )
		.action_{|m|
			buffer.free;
			buffer = Buffer.read(Server.default, filepath++Platform.pathSeparator++files[m.value]);
		}.value_(0); // default to sound in

		OSCdef(\onsetOSCdef).clear;
		OSCdef(\onsetOSCdef).free;

		if (preset.isNil.not, { // not loading a preset file by default
			super.preset( w.name, preset ); // try to read and apply the default preset
		});

		if (autopreset.isNil.not, {
			{ this.auto(autopreset) }.defer(1) // not in a hurry
		});
	}

	close {
		("freeing").postln;
		listener.free;

		players.collect(_.free);
		super.close;

		utils.do{|ut|
			ut.close
		};
		~ehu_effects.remove(this)
	}

	audio {

		Server.default.waitForBoot{
			listener.free;

			players.collect(_.free);

			players = List.newClear(num);

			SynthDef(\onset_listener, { |in=0, gain=1, thres=0.6 |
				var signal, onset;
				signal = SoundIn.ar(in)*gain;
				signal = FFT(LocalBuf(2048), signal, wintype:1);		// threshold
				onset = Onsets.kr(signal, thres, \rcomplex, relaxtime: 2.1, floor: 0.1, mingap: 1,
					medianspan:11, whtype:1, rawodf:0);
				SendReply.kr(onset, '/onsetshooter', 1);
			}).add;

			SynthDef(\ehuplayerbasic, {|bufnum, amp=1, rate=1, st=0, out=0|
				//var len = BufFrames.kr(bufnum);
				Out.ar(out, PlayBuf.ar(2, bufnum, rate, startPos:st, loop:0, doneAction: Done.freeSelf) * amp )
			}).add;

			Server.default.sync; //////////////////////////

			listener = Synth.tail(Server.default, \onset_listener, [\in, in, \threshold, thr]);

			OSCdef(\onsetOSCdef).clear;
			OSCdef(\onsetOSCdef).free;
			OSCdef(\onsetOSCdef, {|msg, time, addr, recvPort|
				{flash.value=1}.defer;
				{flash.value=0}.defer(0.1);
				//players.postln;

				if (buffer.notNil, {
					players[index].free;
					players.put(index, Synth(\ehuplayerbasic, [\out, out, \bufnum, buffer.bufnum, \rate, 1, \amp, amp]));
					index = index + 1;
					if (index >= players.size, {index=0})
				});
			}, '/onsetshooter', Server.default.addr);
		}
	}
}