ChordGUI : EffectGUI {
	/*
	c = ChordGUI.new(master);
	c.notes;

	master is an optional instance of an object that contains a <chord variable. it will be updated each time chord changes in the ChordGUI
	*/

	var chord, main, fund=0;

	*new {|amain, path|
		^super.new.init(amain, path);
	}

	init {|amain, path|
		super.initEffectGUI(path);

		main = amain;

		chord = List.new(6);
		6.do{|i| chord.add( [0, 0, 0]) }; // octave, note, bend

		this.gui("Chord", 260@200);

		//w.view.decorator.nextLine;

		StaticText(w, Rect(0,0, 40, 15)).string="  Rand:";

		ActionButton(w,"all",{
			this.rand;
			this.updatemain;
		});
		ActionButton(w,"octave",{
			this.roctave;
			this.updatemain;
		});
		ActionButton(w,"notes",{
			this.rnotes;
			this.updatemain;
		});
		/*		ActionButton(w,"scramble",{
		this.scramble;
		this.updatemain;
		});*/
		ActionButton(w,"bend",{
			this.rbend;
			this.updatemain;
		});
		ActionButton(w,"reset bend",{
			this.resetbend;
			this.updatemain;
		});
		ActionButton(w,"0 oct",{
			this.zerooct;
			this.updatemain;
		});

		w.view.decorator.nextLine;

		controls[\fund] = EZNumber(w,        // parent
			120@20,   // bounds
			"fundamental", // label
			ControlSpec(0, 127, \lin, 1, 1),    // controlSpec
			{ |ez|
				fund = ez.value;
				this.updatemain;
			}, // action
			64,      // initValue
			true,      // initAction
			90 //labelwidth
		);

		w.view.decorator.nextLine;

		6.do{|i|
			controls[(i+1).asString++"_oct"] = EZPopUpMenu(w, 62@20,
				(i+1).asString,  [0,1,2,3,4,5,6,7,8,9],
				{|m|
					chord[i][0] = m.value;
					this.updatemain;
			}, 3, true, 20);

			controls[(i+1).asString++"_note"] = EZPopUpMenu(w, 42@20,
				//nil,  ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"],
				nil, [0,1,2,3,4,5,6,7,8,9,10,11],
				{|m|
					chord[i][1] = m.value;
					this.updatemain;
			}, 0, true, 20);

			controls[(i+1).asString++"_bend"] = EZSlider( w,         // parent
				100@20,    // bounds
				nil,  // label
				ControlSpec(-0.9, 0.9, \lin, 0.1, 0),     // controlSpec
				{ |ez|
					chord[i][2] = ez.value;
					this.updatemain;
				} // action
			);

			w.view.decorator.nextLine;
		};

		super.defaultpreset( w.name.replace(" ", "_").toLower ); // try to read and apply the default preset

		w.front
	}

	notes {
		var notes = [0,0,0,0,0,0]; // base + 1 to 6
		notes.size.do{|i|
			notes[i] = (chord[i][0]*12) + chord[i][1] + chord[i][2]
		};
		^notes;
	}

	updatemain {
		var notes = this.notes;
		//[fund, notes].postln;
		if (main.isNil.not, {
			main.base(fund);
			main.chord(notes)
		});
	}

	rand{
		this.roctave;
		this.rbend;
		this.rnotes;
	}

	rbend{
		6.do{|i|
			var sl = controls[(i+1).asString++"_bend"]; // counts from 1
			sl.valueAction = rrand(sl.controlSpec.minval, sl.controlSpec.maxval)
		}
	}

	resetbend {
		6.do{|i|
			var sl = controls[(i+1).asString++"_bend"];
			sl.valueAction = 0
		}
	}

	rnotes{
		6.do{|i|
			var sl = controls[(i+1).asString++"_note"];
			sl.valueAction = sl.items.size.rand;//rrand(sl.controlSpec.minval, sl.controlSpec.maxval)
		}
	}

	roctave{
		6.do{|i|
			var sl = controls[(i+1).asString++"_oct"];
			sl.valueAction = sl.items.size.rand;//rrand(sl.controlSpec.minval, sl.controlSpec.maxval)
		}
	}

	/*	scramble {
	var data = [chord[\0],chord[\1],chord[\2],chord[\3],chord[\4],chord[\5]].scramble; //get the [note, bend] and scramble

	6.do{|i| // restore in its new position
	chord[i.asSymbol] = data[i];
	controls[(i+1).asString++"_note"].valueAction = data[i][0];
	controls[(i+1).asString++"_bend"].valueAction = data[i][1];
	};
	this.updatemain
	}*/

	zerooct {
		6.do{|i|
			var sl = controls[(i+1).asString++"_oct"];
			sl.valueAction = 0
		}
	}
}
