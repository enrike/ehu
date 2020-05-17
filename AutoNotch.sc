/*
AutoNotchGUI.new
*/
AutoNotchGUI : EffectGUI {

	var synth, pitchOSCF, label;

	*new {|amain, path, config|
		^super.new.init(amain, path, config);
	}

	init {|amain, path, config|
		super.initEffectGUI(path);

		Server.default.waitForBoot{
			SynthDef(\autonotch, {|in=0, out=0, rq=0.2, db= -24, xf=0.5, lag=5|
				var freq=1000, has_freq, sig, env;
				sig = In.ar(in, 2);
				# freq, has_freq = Pitch.kr( Mix.new(sig), ampThreshold: 0.02, median: 7); // get the main resonant frequency
				SendReply.kr(Impulse.kr(10), '/pitch', [freq, has_freq]);
				sig = BPeakEQ.ar(sig, freq: freq.lag(lag), rq: rq.lag(lag), db: db.lag(lag));
				env = EnvGen.kr(Env.asr(0.05, 1, 0.05), 1, doneAction: 2);
				XOut.ar(out, env, sig);
			}).send;

			Server.default.sync;

			synth = Synth.tail(Server.default, \autonotch);

			Server.default.sync;
		};

		pitchOSCF = OSCFunc({|msg| {
			//maxDecimals
			label.string = msg[3].asString.split($.)[0]
		}.defer;
		}, '/pitch', Server.default.addr);
		this.gui("AutoNotch", 300@90);

		StaticText(w, 12@18).align_(\right).string_("In").resize_(7);
		controls[\in] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\in, m.value);
		}.value = 0; // default to sound in

		StaticText(w, 23@18).align_(\right).string_("Out").resize_(7);
		controls[\out] = PopUpMenu(w, Rect(10, 10, 40, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\out, m.value);
		}.valueAction = 0; //
		w.front;

		controls[\on] = Button(w, 22@18)
		.states_([
			["on", Color.white, Color.black],
			["off", Color.black, Color.red]
		])
		.action_({ arg butt;
			synth.free;
			if (butt.value==1, {
				synth = Synth.tail(Server.default, \autonotch);
			})
		}).valueAction = 1;

		label = StaticText(w, 100@18).string_("--").resize_(7);

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


		if (config.isNil.not, { // not loading a config file by default
			super.preset( w.name.replace(" ", "_").toLower, config ); // try to read and apply the default preset
		});

		w.onClose = {synth.free; pitchOSCF.free};

		w.front;
	}
}