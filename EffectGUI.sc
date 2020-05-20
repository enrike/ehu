
BaseGUI {

	//classvar effectsGroup;

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

	/*	*new {|exepath|
	^super.new.initBaseGUI(exepath);
	}*/

	init {|exepath|
		controls = Dictionary.new;
		order = List.new;

		if (exepath.isNil, {
			try { path = thisProcess.nowExecutingPath.dirname} { path=Platform.userHomeDir}
		},{
			path = exepath;
		});
	}

	gui { |name="", bounds=#[0,0, 310, 120]| // run this if you want to have open and save buttons in the window

		name = name.replace(" ", "_").toLower;

		w = Window.new(name, bounds).alwaysOnTop=true;
		w.view.decorator = FlowLayout(w.view.bounds);
		w.view.decorator.gap=2@2;

		w.onClose = {
			this.close;
		};

		ActionButton(w,"S",{
			this.save;
		});
		ActionButton(w,"O",{
			this.open;
		});
		w.front;
	}

	preset {|name, default=\default|
		var thepath = "";
		name = name.replace(" ", "_").toLower;
		thepath = PathName.new(path ++ Platform.pathSeparator ++ "presets"
			++ Platform.pathSeparator ++ name ++ "_" ++ default.asString ++ ".preset");
		if (thepath.isFile==true, {
			this.read(thepath.fullPath);
		}, {("no preset for"+name).postln});
	}

	update {|name, value| // control widgets remotely
		{controls[name].valueAction = value}.defer
	}

	save {
		var data = Dictionary.new, name="", filename;
		if (w.isNil.not, {name=w.name.replace(" ", "_").toLower}); //prepend the windows name
		filename = name++"_"++Date.getDate.stamp++".preset";

		//data.put(\loc, w.bounds);

		controls.keysValuesDo { |key, widget|
			data.put(key, widget.value)
		};

		("saving preset into" + path ++ Platform.pathSeparator ++ "presets" ++ Platform.pathSeparator ++ filename).postln;

		data.writeArchive(path ++ Platform.pathSeparator ++ "presets" ++ Platform.pathSeparator ++ filename);
	}

	close {}

	open {
		FileDialog({ |apath|
			this.read(apath)
		},
		fileMode: 0,
		stripResult: true,
		path: path);
	}

	read {|apath|
		var	data = Object.readArchive(apath);
		("reading preset"+apath).postln;

		data.keysValuesDo{ |key, value|
			[key, value].postln;
			try {
				{controls[key].valueAction = value}.defer // wait for QT
			}{|er| er.postln}
		};
	}
}



EffectGUI : BaseGUI {
	var <synth;

	/*	*new {|exepath=""|
	^super.new.initEffectGUI(exepath);
	}*/

	init {|exepath|
		super.init(exepath);
	}

	close {
		("freeing"+synth).postln;
		synth.free;
		super.close;
	}

	gui {|name, bounds|
		super.gui(name, bounds);

		StaticText(w, 20@18).align_(\right).string_("In").resize_(7);
		controls[\in] = PopUpMenu(w, Rect(10, 10, 45, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\in, m.value);
		}
		.value_(0); // default to sound in

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
			if (synth.isNil.not, {
				var sname = synth.defName;
				synth.free;
				if (butt.value==1, {
					Server.default.waitForBoot{
						synth = Synth.tail(Server.default, sname);
						Server.default.sync;
						("run"+sname+"synth").postln;
					};
				}, {
					("kill"+sname+"synth").postln;
				})
			});
		}).value=1;
	}
}