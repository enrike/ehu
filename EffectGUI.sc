BaseGUI {

	//classvar effectsGroup;

	var <controls, path, <w, <order, slbounds;

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

		slbounds = (w.bounds.width-30)@20;

		w.onClose = {
			this.close;
		};

		SimpleButton(w,"S",{
			this.save;
		});
		SimpleButton(w,"O",{
			this.open;
		});
		w.front;

		if (~ehuws.isNil, {~ehuws=List.new});
		~ehuws.add(this)
	}

	preset {|name, default=\default|
		var thepath = "";
		name = name.replace(" ", "_").toLower;
		thepath = PathName.new(path ++ Platform.pathSeparator ++ "presets"
			++ Platform.pathSeparator ++ name ++ "_" ++ default.asString ++ ".preset");

		thepath.postln;

		if (thepath.isFile==true, {
			this.read(thepath.fullPath);
		}, {
			("no preset for"+name).postln;
			thepath.postln;
		});
	}

	update {|name, value| // control widgets remotely
		{controls[name].valueAction = value}.defer
	}

	save {
		var data = Dictionary.new, name="", filename;
		if (w.isNil.not, {name=w.name.replace(" ", "_").toLower}); //prepend the windows name
		filename = name++"_"++Date.getDate.stamp++".preset";

		data.put(\bounds, w.bounds);

		controls.keysValuesDo { |key, widget|
			data.put(key, widget.value)
		};

		("saving preset into" + path ++ Platform.pathSeparator ++ "presets" ++ Platform.pathSeparator ++ filename).postln;

		data.writeArchive(path ++ Platform.pathSeparator ++ "presets" ++ Platform.pathSeparator ++ filename);
	}

	close {
		~ehuws.remove(this);
		w.close;
	}

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

		[\bounds, data[\bounds]].postln; //make sure it first deals with ON
		{ w.bounds = data[\bounds] }.defer; // wait for QT
		data.removeAt(\bounds); // we are done with this

		data.keysValuesDo{ |key, value|
			//[key, value].postln; // we must first run ON button to trigger the synth. then do the rest.
			try {
				if(key==\drywet, { // this is for backwards compatibility
					{controls[\xfade].valueAction = value}.defer // wait for QT
				}, {
					{controls[key].valueAction = value}.defer // wait for QT
				});

			}{|er| er.postln; "XXXXX".postln}
		};
	}
}


SimpleButton {
	*new {|parent, label, action|
		^super.new.init(parent, label, action);
	}

	init {|parent, label, action|
		var skin=GUI.skin;
		var font = GUI.font.new(*skin.fontSpecs);
		var w = (label.bounds(font).width + 10).max(20); // (optimalWidth + 10).max(minWidth?20)
		Button.new(parent, w@GUI.skin.buttonHeight)
		.states_([
			[label, Color.black, Color.grey(0.5, 0.2)]
		])
		.action_(action);
	}
}


EffectGUI : BaseGUI {
	var <synth, midisetup, synthdef, utils;

	/*	*new {|exepath=""|
	^super.new.initEffectGUI(exepath);
	}*/

	init {|exepath|
		midisetup = List.new;
		synthdef = SynthDef(\default, {});
		super.init(exepath);
		utils = List.new;
		if (~ehu_effects.isNil, {~ehu_effects=List.new})
	}

	close {
		("freeing"+synth).postln;
		synth.free;
		super.close;
		utils.do{|ut|
			ut.close
		};
		~ehu_effects.remove(this)
	}

	pbut {|controlname|
		SimpleButton(w,"p",{
			ParamWinGUI.new(path:path, name:controls[controlname].label, func:{|data|
				controls[controlname].valueAction = controls[controlname].controlSpec.map(data.asFloat);
			} );
		});
	}

	gui {|name, bounds|
		super.gui(name, bounds);

		StaticText(w, 14@18).align_(\right).string_("In").resize_(7);
		controls[\in] = PopUpMenu(w, Rect(10, 10, 45, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\in, m.value);
		}.value_(0); // default to sound in

		StaticText(w, 20@18).align_(\right).string_("Out").resize_(7);
		controls[\out] = PopUpMenu(w, Rect(10, 10, 45, 17))
		.items_( Array.fill(16, { arg i; i }) )
		.action_{|m|
			synth.set(\out, m.value);
		}.value_(0); // default to sound in

		SimpleButton(w,"midi",{
			this.midi(midisetup);
		});

		SimpleButton(w,"auto",{
			AutoGUI.new(this, path);
		});

		controls[\on] = Button(w, 22@18)
		.states_([
			["on", Color.white, Color.black],
			["off", Color.black, Color.red]
		])
		.action_({ arg butt;
			if (butt.value==1, {
				("running audio:"+name).postln;
				this.audio;
				~ehu_effects.add(this);
			}, {
				synth.free;
				synth = nil;
				~ehu_effects.remove(this);
				("kill"+synthdef.name+"synth").postln;
			})
		}).value=0;

		controls[\up] = Button(w, 11@18)
		.states_([ ["<", Color.white, Color.black]])
		.action_({ arg butt;
			~ehu_effects.do{|ef, i|
				if( (ef==this), {
					if (i>0, {
						synth.moveBefore(~ehu_effects[i-1].synth);
						~ehu_effects.removeAt(i);
						~ehu_effects.insert(i-1, ef);
						Server.local.queryAllNodes;
					})
			})};

		});
		controls[\down] = Button(w, 11@18)
		.states_([ [">", Color.white, Color.black]])
		.action_({ arg butt;
			~ehu_effects.do{|ef, i|
				if( (ef==this), {
					if (i<(~ehu_effects.size-1), {
						synth.moveAfter(~ehu_effects[i+1].synth);
						~ehu_effects.removeAt(i);
						~ehu_effects.insert(i+1, ef);
						Server.local.queryAllNodes;
					})
			})}
		});

		controls[\tree] = Button(w, 8@18)
		.states_([ ["t", Color.white, Color.black]])
		.action_({ arg butt;
			Server.local.queryAllNodes;
		});
	}

	audio {|argarr=#[]|
		Server.default.waitForBoot{
			synth.free;
			synthdef.load;
			Server.default.sync;
			synth = Synth.tail(Server.default, synthdef.name, argarr);
			Server.default.sync;
			("run"+synth.defName+"synth").postln;
		}
	}

	auto {|config=\default|
		utils.add( AutoGUI.new(this, path, config) )
	}

	midi {|setup|
		"Connecting MIDI...".postln;

		if (MIDIClient.initialized==false, {
			MIDIClient.init;
			MIDIIn.connectAll;
			MIDIdef.freeAll;
		});
		setup.do{|pair, i|
			("MIDI"+pair[1].asString+">"+pair[0].asString).postln;
			this.setupControl(pair[0], pair[1]);
		}
	}

	setupControl {|control, channel|
		// sliders and knobs
		MIDIdef.cc(control, {arg ...args;
			{
				var min = controls[control].controlSpec.minval;
				var max = controls[control].controlSpec.maxval;
				controls[control].valueAction_(args[0].linlin(0,127, min, max))
			}.defer;
		}, channel); // match cc

		// R buttons random for sliders
		MIDIdef.cc(control++"_r", {arg ...args;
			{
				if (args[0]==127, {
					var min = controls[control].controlSpec.minval;
					var max = controls[control].controlSpec.maxval;
					controls[control].valueAction_( rrand(min.asFloat, max.asFloat) )
				});
			}.defer;
		}, channel+64); // R buttons in nanokontrol

		// S buttons random short jump for sliders
		MIDIdef.cc(control++"_r", {arg ...args;
			{
				if (args[0]==127, {
					var current = controls[control].value;
					var min = controls[control].controlSpec.minval;
					var max = controls[control].controlSpec.maxval;
					controls[control].valueAction_( rrand(min.asFloat, max.asFloat) );
					{controls[control].valueAction_( current )}.defer(0.2) // back after 0.2
				});
			}.defer;
		}, channel+32); // R buttons in nanokontrol
	}

	read {|apath| //overwrite super.read
		var	data = Object.readArchive(apath);
		("reading preset"+apath).postln;
		synth.postln;

		[\on, data[\on]].postln; //make sure it first deals with ON
		try {
			{ controls[\on].valueAction = data[\on] }.defer; // wait for QT
		}{|er| er.postln};
		data.removeAt(\on); // we are done with this

		//[\bounds, data[\bounds]].postln; //bounds

		if (data[\bounds].isNil.not, {
			{ w.bounds = data[\bounds] }.defer; // wait for QT
		});
		data.removeAt(\bounds); // we are done with this
		{
			data.keysValuesDo{ |key, value|
				[key, value].postln;
				try {
					if(key==\drywet, { // backwards compatibility. remove when all presets are updated
						{controls[\xfade].valueAction = value}.defer // wait for QT
					}, {
						{controls[key].valueAction = value}.defer // wait for QT
					});
				}{|er| er.postln;}
			};
		}.defer(2)
	}
}
