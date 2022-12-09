/*the midi function is designed to be used with a nanokorg controller
*/
Pads : BaseGUI { // NEW
	classvar padcount=0;
	var w, buttons, asls, psls, dirs, loops, synths, amp, tail, nk2values, loopsOSC, posOSC, lvlOSC;
	var out, midiout, midiport, mode;
	var id;

	*new {|path, out=0, col=5, size=120, amp=1, midif=1, loop=0, tail=0, nk2=#[0,1,2,3,4,5,6,7], randmode=0, mode=0|
		^super.new.init(path, out, col, size, amp, midif, loop, tail, nk2, nil, randmode, mode);
	}

	init {|path, aout, col, size, amp, midif, loop, atail, nk2, device="nanoKONTROL2", randmode, amode| //////////////////////
		super.init(nil); // correct?

		padcount = padcount+1;

		id = UniqueID.next + (padcount * 1000); // each pad instance gets a different slot of indexes for players trigger IDs

		["pad with ID", id, "padcout", padcount].postln;

		if (path.isNil, {
			path = Platform.userHomeDir;
		});

		//OSCdef.freeAll; // careful when using this
		//AppClock.clear;

		tail = atail;
		out = aout;
		mode = amode; // 0 on/off switch, 1 on overwrites, 2 on layers them. 3 click on / release off

		Server.default.waitForBoot{
			var buffers, files, wsize, hsize, target, displayname;
			synths.collect(_.free);
			buffers.collect(_.free);
			buffers = List.new;

			if (size.isInteger, {size=size@size});

			path.pathMatch.do{|path|
				var buf = Buffer.read(Server.default, path);
				buffers.add( buf );
			};

			if (~ehu_effects.isNil, {~ehu_effects=List.new});

			Server.default.sync;

			synths = Array.fill(buffers.size, nil);
			buttons = List.new;
			asls = List.new;
			psls = List.new;
			dirs = List.new;
			loops = List.new;
			loopsOSC = List.fill(buffers.size, {nil});
			lvlOSC = List.fill(buffers.size, {nil});
			posOSC = List.fill(buffers.size, {nil});
			//posTasks = List.fill(buffers.size, {nil});

			nk2values = List.fill(buffers.size, { Dictionary.newFrom(
				[\amp, amp, \rate, 1, \loop, loop, \dir, 1]) });

			if (col>buffers.size, {col=buffers.size});
			wsize = col * (size.x+10+10+20+30+30+25);
			//hsize = 15+ ((buffers.size/col) * size) + (((buffers.size%col)!=0).asInteger * size);
			hsize = 3 + 27 + ((buffers.size/col).asInteger * (size.y*0.75));
			if ((buffers.size%col).asBoolean, { hsize = hsize + (size.y*0.75)});

			if (randmode.asBoolean, {wsize=(size.x+10+10+20+30+30+25); hsize=(27+(size.y*0.75))}); //overwrite

			displayname = path.split[path.split.size-2];

			super.gui("Pads"+displayname, wsize@hsize);

			//w = Window("Pads", wsize@hsize).alwaysOnTop=true;
			//w.view.decorator = FlowLayout(w.view.bounds);
			//w.view.decorator.gap=2@2;
			w.onClose = {
				"Closing window".postln;
				synths.collect(_.free);
				buffers.collect(_.free);
				//posTasks.collect(_.stop);
				loopsOSC = List.fill(buffers.size, {nil});
				posOSC = List.fill(buffers.size, {nil});
				lvlOSC = List.fill(buffers.size, {nil});
				//posTasks = List.fill(buffers.size, {nil});
				OSCdef.freeAll; // is this a good idea??
				~ehu_effects.remove(this)
			};

			StaticText(w, 20@18).align_(\right).string_("Out").resize_(7);
			controls[\out] = PopUpMenu(w, Rect(10, 10, 45, 17))
			.items_( Array.fill(16, { arg i; i }) )
			.action_{|m|
				out = m.value;
				synths.collect(_.set(\out, out));
			}.value_(out); // default to sound in

			w.view.decorator.nextLine;

			// slider to control de final mix volume? limiter?

			if (randmode.asBoolean, {
				target = buffers[0]; // if randmode just need one in this case
			}, {
				target = buffers;
			});

			target.do{|buf, index|
				var sl, dirbut, loopbut, bu, levels = List.new, playhead;
				var name;

				Task{ // sync
					SynthDef(\splayerS, {|buffer, amp=1, out=0, rate=1, dir=1, loop=0,
						index=0, trigger=0, id=0|
						var signal, phasor;//, numchans = buf.numChannels; // dynamic numchan
						signal = amp * PlayBuf.ar(2, buffer, dir*rate*BufRateScale.kr(buffer),
							loop: loop, doneAction:2);
						phasor = Phasor.ar( 0, dir*rate*BufRateScale.kr(buffer),
							start: 0, end: BufFrames.ir(buffer), resetPos: 0);
						SendPeakRMS.kr(signal, 10, 3, '/lvl', index);
						SendReply.kr( LFPulse.kr(12, 0), '/pos', phasor/BufFrames.ir(buffer), index);
						SendReply.ar( Trig.ar(phasor >= ( BufFrames.ir(buffer) - 1)), '/loop', 1, index);
						Out.ar(out, signal);
					}).load;

					SynthDef(\splayerM, {|buffer, amp=1, out=0, rate=1, dir=1, loop=0,
						index=0, trigger=0, id=0|
						var signal, phasor;
						signal = amp * PlayBuf.ar(1, buffer, dir*rate*BufRateScale.kr(buffer),
							loop: loop, doneAction:2);
						phasor = Phasor.ar( 0, dir*rate*BufRateScale.kr(buffer),
							start: 0, end: BufFrames.ir(buffer), resetPos: 0);
						signal = Pan2.ar(signal); // duplicate if mono
						SendPeakRMS.kr(signal, 10, 3, '/lvl', index);
						SendReply.kr( LFPulse.kr(12, 0), '/pos', phasor/BufFrames.ir(buffer), index);
						SendReply.ar( Trig.ar(phasor >= ( BufFrames.ir(buffer) - 1)), '/loop', 1, index);
						Out.ar(out, signal);
					}).load;
					Server.default.sync; // takes time to load
				}.start; // sync task end

				name = index.asString+"\n"; //
				if (randmode.asBoolean, {name="RAND\n"}); //overwrite
				if (midif.asBoolean, {name = name++"nk:"+(nk2[index]+1)++"\n"}); // only if midi is on
				name = name + PathName(buf.path).fileName.insert(14, "\n"); //break if too long


				bu = Button(w, size.x@(size.y*0.75)).states_([
					[name, Color.white, Color.black],
					[name, Color.black, Color.red]
				])
				.action_({ arg butt;
					if ( (mode==1) || (butt.value==1), {
						var synthname;
						butt.value_(1); /// dont go black ever
						if ( buf.numChannels==2, {
							synthname = \splayerS
						},{
							synthname = \splayerM
						});
						//[buf.duration, buf.numChannels].postln;

						if (synths[index].notNil, {synths[index].free});
						//if (tail==1, {tail=\addToTail},{tail=\addToHead});

						if (randmode.asBoolean, {buf = buffers.choose});

						if (tail==1, {
							synths[index] = Synth.tail(Server.default, synthname,
								[buffer: buf,
									out:out,
									amp:nk2values[index][\amp],
									rate:nk2values[index][\rate],
									loop:nk2values[index][\loop],
									dir:nk2values[index][\dir],
									index:id+index, // fix this to get proper unique IDs across pads
									//id:id,
									trigger:1
							]);
						}, {
							synths[index] = Synth(synthname,
								[buffer: buf,
									out:out,
									amp:nk2values[index][\amp],
									rate:nk2values[index][\rate],
									loop:nk2values[index][\loop],
									dir:nk2values[index][\dir],
									index:id+index,  // fix this to get proper unique IDs across pads
									//id:id,
									trigger:1
									//target:Server.default,
									//addAction: tail // CHECK THIS WORKS OK ###################
							]);
						});

						OSCdef(\loop++index).free; // not working!!!! ++++++++++++++++
						loopsOSC[index] = OSCdef(\loop++index, {|msg, time, addr, recvPort|
							if ((id+index)==msg[2], {
								if (nk2values[index][\loop]==0, { // single shot. going OFF auto
									this.clean(index);
									{
										butt.value_(0);
										playhead.value_(0);
										levels.do{|lvl|
											lvl.value_(0);
											lvl.peakLevel_(0)
										};
									}.defer
								})
							}) ;
						}, '/loop') ;

						OSCdef(\lvl++index).free;
						lvlOSC[index] = OSCdef(\lvl++index, {|msg, time, addr, recvPort|
							//msg.postln;
							if ((id+index)==msg[2], {
								levels.do({|lvl, i| // in levels
									{
										try{ // mono sounds error
											lvl.peakLevel = msg[3..][i*2].ampdb.linlin(-80, 0, 0, 1, \min);
											lvl.value = msg[3..][(i*2)+1].ampdb.linlin(-80, 0, 0, 1)
										}
									}.defer;
								});
							})
						}, '/lvl'); //osc tag

						OSCdef(\pos++index).free;
						posOSC[index] = OSCdef(\pos++index, {|msg, time, addr, recvPort|
							if ((id+index)==msg[2], { {playhead.value = msg[3]}.defer })
						}, '/pos'); //osc tag

					}, { // OFF BUTTON
						this.clean(index);
						butt.value_(0);
						playhead.value_(0);
						levels.do{|lvl|
							lvl.value_(0);
							lvl.peakLevel_(0)
						};
						/*						this.ctlmsg(index+32, 0);
						this.ctlmsg(index+48, 0);
						this.ctlmsg(index+64, 0);*/
					})
				}).value=0;
				buttons.add(bu);

				levels.add( LevelIndicator(w, 3@(size.y*0.75)).warning_(0.9).critical_(1.0).drawsPeak_(true) );
				levels.add( LevelIndicator(w, 3@(size.y*0.75)).warning_(0.9).critical_(1.0).drawsPeak_(true) );

				playhead = Slider(w,10@(size.y*0.75))//AMP
				.orientation_(\vertical)
				.thumbSize_(10)
				.knobColor_(Color.white)
				.background_(Color.black);

				dirbut = Button(w, 15@(size.y*0.75)).states_([
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

				loopbut = Button(w, 15@(size.y*0.75)).states_([
					["o\no\no", Color.green, Color.black],
					["-\n-\n-", Color.green, Color.black]
				])
				.action_({ arg butt;
					nk2values[index][\loop] = (butt.value-1).abs;
					synths[index].set(\loop, nk2values[index][\loop])
				}).value = abs(loop-1); //display default but must reverse value
				loops.add(loopbut);

				sl = EZSlider( w, //AMP
					28@(size.y*0.75),
					nil,
					action: { |sl|
						nk2values[index][\amp] = sl.value;
						synths[index].set(\amp, nk2values[index][\amp])
				}, layout: \vert, labelHeight: 17);
				sl.value_(amp);
				asls.add(sl);

				sl = EZSlider( w,   //PITCH
					28@(size.y*0.75), nil,
					ControlSpec(0, 1.25, \lin, 0.01, 1),
					action: { |sl|
						nk2values[index][\rate] = sl.value;//.linlin(0,1, 0, 1.25);
						synths[index].set(\rate, nk2values[index][\rate])
				}, layout: \vert, labelHeight: 17);
				sl.value_(1);//1.linlin(0,1.25, 0, 1));
				sl.setColors(sliderBackground: Color.magenta);
				psls.add(sl);

				if((index>0)&&((index+1)%col.max(1)==0), {
					w.view.decorator.nextLine;
				});
			};

			if (midif==1, {this.midi(nk2, device)});

			w.front;
		}
	}

	clean {|index|
		["OFF", index].postln;
		OSCdef(\loop++index).free;
		OSCdef(\pos++index).free;
		OSCdef(\lvl++index).free;
		OSCdef(\pos++index).free;
		loopsOSC[index].free;
		loopsOSC[index] = nil;
		posOSC[index].free;
		posOSC[index] = nil;
		synths[index].free;
		synths[index] = nil;
		("pads kill"+index+"synth").postln;
	}

	play {|...args|
		args.asArray.do{|pad|
			{buttons[pad].valueAction_(1)}.defer
		}
	}

	stop {|...args|
		args.asArray.do{|pad|
			{buttons[pad].valueAction_(0)}.defer
		}
	}


	out {|chan=0|
		out=chan;
		synths.do{|i|i.set(\out, out)};
	}

	ctlmsg {|index, val=0|
		midiout.control(chan:midiport, ctlNum: index, val: val)
	}

	midi {|nk2, device|
		//MIDIdef.freeAll;
		if (MIDIClient.initialized.not, {
			MIDIClient.init;
			//MIDIClient.destinations;
			MIDIIn.connectAll;

			/*			MIDIClient.destinations.do{|dev, i|
			if (dev.device==device, {
			midiout = MIDIOut(0, MIDIClient.destinations[i].uid);
			MIDIOut.connect(1,i);
			});
			}*/
		});

		// [0,16,383, 48, 64] //vol, knob, S, M, R
		{ // nanokorg MIDI
			nk2.do{|value, i |
				//[id, i, nk2].postln;
				MIDIdef("\vol"++value++id).free;
				MIDIdef("\knob"++value++id).free;
				MIDIdef("\s"++value++id).free;
				MIDIdef("\r"++value++id).free;
				MIDIdef("\m"++value++id).free;
				// slider AMP
				MIDIdef.cc("\vol"++value++id, {arg ...args;
					nk2values[i][\amp] = args[0]/127;
					synths[i].set(\amp, nk2values[i][\amp]);
					{ asls[i].value = nk2values[i][\amp] }.defer; //just display
				}, value); // 0!!
				// knob PITCH 0-2
				MIDIdef.cc("\knob"++(16+value)++id, {arg ...args;
					nk2values[i][\rate] = args[0].linlin(0,128, 0, 1.25);
					//nk2values[i][\rate].postln;
					synths[i].set(\rate, nk2values[i][\rate]);
					{ psls[i].value = nk2values[i][\rate] }.defer; //just display
				}, (16+value)); // 16!!
				// S key LOOP?
				MIDIdef.cc("\s"++(32+value)++id, {arg ...args;
					if (args[0]>0, { // not when release
						nk2values[i][\loop] = (nk2values[i][\loop]-1).abs; //
						synths[i].set(\loop, nk2values[i][\loop]);
						//this.ctlmsg(i+32, 127);
						{ loops[i].value = (nk2values[i][\loop]-1).abs }.defer; //just display
						//{loops[i].valueAction_( (loops[i].value-1).abs )}.defer
					});
				}, (32+value)); // 32!!
				// M key reverse?
				MIDIdef.cc("\m"++(48+value)++id, {arg ...args;
					if (args[0]>0, { // not when release
						nk2values[i][\dir] = nk2values[i][\dir].neg;
						synths[i].set(\dir, nk2values[i][\dir]);
						//this.ctlmsg(i+48, 127);
						{ dirs[i].value = nk2values[i][\dir].neg }.defer; //just display as NEG in this case
						//{dirs[i].valueAction_( (dirs[i].value-1).abs )}.defer
					});
				}, (48+value)); // 48!!
				// R key PLAY/PAUSE
				MIDIdef.cc("\r"++(64+value)++id, {arg ...args;
					if (args[0]>0, {// not when release
						//this.ctlmsg(i+32, 127);
						{buttons[i].valueAction_( (buttons[i].value-1).abs )}.defer
					});
				}, (64+value)); // 64!!
			}
		}.defer(0.5);
	}
}

