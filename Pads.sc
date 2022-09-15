/*the midi function is designed to be used with a nanokorg controller
*/
Pads { // NEW
	var w, buttons, asls, psls, dirs, loops, synths, amp, tail, nk2values, loopsOSC, out;

	*new {|path, out=0, col=5, size=120, amp=1, midif=1, loop=0, tail=0, nk2=#[0,1,2,3,4,5,6,7]|
		^super.new.init(path, out, col, size, amp, midif, loop, tail, nk2);
	}

	init {|path, aout, col, size, amp, midif, loop, atail, nk2| //////////////////////
		//super.init(path);
		if (path.isNil, {
			path = Platform.userHomeDir;
		});

		OSCdef.freeAll; // careful when using this
		//AppClock.clear;

		tail = atail;
		out = aout;

		Server.default.waitForBoot{
			var buffers, files, wsize, hsize;
			synths.collect(_.free);
			buffers.collect(_.free);
			buffers = List.new;

			SynthDef(\splayerS, {|buffer, amp=1, out=0, rate=1, dir=1, loop=0, index=0|
				var pb;
				SendReply.kr(TDelay.kr(Impulse.kr(BufDur.kr(buffer)), BufDur.kr(buffer)), '/loop', 1, index);
				pb = PlayBuf.ar(2, buffer, BufRateScale.kr(buffer)*rate*dir, loop: loop);//, doneAction:2);
				Out.ar(out, pb *  amp);
			}).load;

			SynthDef(\splayerM, {|buffer, amp=1, out=0, rate=1, dir=1, loop=0|
				Out.ar(out, (PlayBuf.ar(1, buffer, BufRateScale.kr(buffer)*rate*dir, loop: loop)!2) * amp);
			}).load;

			path.pathMatch.do{|path|
				var buf = Buffer.read(Server.default, path);
				buffers.add( buf );
			};

			Server.default.sync;

			synths = Array.fill(buffers.size, nil);
			buttons = List.new;
			asls = List.new;
			psls = List.new;
			dirs = List.new;
			loops = List.new;
			loopsOSC = List.fill(buffers.size, {nil});

			nk2values = List.fill(buffers.size, { Dictionary.newFrom(
				[\amp, amp, \rate, 1, \loop, loop, \dir, 1]) });

			wsize = (col*(size+3+25+25+25+5))+8;
			hsize = (buffers.size/col)*(size+7);
			//if (buffers.size%col>0, {hsize = hsize+(size*0.8)}); // extra row

			w = Window("Pads", wsize@hsize);
			w.view.decorator = FlowLayout(w.view.bounds);
			w.view.decorator.gap=2@2;
			w.onClose = {
				synths.collect(_.free);
				buffers.collect(_.free);
			};


			/*			StaticText(w, 20@18).align_(\right).string_("Out").resize_(7);
			//controls[\out] =
			PopUpMenu(w, Rect(10, 10, 45, 17))
			.items_( Array.fill(16, { arg i; i }) )
			.action_{|m|
			//synth.set(\out, m.value);
			synths.do{|i|i.set(\out, m.value)};
			}.value_(0); // default to sound in

			w.view.decorator.nextLine;*/

			// slider to control de final mix volume? limiter?

			buffers.do{|buf, index|
				var sl, dirbut, loopbut, bu;
				var name = PathName(buf.path).fileName;

				bu = Button(w, size@(size*0.75)).states_([
					[name, Color.white, Color.black],
					[name, Color.black, Color.red]
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
									rate:nk2values[index][\rate],
									loop:nk2values[index][\loop],
									dir:nk2values[index][\dir],
									index:index
							]);
						}, {
							synths[index] = Synth(synthdef,
								[buffer: buf, out:out, amp:nk2values[index][\amp],
									rate:nk2values[index][\rate],
									loop:nk2values[index][\loop],
									dir:nk2values[index][\dir],
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

				dirbut = Button(w, 20@(size*0.75)).states_([
					[">\n>\n>", Color.green, Color.black],
					["<\n<\n<", Color.green, Color.black]
				])
				.action_({ arg butt;
					if (butt.value==0, {
						nk2values[index][\dir] = 1;
					}, {
						nk2values[index][\dir] = -1;
					});
					synths[index].set(\dir, nk2values[index][\dir])
				});
				dirs.add(dirbut);

				loopbut = Button(w, 20@(size*0.75)).states_([
					["o\no\no", Color.green, Color.black],
					["-\n-\n-", Color.green, Color.black]
				])
				.action_({ arg butt;
					nk2values[index][\loop] = (butt.value-1).abs;
					synths[index].set(\loop, nk2values[index][\loop])
				}).value = abs(loop-1); //display default but must reverse value
				loops.add(loopbut);

				sl = Slider(w,15@(size*0.75))//AMP
				.orientation_(\vertical)
				//.focusColor_(Color.red(alpha:0.2))
				.background_(Color.grey)
				.value_(amp)
				.action_({|sl|
					nk2values[index][\amp] = sl.value;
					synths[index].set(\amp, nk2values[index][\amp])
				}).value = amp;
				asls.add(sl);

				sl = Slider(w,15@(size*0.75)) //PITCH
				.orientation_(\vertical)
				.background_(Color.magenta)
				.value_(1)
				.action_({|sl|
					nk2values[index][\rate] = sl.value.linlin(0,1, 0, 1.25);
					synths[index].set(\rate, nk2values[index][\rate])
				}).value_(1.linlin(0,1.25, 0, 1));
				psls.add(sl);

				if((index>0)&&((index+1)%col.max(1)==0), {
					w.view.decorator.nextLine;
				});
			};

			if (midif==1, {this.midi(nk2)});

			w.front;
		}
	}


	out {|chan=0|
		out=chan;
		synths.do{|i|i.set(\out, out)};
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
				MIDIdef("\m"++i).free;
				// slider AMP
				MIDIdef.cc("\vol"++i, {arg ...args;
					nk2values[i][\amp] = args[0]/127;
					synths[i].set(\amp, nk2values[i][\amp]);
					{ asls[i].value = nk2values[i][\amp] }.defer; //just display
				}, i); // 0!!
				// knob PITCH 0-2
				MIDIdef.cc("\knob"++(16+i), {arg ...args;
					nk2values[i][\rate] = args[0].linlin(0,128, 0, 1.25);
					nk2values[i][\rate].postln;
					synths[i].set(\rate, nk2values[i][\rate]);
					{ psls[i].value = nk2values[i][\rate] }.defer; //just display
				}, (16+i)); // 16!!
				// S key LOOP?
				MIDIdef.cc("\s"++(32+i), {arg ...args;
					if (args[0]>0, { // not when release
						nk2values[i][\loop] = (nk2values[i][\loop]-1).abs; //
						synths[i].set(\loop, nk2values[i][\loop]);
						{ loops[i].value = (nk2values[i][\loop]-1).abs }.defer; //just display
						//{loops[i].valueAction_( (loops[i].value-1).abs )}.defer
					});
				}, (32+i)); // 32!!
				// M key reverse?
				MIDIdef.cc("\m"++(48+i), {arg ...args;
					if (args[0]>0, { // not when release
						nk2values[i][\dir] = nk2values[i][\dir].neg;
						synths[i].set(\dir, nk2values[i][\dir]);
						{ dirs[i].value = nk2values[i][\dir].neg }.defer; //just display as NEG in this case
						//{dirs[i].valueAction_( (dirs[i].value-1).abs )}.defer
					});
				}, (48+i)); // 48!!
				// R key PLAY/PAUSE
				MIDIdef.cc("\r"++(64+i), {arg ...args;
					if (args[0]>0, {// not when release
						{buttons[i].valueAction_( (buttons[i].value-1).abs )}.defer
					});
				}, (64+i)); // 64!!
			}
		}.defer(0.5);
	}
}

