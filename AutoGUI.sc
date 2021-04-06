/*
a = Auto.new;
a.sch("vol", 2.5, [0.5, 0.7]) // rand vol slider every 2.5 secs between 0.5-0.7 range
a.rand(["feedback", "deltime", "amp"])
*/


Auto {
	var <procs, <main;

	*new {|amain, preset|
		^super.new.initAuto(amain, preset)
	}

	initAuto {|amain, preset|
		main = amain;
		procs = Dictionary.new;
	}

	kill {
		procs.collect(_.stop)
	}

	sch {|control, time, range, alag=0|
		var atask, widget, anim;

		if (procs[control.asSymbol].isNil.not, { this.stop(control.asSymbol) }); // kill if already there before rebirth
		widget = main.controls[control];

		//time ? time = 1;
		//range ? range = [widget.controlSpec.minval , widget.controlSpec.maxval];

		range = range.asFloat;

		atask = Task({
			inf.do({|index|	// option to ease the jump between values using lag or varlag?
				{ widget.valueAction = rrand(range[0], range[1]) }.defer;
/*				var target = rrand(range[0], range[1]);
				var steps = time * 20; //pasos
				var steplen = (target - widget.value)/steps;
				anim.stop;
				anim.free;
				anim = nil;
				//[steps, steplen, (time/steps)].postln;
				anim=Task({
					steps.do({|index|	// option to ease the jump between values using lag or varlag?
						try{ { widget.valueAction = widget.value+steplen }.defer };
							(time/steps).wait;
					});
					anim.stop;
					anim.free;
					anim = nil;
				}, AppClock).start;*/

				time.wait;
			});
		});

		atask.start;
		procs.add(control.asSymbol -> atask);// to keep track of them
	}

	stop {|name|
		procs[name.asSymbol].stop;
		procs.removeAt(name.asSymbol);
	}
	resume {|name| procs[name.asSymbol].resume}
	pause {|name| procs[name.asSymbol].pause}
}



/*
main must be an instance that contains a dictionary called controls with instances of Sliders
*/

AutoGUI : BaseGUI {

	var auto, values;

	*new {|main, path, preset|
		^super.new.init(main, path, preset);
	}

	init {|main, path, preset|
		super.init(path);

		auto = Auto.new(main);

		values = Dictionary.new;

		this.gui("Auto"+main.class, Rect(430,0, 400, 22+(main.order.size*22)));

		w.onClose = {
			auto.kill;
		};

		StaticText(w, Rect(0,0, 40, 15)).string="  Rand:";

		SimpleButton(w,"all",{
			this.rand;
		});

		SimpleButton(w,"sliders",{
			this.randsliders;
		});

		SimpleButton(w,"times",{
			this.randtimes;
		});

		SimpleButton(w,"reset",{
			this.reset;
		});

		//auto.main.controls.keysValuesDo{ |name, control, index|
		main.order.do{|name, index| // order should be a property passed as arg on init
			var control = auto.main.controls[name];

			if (control.isKindOf(EZSlider), {
				values[name] = Dictionary.new;
				values[name][\range] = [0,1];
				values[name][\time] = 1;
				values[name][\lag] = 0;

				//slider
				controls[name] = EZRanger( w,  // parent
					290@20,    // bounds
					name,  // label
					control.controlSpec,     // controlSpec
					{ |ez|
						values[name][\range] = ez.value;
						if (auto.procs[name].isNil.not, {
							auto.sch(name, values[name][\time], ez.value, values[name][\lag])
						});
					}, initVal:[control.controlSpec.minval, control.controlSpec.maxval], initAction:true // action
				);

				// time number
				controls[name++"_time"] = EZNumber(w, 30@20, nil, ControlSpec(0.01, 120, \lin, 0.01, 1),
					{|ez|
						values[name][\time] = ez.value;
						if (auto.procs[name].isNil.not, {
							auto.sch(name, ez.value, values[name][\range], values[name][\lag])
						})
				}, 1);


				// lag number
				// controls[name++"_lag"] = EZNumber(w, 30@20, nil, ControlSpec(0, 120, \lin, 0.01, 0),
				// 	{|ez|
				// 		values[name][\lag] = ez.value;
				// 		if (auto.procs[name].isNil.not, {
				// 			auto.sch(name, values[name][\time], values[name][\range], ez.value)
				// 		})
				// });

				//button
				controls[name++"_button"] = Button(w, Rect(20, 20, 20, 20))
				.states_([
					[">", Color.white, Color.black],
					["||", Color.black, Color.red],
				])
				.action_({ arg butt;
					if (butt.value==1, {
						auto.sch(name, values[name][\time], values[name][\range], values[name][\lag]);
					},{
						auto.stop(name)
					});
				});

				SimpleButton(w,"rr",{
					var rmin = controls[name.asSymbol].controlSpec.clipLo.asFloat;
					var rmax = controls[name.asSymbol].controlSpec.clipHi.asFloat;
					var min = rrand(rmin, rmax);
					var max = min + rrand(min, rmax);
					controls[name.asSymbol].valueAction = [min, max]
				});
			})
		};

		if (preset.isNil.not, { // not loading a preset file by default
			super.preset( w.name, preset ); // try to read and apply the default preset
		});
		w.front;
	}

	rand{
		this.randsliders;
		this.randtimes
	}
	randsliders{
		controls.do{|control|
			if (control.isKindOf(EZRanger), {
				var rmin = control.controlSpec.clipLo.asFloat;
				var rmax = control.controlSpec.clipHi.asFloat;
				var min = rrand(rmin, rmax);
				var max = min + rrand(min, rmax);
				control.valueAction = [min, max]
			})
		}
	}
	randtimes{
		controls.do{|control|
			if (control.isKindOf(EZNumber), {
				control.valueAction = control.controlSpec.maxval.asFloat.rand
			})
		}
	}

	reset {
		controls.do{|control|
			if (control.isKindOf(EZNumber), {
				control.valueAction = 1
			}, {
				control.valueAction = [control.controlSpec.clipLo, control.controlSpec.clipHi]
			})
		}
	}

	close {
		w.close
	}
}