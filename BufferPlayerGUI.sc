/*
BufferPlayerGUI.new

by ixi-audio.net
license GPL

no loop play?
*/

BufferPlayerGUI  {
	var <controls, <w;
	var synthdef, synth;
	var plotview, drawview, plotwinrefresh, b, uid, posOSCF, pos;

	*new {
		^super.new.init;
	}

	init {
		controls = Dictionary.new;

		uid = UniqueID.next;

		synthdef = SynthDef(\play, {|out = 0, rate = 0, trig = 0, start=0, end=1, reset=0,
			loop=0, amp=1, uid=0, bufnum|
			var dur, phasor;
			dur = BufFrames.kr(bufnum);
			phasor = Phasor.ar( trig, rate * BufRateScale.kr(bufnum), start*dur, end*dur, resetPos: reset*dur);
			SendReply.kr(Impulse.kr(12), '/pos', phasor, uid);
			Out.ar(out, BufRd.ar( 2, bufnum, phasor, loop:loop ) * amp);
		});

		this.audio([\uid, uid]);

		posOSCF = OSCFunc({|msg|
			if (msg[2] == uid, { pos = msg[3].asInteger })
		}, '/pos', Server.default.addr);

		Server.default.waitForBoot{
			w = Window.new("Player", 450@220).alwaysOnTop=true;
			w.view.decorator = FlowLayout(w.view.bounds);
			w.view.decorator.gap=2@2;

			w.onClose = {
				this.close;
			};

			w.front;

			StaticText(w, 20@18).align_(\right).string_("Out").resize_(7);
			controls[\out] = PopUpMenu(w, Rect(10, 10, 45, 17))
			.items_( Array.fill(16, { arg i; i }) )
			.action_{|m|
				synth.set(\out, m.value);
			}
			.value_(0); // default to sound in

			controls[\on] = Button(w, 22@18)
			.states_([
				["on", Color.white, Color.black],
				["off", Color.black, Color.red]
			])
			.action_({ arg butt;
				synth.free;
				synth = nil;
				if (butt.value==1, {
					this.audio
				}, {
					("kill"+synthdef.name+"synth").postln;
				})
			}).value=1;

			controls[\play] = Button(w, 22@18)
			.states_([
				[">", Color.white, Color.black],
				["||", Color.black, Color.red]
			])
			.action_({ arg butt;
				if (butt.value==0, {
					synth.set(\rate, 0);
					synth.set(\amp, 0);
				},{
					if (synth.isNil, {
						synth = Synth.new(synthdef.name, [\uid, uid, \bufnum, b,
							//\loop, controls[\loop].value,
							\out, controls[\out].value,
							\rate, controls[\pitch].value,
							\amp, controls[\amp].value]);
					}, { // already there
						synth.set(\rate, controls[\pitch].value);
						synth.set(\amp, controls[\amp].value);
					});

				})
			}).valueAction=0;

			SimpleButton(w,"<<",{
				synth.set(\trig, 0); // go back to start
				{synth.set(\trig, 1)}.defer(0.05)
			});

			SimpleButton(w,"reset",{
				plotview.selectNone(0);
				synth.set(\start, 0);
				synth.set(\end, 1);
			});


			/*			controls[\loop] = Button(w, 35@18)
			.states_([
			["loop", Color.white, Color.black],
			["loop", Color.black, Color.red]
			])
			.action_({ arg butt;
			synth.set(\loop, butt.value);// this cannot be modulated once it is playing
			}).valueAction=0;*/

			SimpleButton(w,"LOAD",{
				this.choosefile;
			});

			controls[\amp] = EZSlider( w,         // parent
				130@20,    // bounds
				"amp",  // label
				ControlSpec(0, 2, \lin, 0.01, 0.6),     // controlSpec
				{ |ez| synth.set(\amp, ez.value) },
				labelWidth: 30,
				numberWidth:30
			);

			StaticText(w, 8@18);

			controls[\pitch] = EZKnob( w,        // parent
				80@20,    // bounds
				nil,
				ControlSpec(-2, 2, \lin, 0.01, 1),     // controlSpec
				{ |ez|
					if (controls[\play].value==1, { //not paused
						synth.set(\rate, ez.value)
					})
				},
				layout: \horz,
				knobSize: 20@20,
				initAction:false
			);

			w.view.decorator.nextLine;

			plotview = SoundFileView(w, Rect(0, 0, w.bounds.width-8, w.bounds.height-30))
			.elasticMode_(true)
			.timeCursorOn_(true)
			.timeCursorColor_(Color.red)
			.drawsWaveForm_(true)
			.gridOn_(true)
			.gridResolution_(10)
			.gridColor_(Color.white)
			.waveColors_([ Color.new255(103, 148, 103), Color.new255(103, 148, 103) ])
			.background_(Color.new255(155, 205, 155))
			.canFocus_(false)
			.currentSelection_(0)
			.setEditableSelectionStart(0, true)
			.setEditableSelectionSize(0, true)
			.setSelectionColor(0, Color.grey)
			.mouseDownAction_({ |view, x, y, mod, num| // update selection loop
				if (num==0, { synth.set(\start, x.linlin(0, view.bounds.width, 0,1)) });
			})
			.mouseUpAction_({ |view, x, y, mod, num, clickCount|
				if (num==0, { synth.set(\end, x.linlin(0, view.bounds.width, 0,1)) });
			});

			plotwinrefresh = Task({
				inf.do({
					plotview.timeCursorPosition = pos;
					0.1.wait;
				})
			}, AppClock);
			plotwinrefresh.start;

			"To zoom in/out: Shift + right-click + mouse-up/down".postln;
			"To scroll: right-click + mouse-left/right".postln;
		}
	}

	close {
		w.close;
		posOSCF.free;
		synth.free;
		b.free;
	}

	choosefile {
		FileDialog({ |apath|
			this.load(apath)
		},
		fileMode: 0,
		stripResult: true
		);
	}

	load {|path|
		synth.free;
		synth = nil;
		b.free;
		b = Buffer.read(Server.default, path, action: {
			if (w.isNil.not, {
				var f = { |b,v|
					b.loadToFloatArray(action: { |a| { v.setData(a, channels:b.numChannels) }.defer });
					//v.gridResolution(b.duration/10); // I would like to divide the window in 10 parts no matter what the sound dur is. Cannot change gridRes on the fly?
				};
				if (b.isNil.not, {
					f.(b, plotview)}); // only if a buf is provided
			});
		});
	}

	audio {|argarr=#[]|
		Server.default.waitForBoot{
			synthdef.load;
			Server.default.sync;
			//synth = Synth(synthdef.name, argarr);
			//Server.default.sync;
			//("run"+synth.defName+"synth").postln;
		}
	}
}