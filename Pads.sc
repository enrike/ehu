/*the midi function is designed to be used with a nanokorg controller
*/
Pads { // NEW
	var w, buttons, sls, synths, amp, tail, nk2values, loopsOSC;

	*new {|path, out=0, col=5, size=120, amp=1, midif=1, loop=0, tail=0, nk2=#[0,1,2,3,4,5,6,7]|
		^super.new.init(path, out, col, size, amp, midif, loop, tail, nk2);
	}

	init {|path, out, col, size, amp, midif, loop, atail, nk2| //////////////////////
		//super.init(path);
		if (path.isNil, {
			path = Platform.userHomeDir;
		});

		OSCdef.freeAll; // careful when using this
		//AppClock.clear;

		tail = atail;

		Server.default.waitForBoot{
			var buffers, files, wsize, hsize;
			synths.collect(_.free);
			buffers.collect(_.free);
			buffers = List.new;

			SynthDef(\splayerS, {|buffer, amp=1, out=0, rate=1, loop=0, index=0|
				var pb;
				SendReply.kr(TDelay.kr(Impulse.kr(BufDur.kr(buffer)), BufDur.kr(buffer)), '/loop', 1, index);
				pb = PlayBuf.ar(2, buffer, BufRateScale.kr(buffer)*rate, loop: loop);//, doneAction:2);
				Out.ar(out, pb *  amp);
			}).load;

			SynthDef(\splayerM, {|buffer, amp=1, out=0, rate=1, loop=0|
				Out.ar(out, (PlayBuf.ar(1, buffer, BufRateScale.kr(buffer)*rate, loop: loop)!2) * amp);
			}).load;

			path.pathMatch.do{|path|
				var buf = Buffer.read(Server.default, path);
				buffers.add( buf );
			};

			Server.default.sync;

			synths = Array.fill(buffers.size, nil);
			buttons = List.new;
			sls = List.new;
			loopsOSC = List.fill(buffers.size, {nil});

			nk2values = List.fill(buffers.size, { Dictionary.newFrom([\amp, amp, \rate, 1, \loop, loop]) });

			wsize = (col*(size+2+15+2))+8;
			hsize = (buffers.size/col)*(size);
			//if (buffers.size%col>0, {hsize = hsize+(size*0.8)}); // extra row

			w = Window("Pads", wsize@hsize);
			w.view.decorator = FlowLayout(w.view.bounds);
			w.view.decorator.gap=2@2;
			w.onClose = {
				synths.collect(_.free);
				buffers.collect(_.free);
			};

			// slider to control de final mix volume? limiter?

			buffers.do{|buf, index|
				var sl = nil;
				var name = PathName(buf.path).fileName;
				var bu = Button(w, size@(size*0.75)).states_([
					[">"+name, Color.white, Color.black],
					["||"+name, Color.black, Color.red]
				])
				.action_({ arg butt;
					if (butt.value==1, {
						//synths[index] = Synth.tail(Server.default, \splayer, [buffer: buf, out:out, \amp:mode]);
						var synthdef = "";
						[buf.duration, buf.numChannels].postln;
						if (buf.numChannels==1, {
							synthdef = \splayerM;
						},{
							synthdef = \splayerS;
						});

						if (synths[index].notNil, {synths[index].free});

						if (tail==1, {
							synths[index] = Synth.tail(Server.default, synthdef,
								[buffer: buf, out:out, amp:nk2values[index][\amp],
									rate:nk2values[index][\rate], loop:nk2values[index][\loop],
									index:index
							]);
						}, {
							synths[index] = Synth(synthdef,
								[buffer: buf, out:out, amp:nk2values[index][\amp],
									rate:nk2values[index][\rate], loop:nk2values[index][\loop],
									index:index
							]);
						});

						OSCdef(\loop++index).free;
						loopsOSC[index] = OSCdef(\loop++index, {|msg, time, addr, recvPort|
							if (index==msg[2], {
								if (nk2values[index][\loop]==0, {
									"loop?".postln;
									OSCdef(\loop++index).free;
									loopsOSC[index].free;
									loopsOSC[index] = nil;
									synths[index].free;
									synths[index] = nil;
									{butt.value_(0)}.defer; // button goes back
								})
							}) ;
						}, '/loop') ;

						//nk2values[index][\loop].postln;
						/*if(nk2values[index][\loop]==0, { // OFF BUTTON. change off method!
						{butt.valueAction_(0)}.defer(buf.duration *
						(Server.default.sampleRate/buf.sampleRate) / nk2values[index][\rate]) //off button when done
						});*/
					}, {
						OSCdef(\loop++index).free;
						loopsOSC[index].free;
						loopsOSC[index] = nil;
						synths[index].free;
						synths[index] = nil;
						butt.value_(0);
						("pads kill"+index+"synth").postln;
					})
				}).value=0;

				buttons.add(bu);

				sl = Slider(w,15@(size*0.75))
				.orientation_(\vertical)
				//.focusColor_(Color.red(alpha:0.2))
				//.background_(Color.rand)
				.value_(amp)
				.action_({|sl|
					nk2values[index][\amp] = sl.value;
					synths[index].set(\amp, nk2values[index][\amp])
				});
				buttons.add(sl);

				if((index>0)&&((index+1)%col.max(1)==0), {
					w.view.decorator.nextLine;
				});
			};

			if (midif==1, {this.midi(nk2)});

			w.front;
		}
	}



	midi {|nk2|
		MIDIdef.freeAll;
		if (MIDIClient.initialized==false, {
			MIDIClient.init;
			MIDIIn.connectAll;
		});

		// [0,16,383, 48, 64] //vol, knob, S, M, R
		{ // nanokorg MIDI
			nk2.do{|i |
				MIDIdef("\vol"++i).free;
				MIDIdef("\knob"++i).free;
				MIDIdef("\s"++i).free;
				MIDIdef("\r"++i).free;
				// slider AMP
				MIDIdef.cc("\vol"++i, {arg ...args;
					nk2values[i][\amp] = args[0]/127;
					synths[i].set(\amp, nk2values[i][\amp]);
					sls.value = nk2values[i][\amp]; //just display
				}, i);
				// knob PITCH 0-2
				MIDIdef.cc("\knob"++(16+i), {arg ...args;
					nk2values[i][\rate] = args[0].linlin(0,128, 0, 1.25);
					nk2values[i][\rate].postln;
					synths[i].set(\rate, nk2values[i][\rate])
				}, (16+i));
				// R key LOOP
				MIDIdef.cc("\s"++(32+i), {arg ...args;
					if (args[0]>0, { // only when pressed
						nk2values[i][\loop] = args[0]/127; //
						synths[i].set(\loop, nk2values[i][\loop])
					});
				}, (32+i));
				// R key PLAY/PAUSE
				MIDIdef.cc("\r"++(64+i), {arg ...args;
					if (args[0]>0, {// only when pressed
						{buttons[i].valueAction_( (buttons[i].value-1).abs )}.defer
					});
				}, (64+i));
			}
		}.defer(0.5);
	}
}

