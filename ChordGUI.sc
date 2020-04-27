ChordGUI : EffectGUI {
	/*
	c = ChordGUI.new(master);
	c.notes;

	master is an optional instance of an object that contains a <chord variable. it will be updated each time chord changes in the ChordGUI
	*/

	var chord, main, fund=0;

	*new {|amain, path, chord|
		^super.new.init(amain, path, chord);
	}

	init {|amain, path, achord|
		super.initEffectGUI(path);

		main = amain;

		chord = List.new(6);
		6.do{|i| chord.add( [0, 0, 0]) }; // octave, note, bend

		if (achord.isNil.not, {chord = this.setchord(chord)});
		//if (base.isNil.not, {this.base(abase)});

		this.gui("Chord", 260@200);

		//w.view.decorator.nextLine;

		StaticText(w, Rect(0,0, 40, 15)).string="  Rand:";

		ActionButton(w,"all",{
			this.rand;
			//this.updatemain;
		});
		ActionButton(w,"octave",{
			this.roctave;
			//this.updatemain;
		});
		ActionButton(w,"notes",{
			this.rnotes;
			//this.updatemain;
		});
		/*		ActionButton(w,"scramble",{
		this.scramble;
		this.updatemain;
		});*/
		ActionButton(w,"bend",{
			this.rbend;
			//this.updatemain;
		});
		ActionButton(w,"scramble",{
			this.scramble;
		});

		ActionButton(w,"reset bend",{
			this.resetbend;
			//this.updatemain;
		});
		ActionButton(w,"0 oct",{
			this.zerooct;
			//this.updatemain;
		});

		w.view.decorator.nextLine;

/*		controls[\fund] = EZNumber(w,        // parent
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
		);*/

		w.view.decorator.nextLine;

		6.do{|i|
			controls[(i+1).asString++"_oct"] = EZPopUpMenu(w, 50@20,
				(i+1).asString,  [0,1,2,3,4,5,6,7,8,9],
				{|m|
					chord[i][0] = m.value;
					this.updatemain;
			}, 3, true, 10);

			controls[(i+1).asString++"_note"] = EZPopUpMenu(w, 40@20,
				//nil,  ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"],
				nil, [0,1,2,3,4,5,6,7,8,9,10,11],
				{|m|
					chord[i][1] = m.value;
					this.updatemain;
			}, 0, true, 20);

			controls[(i+1).asString++"_bend"] = EZSlider( w,         // parent
				155@20,    // bounds
				nil,  // label
				ControlSpec(-0.9, 0.9, \lin, 0.01, 0),     // controlSpec
				{ |ez|
					chord[i][2] = ez.value;
					this.updatemain;
				} // action
			);

			w.view.decorator.nextLine;
		};

		// if no default it should get the chord and base from main and display it in the widgets
		super.defaultpreset( w.name.replace(" ", "_").toLower ); // try to read and apply the default preset

		w.front
	}

	notes { // **** this is wrong because here notes are abs and in main are relative to a fund
		var notes = [0,0,0,0,0,0];
		notes.size.do{|i|
			notes[i] = (chord[i][0]*12) + chord[i][1] + chord[i][2] // base, note and bend
		};
		^notes;
	}

	setnotes {|achord| // six abs MIDI notes array
		chord = List.new(6);
		achord.do{|note| chord.add( this.decompose(note)) }; // octave, note, bend
	}

	decompose {|abs|
		var octave, note, blend;
		octave = (abs/12).asInteger; //octave
		note = abs.asInteger - (octave * 12); // note position within octave
		blend = abs - (abs.asInteger); // bend
		^[octave, note, blend];
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
		this.updatemain
	}

	rbend{
		6.do{|i|
			var sl = controls[(i+1).asString++"_bend"]; // counts from 1
			sl.value = rrand(sl.controlSpec.minval, sl.controlSpec.maxval);
			chord[i][2] = sl.value;
		};
		this.updatemain
	}

	resetbend {
		6.do{|i|
			var sl = controls[(i+1).asString++"_bend"];
			sl.value = 0;
			chord[i][2] = 0;
		};
		this.updatemain
	}

	rnotes{
		6.do{|i|
			var sl = controls[(i+1).asString++"_note"];
			sl.value = sl.items.size.rand;//rrand(sl.controlSpec.minval, sl.controlSpec.maxval)
			chord[i][1] = sl.value;
		};
		this.updatemain
	}

	roctave{
		6.do{|i|
			var sl = controls[(i+1).asString++"_oct"];
			sl.valueAction = sl.items.size.rand;//rrand(sl.controlSpec.minval, sl.controlSpec.maxval)
			chord[i][0] = sl.value;
		};
		this.updatemain
	}

	scramble { // octave, note, bend
		chord = chord.scramble;
		chord.do{|note, i| // restore gui to new position
			controls[(i+1).asString++"_oct"].value = note[0];
			controls[(i+1).asString++"_note"].value = note[1];
			controls[(i+1).asString++"_bend"].value = note[2];
		};
		this.updatemain
	}

	zerooct {
		6.do{|i|
			var sl = controls[(i+1).asString++"_oct"];
			sl.value = 0;
			chord[i][0] = 0;
		};
		this.updatemain
	}
}
