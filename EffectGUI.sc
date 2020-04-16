
EffectGUI {

	classvar effectsGroup;

	var <controls, path, <w, <order;

	/*
	a base class for a GUI window with widgets that can save/restore the configuration of the widgets.
	To be extended adding widgets after calling this.gui and finally calling w.front

	widgets must be added to controls dictionary like this

	controls[\gainin] = EZSlider( w,         // parent
	420@20,    // bounds
	"in gain",  // label
	ControlSpec(0, 2, \lin, 0.001, 0, \amp),     // controlSpec
	{ |ez| synth.set(\gain, ez.value) } // action
	);
	*/

	*new {|exepath|
		^super.new.initEffectGUI(exepath);
	}

	initEffectGUI {|exepath|
		controls = Dictionary.new;
		order = List.new;

		if (exepath.isNil, {
			try { path = thisProcess.nowExecutingPath.dirname} { path=Platform.userHomeDir}
		},{
			path = exepath;
		});

		effectsGroup = Group.new(Server.default, \addToTail); // was addAfter
	}

	gui { |name="", bounds=#[0,0, 310, 120]| // run this if you want to have open and save buttons in the window
		w = Window.new(name, bounds).alwaysOnTop=true;
		w.view.decorator=FlowLayout(w.view.bounds);
		w.view.decorator.gap=2@2;

		ActionButton(w,"S",{
			this.save;
		});
		ActionButton(w,"O",{
			this.open;
		});
	}

	update {|name, value| // control widgets remotely
		{controls[name].valueAction = value}.defer
	}

	save {
		var data = Dictionary.new, name="", filename;
		if (w.isNil.not, {name=w.name.replace(" ", "_")}); //prepend the windows name
		filename = name++"_"++Date.getDate.stamp++".preset";

		controls.keysValuesDo { |key, widget|
			data.put(key, widget.value)
		};

		("saving preset into" + path ++ Platform.pathSeparator ++ filename).postln;

		data.writeArchive(path ++ Platform.pathSeparator ++ filename);
	}

	open {
		var data;
		FileDialog({ |path|
			data = Object.readArchive(path);
			data.keysValuesDo{ |key, value|
				try {
					{controls[key].valueAction = value}.defer // wait for QT
				}{|er| er.postln}
			};
		},
		fileMode: 0,
		stripResult: true,
		path: path);
	}
}