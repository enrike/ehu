ChordGUI : BaseGUI {
	/*
	c = ChordGUI.new(master);
	c.notes;

	master is an optional instance of an object that contains a <chord variable. it will be updated each time chord changes in the ChordGUI
	*/

	var chord, main;

	*new {|amain, path, chord, preset|
		^super.new.init(amain, path, chord, preset);
	}

	init {|amain, path, achord, preset|
		super.init(path);

		main = amain;

		chord = List.new(6);
		6.do{|i| chord.add( [0, 0, 0]) }; // octave, note, bend

		this.gui("Chord", 260@205);

		StaticText(w, Rect(0,0, 40, 15)).string="  Rand:";

		ActionButton(w,"all",{
			this.rand;
		});
		ActionButton(w,"octave",{
			this.roctave;
		});
		ActionButton(w,"notes",{
			this.rnotes;
		});
		ActionButton(w,"bend",{
			this.rbend;
		});
		ActionButton(w,"scramble",{
			this.scramble;
		});

		ActionButton(w,"reset bend",{
			this.resetbend;
		});

		w.view.decorator.nextLine;

		6.do{|i|
			//order.add((i+1).asString++"_oct");
			controls[(i+1).asString++"_oct"] = EZPopUpMenu(w, 50@20,
				(i+1).asString,  [0,1,2,3,4,5,6,7,8,9],
				{|m|
					chord[i][0] = m.value;
					this.updatemain;
			}, 3, false, 10);

			//order.add((i+1).asString++"_note");
			controls[(i+1).asString++"_note"] = EZPopUpMenu(w, 42@20,
				nil,  ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"],
				//nil, [0,1,2,3,4,5,6,7,8,9,10,11],
				{|m|
					chord[i][1] = m.value;
					this.updatemain;
			}, 0, false, 20);

			order.add((i+1).asString++"_bend");
			controls[(i+1).asString++"_bend"] = EZSlider( w,         // parent
				155@20,    // bounds
				nil,  // label
				ControlSpec(-0.9, 0.9, \lin, 0.01, 0),     // controlSpec
				{ |m|
					chord[i][2] = m.value;
					this.updatemain;
				} // action
			);

			w.view.decorator.nextLine;
		};

		StaticText(w, Rect(0,0, 10, 4)).string="";
		w.view.decorator.nextLine;

		//order.add("all_oct");
		controls["all_oct"] = EZPopUpMenu(w, 50@20,
			":A",  [0,1,2,3,4,5,6,7,8,9],
			{|ez|
				6.do{|i|
					chord[i][0] = ez.value;
					controls[(i+1).asString++"_oct"].value = ez.value
				};
				this.updatemain;
		}, 3, false, 10);

		//order.add("all_note");
		controls["all_note"] = EZPopUpMenu(w, 42@20,
			nil,  ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"],
			//nil, [0,1,2,3,4,5,6,7,8,9,10,11],
			{|ez|
				6.do{|i|
					chord[i][1] = ez.value;
					controls[(i+1).asString++"_note"].value = ez.value
				};
				this.updatemain;
		}, 0, false, 20);

		order.add("all_bend");
		controls["all_bend"] = EZSlider( w,         // parent
			155@20,    // bounds
			nil,  // label
			ControlSpec(-0.9, 0.9, \lin, 0.01, 0),     // controlSpec
			{ |ez|
				6.do{|i|
					chord[i][2] = ez.value;
					controls[(i+1).asString++"_bend"].value = ez.value
				};
				this.updatemain;
			} // action
		);

		if (preset.isNil, { // not loading a config file by default
			// if no default it should get the chord and base from main and display it in the widgets
			if (achord.isNil.not, { this.setnotes(achord) });
		}, {
			super.preset( w.name, preset ); // try to read and apply the default preset
		});


		w.front
	}

	notes {
		var notes = [0,0,0,0,0,0];
		notes.size.do{|i|
			notes[i] = (chord[i][0]*12) + chord[i][1] + chord[i][2] // base, note and bend
		};
		^notes;
	}

	setnotes {|achord| // six abs MIDI notes array. just display it
		chord = List.new(6);

		achord.do{|note| chord.add( this.decompose(note)) }; // octave, note, bend

		chord.do{|values, i|
			var sl = controls[(i+1).asString++"_oct"];
			sl.value = values[0];
			sl = controls[(i+1).asString++"_note"];
			sl.value = values[1];
			sl = controls[(i+1).asString++"_bend"];
			sl.value = values[2];
		};
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
		if (main.isNil.not, {
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
}
