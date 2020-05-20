/*

NeckGUI.new(master)

master is an optional instance of an object that contains a <chord variable. it will be updated each time chord changes in the ChordGUI

to do: explore tunnings
*/



GNeckGUI : BaseGUI {
	var main; // this is an object that has a <chord variable
	var buttons;
	var notes;
	var tune;
	var midi;
	var <chord;

	*new {|amain, path, preset|
		^super.new.init(amain, path, preset);
	}

	init {|amain, path, preset| //////////////////////
		super.initBaseGUI(path);

		main = amain;

		buttons = List.new;
		6.do{ buttons.add(List()) }; // 6 strings
		notes = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
		tune = ['E', 'A', 'D', 'G', 'C', 'E']; // C E G C E G // G B D G B D
		midi = [40, 45, 50, 55, 59, 64]; //  E2–A2–D3–G3–B3–E4.
		chord = [40, 45, 50, 55, 59, 64]; // init to open strings chord

		super.gui("GNeck", 138@495);

		w.view.decorator.nextLine;

		controls[\chord] = EZPopUpMenu.new(w, 52@20, nil,
			GuitarChords.chords.asSortedArgsArray.reject({|i| i; i.isArray}),
			{|ez|
				var ch = GuitarChords.chords[ez.item.asSymbol]; // -1 because first row is not there
				//buttons.flat.collect(_.valueAction = 0);
				buttons.flat.collect(_.value = 0);
				chord = midi.copy; // reset to open chord
				6.do{|i|
					var index = midi[0] + ch[i] - midi[i] - 1;
					if (index>=0, {
						buttons[i][index].valueAction = 1
					})
				};
				if (main.isNil.not, { main.chord(chord) }); // update chord in main object
		}, 0, false, 32);

		ActionButton(w,"choose",{
			this.choose
		});

		w.view.decorator.nextLine;

		ActionButton(w,"clear",{
			this.clear
		});
		ActionButton(w,"rand",{
			this.rand
		});

		w.view.decorator.nextLine;

		this.doButtons;

		if (preset.isNil.not, { // not loading a preset file by default
			super.preset( w.name, preset ); // try to read and apply the default preset
		});

		w.front;
	}

	doButtons {
		tune.size.do{|i| // top line of open notes
			StaticText(w, 20@20).align_(\center).string = tune[i];
		};
		w.view.decorator.nextLine;

		(1..15).do{|freth| //0 is top
			6.do{|string|
				var base = notes.indexOf(tune[string]);
				var button = Button.new(w, Rect(0,0,20,25))
				.states_([
					[notes.wrapAt(base+freth), Color.black, Color.grey],
					[notes.wrapAt(base+freth), Color.white, Color.black],
				])
				.action_({ arg but;
					if (but.value==1, {
						var pos = buttons.flat.indexOf(but);
						var myst = (pos/15).asInteger;
						var relpos = buttons[myst].indexOf(but);
						chord[string] = midi[myst] + relpos + 1; //MIDI number for this note
					}, { // open string when clicked off
						chord[string] = midi[string];
					});

					buttons[string].do{|abut|
						if(abut != but, { abut.value = 0 }); //others in same string switch off
					};

					if (main.isNil.not, { main.chord(chord) }); // update chord in main object
				});
				buttons[string].add(button);
				//controls[ (freth.asString++"_"++string.asString).asSymbol ] = button; // every single one
			};
			w.view.decorator.nextLine;
		};
	}

	clear {
		buttons.flat.collect(_.value = 0);
		chord = midi.copy;
		if (main.isNil.not, { main.chord(chord) }); // update chord in main object
	}

	rand {
		6.do{|string|
			buttons[string].choose.valueAction = 1;
		}
	}

	choose {
		controls[\chord].valueAction = controls[\chord].items.size.rand
	}
}
