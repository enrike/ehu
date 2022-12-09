/* FEEDBACK
*/
f = Feedback1.new;


/* EFFECTS
// effects I use in combination with the feedback unit. some are just quick tests
*/
AutoNotchGUI.new;
TremoloGUI.new;
NormalizerGUI.new;
CompanderGUI.new;
CompressorGUI.new;
FreqShiftGUI.new;
LimiterGUI.new;
ChaosPitchShiftGUI.new;
PitchShiftGUI.new;
GainLimiterGUI.new;
DelayGUI.new;
FreeverbGUI.new;
GVerbGUI.new;
M2stGUI.new; // takes a mono channel and duplicates it in two channels
Shooter.new(filepath:"/path/to/folder/with/files") // shots a sample when onset is detected. (max 5 layers at the same time)
Mirror.new; // reverse play then incoming sound when onset is detected
PatcherGUI.new // patches the audio from a bus into another bus
//SoundGateGUI.new // reverse noise gate (only low noise passes)
Chrono.new // just a GUI chronometer that counts to 59:59

Launcher.new; // simple GUI to launch all effects above

ChannelEQ.new; // download and install from https://github.com/enrike/supercollider-channeleq requieres wslib and tabbed views quarks
ChannelEQ.new(10); // you can use the loop bus value to control the internal feedback EQ

BufferPlayerGUI.new // play a sound file




/* PADS
GUI to control the samples located in a folder.
It can be used together with NanoKontrol2. Each sample (up to 8) will be controlled by one control area in the NK2.
R1 > play/stop, M1 > loop on/off, S1 > switch direction, Vol1 > amplitude, Knob1 > pitch, etc...
*/
Pads.new("/path/to/files/*.flac", // Pads arguments
	out:0, // buses to output the audio [0,1]
	col:5, // how many columns the window should contain
	size:120, // horizonal size of the button, vertical will be 0.75. pass 100@50 if you want to specify both
	amp:1, // inital amp
	midif:1, // connect to nanokontrol2 MIDI controller?
	loop:0,  // init loop state
	tail:0, // addToTail the synth or not
	nk2:[0,3,5], // nanokontrol2 asign which control will be linked to which sample. eg [0,3,5], first sound > control number 0, second sound control number 3
	randmode:0, // 1 will create a single button that will trigger random samples from the path
	mode:0 // 0 button click will on/off, 1 button click will on/on
)

p=Pads.new("/path/to/files/*.flac")
p.play(0,2) // will play pad 0 and 2. you must wait until the Pad has been propely initiated to run this
p.stop(3,4)




// PRESET SYSTEM.
/* the save/open buttons allow to save and load the current state of the window and GUI inside the "presets" directory.
Save will name the preset like EFFECTNAME_yearmothday_hourminsecs.preset
When the window opens it tries to load a prest named "presets/EFFECTNAME_default.preset"
they can be loaded like this too:
*/

CompanderGUI.new(preset:"201024_182949"); // will load "presets/compander_201024_182949.preset"
GainLimiterGUI.new(preset:"201024_160150"); // will load "presets/gainlimiter_201024_160150.preset". Beware the missing capitals

// presets + defer
( // if you use defer you must pass the ref to nowExecutingPath to find the presets folder
x = thisProcess.nowExecutingPath.dirname;
{
	CompanderGUI.new(exepath:x, preset:"201024_182949"); //make sure the presets are there
	AutoNotchGUI.new(exepath:x, preset:"201026_122236");
	ChannelEQ.new;
	GainLimiterGUI.new(exepath:x, preset:"201026_122001");
}.defer(3); // for instance
)



// AUTO system and auto presets
/* auto button just opens a window that allows to set each slider to a rand variable every N secs. Presets can be saved and also set on initialization. This below should open Feedback and its auto window with the default auto preset
*/
Feedback1.new(autopreset:"default");


// save ALL open window's current state at the same time
~ehuws.collect(_.save)
// because each wind add itsleft to ~ehuws list
~ehuws.collect(_.save("bigmuff")) // will save the presets under "bigmuff" tag instead of inserting the date-time stamp


